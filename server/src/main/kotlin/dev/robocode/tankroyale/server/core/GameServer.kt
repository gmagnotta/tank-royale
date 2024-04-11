package dev.robocode.tankroyale.server.core

import com.google.gson.Gson
import dev.robocode.tankroyale.schema.*
import dev.robocode.tankroyale.schema.BotIntent
import dev.robocode.tankroyale.schema.GameSetup
import dev.robocode.tankroyale.server.Server
import dev.robocode.tankroyale.server.dev.robocode.tankroyale.server.connection.ConnectionHandler
import dev.robocode.tankroyale.server.dev.robocode.tankroyale.server.connection.GameServerConnectionListener
import dev.robocode.tankroyale.server.dev.robocode.tankroyale.server.model.InitialPosition
import dev.robocode.tankroyale.server.dev.robocode.tankroyale.server.model.ParticipantId
import dev.robocode.tankroyale.server.dev.robocode.tankroyale.server.score.ResultsView
import dev.robocode.tankroyale.server.mapper.*
import dev.robocode.tankroyale.server.model.*
import org.java_websocket.WebSocket
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt


/** Game server. */
class GameServer(
    /** Supported game types */
    private val gameTypes: Set<String>,
    /** Optional controller secrets */
    controllerSecrets: Set<String>,
    /** Optional bot secrets */
    botSecrets: Set<String>
) {
    /** Connection handler for observers and bots */
    /** Initializes connection handler */
    private val connectionHandler: ConnectionHandler =
        ConnectionHandler(ServerSetup(gameTypes), GameServerConnectionListener(this), controllerSecrets, botSecrets)

    /** Current server state */
    private var serverState = ServerState.WAIT_FOR_PARTICIPANTS_TO_JOIN

    /** Current game setup */
    private lateinit var gameSetup: dev.robocode.tankroyale.server.model.GameSetup

    /** Game participants (bots connections) */
    private val participants = ConcurrentHashMap.newKeySet<WebSocket>()

    /** Game participants that signalled 'ready' for battle */
    private val readyParticipants = ConcurrentHashMap.newKeySet<WebSocket>()

    /** Map over participant ids: bot connection -> bot id */
    private val participantIds = ConcurrentHashMap<WebSocket, BotId>()

    /** Map over bot intents: bot connection -> bot intent */
    private val botIntents = ConcurrentHashMap<WebSocket, dev.robocode.tankroyale.server.model.BotIntent>()

    /** Map over participants sent to clients */
    private val participantMap = mutableMapOf<BotId, Participant>()

    /** Model updater that keeps track of the game state/model */
    private var modelUpdater: ModelUpdater? = null

    /** Timer for 'ready' timeout */
    private var readyTimeoutTimer: NanoTimer? = null

    /** Timer for 'turn' timeout */
    private var turnTimeoutTimer: NanoTimer? = null

    /** Current TPS setting (Turns Per Second) */
    private var tps = Server.tps

    /** Logger */
    private val log = LoggerFactory.getLogger(GameServer::class.java)

    /** JSON handler */
    private val gson = Gson()

    /** Tick lock for onNextTurn() */
    private val tickLock = Any()

    private var botListUpdateMessage = BotListUpdate().apply {
        this.type = Message.Type.BOT_LIST_UPDATE
        this.bots = listOf<BotInfo>()
    }

    /** Starts this server */
    fun start() {
        log.info("Starting server on port ${Server.port} with supporting game type(s): ${gameTypes.joinToString()}")
        connectionHandler.start()
    }

    /** Stops this server */
    fun stop() {
        log.info("Stopping server")
        connectionHandler.stop()
    }

    /** Prepares the game and wait for participants to become 'ready' */
    private fun prepareGame() {
        log.debug("Preparing game")

        serverState = ServerState.WAIT_FOR_READY_PARTICIPANTS
        participantIds.clear()
        readyParticipants.clear()

        prepareParticipantIds()
        prepareModelUpdater()
        sendGameStartedToParticipants()
        startReadyTimer()
    }

    private val startGameLock = Any()

    /** Starts the game if all participants are ready */
    private fun startGameIfParticipantsReady() {
        synchronized(startGameLock) {
            if (readyParticipants.size == participants.size) {
                participantMap.apply {
                    clear()
                    putAll(createParticipantMap())
                }

                readyTimeoutTimer?.stop()

                readyParticipants.clear()
                botIntents.clear()

                startGame()
            }
        }
    }

    private fun prepareParticipantIds() {
        participants.forEachIndexed { index, conn ->
            participantIds[conn] = BotId(index + 1)
        }
    }

    /** Send game-started event to all participant bots to get them started */
    private fun sendGameStartedToParticipants() {
        val gameSetup = GameSetupMapper.map(gameSetup)
        val participantsAndTeamIds = getParticipantsAndTeamIds()
        val botHandshakes = connectionHandler.getBotHandshakes()

        participantIds.forEach { (conn, botId) ->
            val teamId = botHandshakes[conn]?.teamId

            val teammateIds =
                teamId?.let { participantsAndTeamIds.filterValues { it == teamId }.keys.toSet().minus(botId) }
                    ?: emptySet()

            val gameStartedForBot = createGameStartedEventForBot(botId, teammateIds, gameSetup)
            send(conn, gameStartedForBot)
        }
    }

    private fun getParticipantsAndTeamIds(): Map<BotId, Int?> = participantIds
        .mapNotNull { (conn, botId) -> connectionHandler.getBotHandshakes()[conn]?.teamId?.let { botId to it } }
        .associateBy({ it.first }, { it.second })

    /** Creates a GameStartedEventForBot with current game setup */
    private fun createGameStartedEventForBot(
        botId: BotId,
        teammateIds: Set<BotId>,
        gameSetup: GameSetup
    ): GameStartedEventForBot {
        return GameStartedEventForBot().apply {
            type = Message.Type.GAME_STARTED_EVENT_FOR_BOT
            myId = botId.value
            this.teammateIds = teammateIds.map { it.value }
            this.gameSetup = gameSetup

            val botsMap: MutableMap<BotId, MutableBot> = modelUpdater?.botsMap!!
            botsMap[botId]?.let {
                startX = it.x
                startY = it.y
                startDirection = it.direction
            }
        }
    }

    /** Starts the 'ready' timer */
    private fun startReadyTimer() {
        readyTimeoutTimer = NanoTimer(gameSetup.readyTimeout * 1_000_000L) { onReadyTimeout() }.apply { start() }
    }

    /** Starts a new game */
    private fun startGame() {
        log.info("Starting game")

        serverState = ServerState.GAME_RUNNING

        sendGameStartedToObservers()
        prepareModelUpdater()
        resetTurnTimeout()
    }

    /** Send GameStarted to all participant observers to get them started */
    private fun sendGameStartedToObservers() {
        if (connectionHandler.observerAndControllerConnections.isNotEmpty()) {
            broadcastToObserverAndControllers(GameStartedEventForObserver().apply {
                type = Message.Type.GAME_STARTED_EVENT_FOR_OBSERVER
                gameSetup = GameSetupMapper.map(this@GameServer.gameSetup)
                participants = participantMap.values.toList()
            })
        }
    }

    /** Creates a map over participants from the bot connection handshakes */
    private fun createParticipantMap(): Map<BotId, Participant> {
        val participantMap = mutableMapOf<BotId, Participant>()
        for (conn in participants) {
            val handshake = connectionHandler.getBotHandshakes()[conn]
            val botId = participantIds[conn] ?: continue
            val participant = Participant().apply {
                id = botId.value
                sessionId = handshake!!.sessionId
                name = handshake.name
                version = handshake.version
                description = handshake.description
                authors = handshake.authors
                homepage = handshake.homepage
                countryCodes = handshake.countryCodes
                gameTypes = handshake.gameTypes
                platform = handshake.platform
                programmingLang = handshake.programmingLang
                initialPosition = handshake.initialPosition
                teamId = handshake.teamId
                teamName = handshake.teamName
                teamVersion = handshake.teamVersion
                isDroid = handshake.isDroid
            }
            participantMap[botId] = participant
        }
        return participantMap
    }

    /** Prepares model-updater */
    private fun prepareModelUpdater() {
        val participantIds = createParticipantIds()

        val initialPositions = participantMap.filter { it.value.initialPosition != null }.mapValues {
            val p = it.value.initialPosition
            InitialPosition(p.x, p.y, p.direction)
        }
        val droidFlags = participantMap.mapValues { it.value.isDroid == true }

        modelUpdater = ModelUpdater(gameSetup, participantIds, initialPositions, droidFlags)
    }

    private fun createParticipantIds(): Set<ParticipantId> {

        val participantIds = mutableSetOf<ParticipantId>()

        connectionHandler.getBotHandshakes().forEach { (conn, botHandshake) ->
            this.participantIds[conn]?.let { botId ->
                val teamId = botHandshake.teamId?.let { TeamId(it) }
                participantIds += ParticipantId(botId, teamId)
            }
        }
        return participantIds
    }

    /** Last reset turn timeout period */
    @Volatile
    var lastResetTurnTimeoutPeriod: Long = 0

    /** Resets turn timeout timer */
    private fun resetTurnTimeout() {
        val period = calculateTurnTimeoutPeriod()

        // New timer is only set, if the period is different from the last reset time
        if (period != lastResetTurnTimeoutPeriod) {
            lastResetTurnTimeoutPeriod = period

            // Start new turn timeout timer to invoke onNextTurn()
            turnTimeoutTimer?.stop()
            turnTimeoutTimer = NanoTimer(period) { onNextTurn() }.apply { start() }
        }
    }

    /** Calculates and returns a timeout turn period measured in nanoseconds based on current TPS */
    private fun calculateTurnTimeoutPeriod(): Long {
        val period = if (tps <= 0) 0 else 1_000_000_000L / tps
        val turnTimeout = gameSetup.turnTimeout * 1000L
        return if (turnTimeout > period) turnTimeout else period
    }

    /** Broadcast game-aborted event to all observers and controllers */
    private fun broadcastGameAborted() {
        broadcastToAll(GameAbortedEvent().apply {
            type = Message.Type.GAME_ABORTED_EVENT
        })
    }

    /** Returns a list of bot results (for bots) ordered on the score ranks */
    private fun getResultsForBot(botId: BotId): ResultsForBot {
        val results = modelUpdater!!.getResults()

        val index = results.indexOfFirst { it.participantId.botId == botId }
        check(index >= 0) { "botId was not found in results: $botId" }

        val score = results[index]
        return ResultsForBot().apply {
            this.rank = index + 1
            survival = score.survivalScore.roundToInt()
            lastSurvivorBonus = score.lastSurvivorBonus.roundToInt()
            bulletDamage = score.bulletDamageScore.roundToInt()
            bulletKillBonus = score.bulletKillBonus.toInt()
            ramDamage = score.ramDamageScore.roundToInt()
            ramKillBonus = score.ramKillBonus.roundToInt()
            totalScore = score.totalScore.roundToInt()
            firstPlaces = score.firstPlaces
            secondPlaces = score.secondPlaces
            thirdPlaces = score.thirdPlaces
        }
    }

    /** Returns a list of bot results (for observers and controllers) ordered on the score ranks */
    private fun getResultsForObservers(): List<ResultsForObserver> {

        val results = mutableListOf<ResultsForObserver>()

        val scores = ResultsView.getResults(modelUpdater!!.getResults(), participantMap.values)
        scores.forEach { score ->
            run {
                participantMap[score.participantId.botId]?.let { participant ->

                    val (id, name, version) =
                        if (participant.teamId == null)
                            Triple(participant.id, participant.name, participant.version)
                        else
                            Triple(participant.teamId, participant.teamName, participant.teamVersion)

                    ResultsForObserver().apply {
                        this.id = id
                        this.name = name
                        this.version = version
                        this.rank = score.rank
                        survival = score.survivalScore.roundToInt()
                        lastSurvivorBonus = score.lastSurvivorBonus.roundToInt()
                        bulletDamage = score.bulletDamageScore.roundToInt()
                        bulletKillBonus = score.bulletKillBonus.toInt()
                        ramDamage = score.ramDamageScore.roundToInt()
                        ramKillBonus = score.ramKillBonus.roundToInt()
                        totalScore = score.totalScore.roundToInt()
                        firstPlaces = score.firstPlaces
                        secondPlaces = score.secondPlaces
                        thirdPlaces = score.thirdPlaces

                        results += this
                    }
                }
            }
        }
        return results
    }


    /** Broadcast pause event to all observers */
    private fun broadcastGamedPausedToObservers() {
        broadcastToObserverAndControllers(GamePausedEventForObserver().apply {
            type = Message.Type.GAME_PAUSED_EVENT_FOR_OBSERVER
        })
    }

    /** Broadcast resume event to all observers */
    private fun broadcastGameResumedToObservers() {
        broadcastToObserverAndControllers(GameResumedEventForObserver().apply {
            type = Message.Type.GAME_RESUMED_EVENT_FOR_OBSERVER
        })
    }

    /** Broadcast TPS-changed event to all observers */
    private fun broadcastTpsChangedToObservers(tps: Int) {
        broadcastToObserverAndControllers(TpsChangedEvent().apply {
            type = Message.Type.TPS_CHANGED_EVENT
            this.tps = tps
        })
    }

    private fun updateGameState(): GameState {
        val mappedBotIntents = mutableMapOf<BotId, dev.robocode.tankroyale.server.model.BotIntent>()
        botIntents.forEach { (key, value) -> participantIds[key]?.let { botId -> mappedBotIntents[botId] = value } }
        return modelUpdater!!.update(mappedBotIntents.toMap())
    }

    private fun onReadyTimeout() {
        log.debug("Ready timeout")
        if (readyParticipants.size >= gameSetup.minNumberOfParticipants) {
            // Start the game with the participants that are ready
            participants.apply {
                clear()
                addAll(readyParticipants)
            }
            startGame()
        } else {
            // Not enough participants -> prepare another game
            serverState = ServerState.WAIT_FOR_PARTICIPANTS_TO_JOIN
        }
    }

    private fun onNextTurn() {
        if (serverState !== ServerState.GAME_RUNNING) return

        // Required as this method can be called again while already running.
        // This would give a raise condition without the synchronized lock.
        synchronized(tickLock) {
            // Update game state
            updateGameState().apply {
                onNextTick(lastRound)

                if (isGameEnded) {
                    onGameEnded()
                }
            }
        }
    }

    private fun onGameEnded() {
        log.info("Game ended")

        broadcastGameEndedToParticipants()
        broadcastGameEndedToObservers()

        // Must be done after the broadcasting
        serverState = ServerState.GAME_STOPPED

        cleanupAfterGameStopped()
    }

    private fun onNextTick(lastRound: IRound?) {
        lastRound?.apply {
            lastTurn?.apply {
                if (turnNumber == 1) {
                    log.debug("Round started: $roundNumber")

                    // Clear in first turn (left over from other round?), but BEFORE broadcasting round started event
                    botIntents.clear()

                    broadcastRoundStartedToAll(roundNumber)
                } else { // not turn 1
                    // Send SkippedTurn, except in turn 1
                    checkForSkippedTurns(turnNumber)

                    // Clear bot intents after skipped turns have been handled, but BEFORE broadcasting tick event
                    botIntents.clear()
                }
                sendGameTickToParticipants(roundNumber, this)
                broadcastGameTickToObservers(roundNumber, this)

                // Send round ended _after_ tick has been sent
                if (roundEnded) {
                    log.debug("Round ended: $roundNumber")
                    broadcastRoundEndedToParticipants(roundNumber, turnNumber)
                    broadcastRoundEndedToObservers(roundNumber, turnNumber)
                }
            }
        }
    }

    private fun broadcastGameEndedToParticipants() {
        participants.forEach { conn ->
            participantIds[conn]?.let { botId ->
                GameEndedEventForBot().apply {
                    type = Message.Type.GAME_ENDED_EVENT_FOR_BOT
                    numberOfRounds = modelUpdater!!.numberOfRounds
                    results = getResultsForBot(botId)

                    send(conn, this)
                }
            }
        }
    }

    private fun broadcastGameEndedToObservers() {
        broadcastToObserverAndControllers(GameEndedEventForObserver().apply {
            type = Message.Type.GAME_ENDED_EVENT_FOR_OBSERVER
            numberOfRounds = modelUpdater!!.numberOfRounds
            results = getResultsForObservers() // Use the stored score!
        })
    }

    private fun broadcastRoundStartedToAll(roundNumber: Int) {
        broadcastToAll(RoundStartedEvent().also {
            it.type = Message.Type.ROUND_STARTED_EVENT
            it.roundNumber = roundNumber
        })
    }

    private fun broadcastRoundEndedToParticipants(roundNumber: Int, turnNumber: Int) {
        participants.forEach { conn ->
            participantIds[conn]?.let { botId ->
                RoundEndedEventForBot().apply {
                    type = Message.Type.ROUND_ENDED_EVENT_FOR_BOT
                    this.roundNumber = roundNumber
                    this.turnNumber = turnNumber
                    results = getResultsForBot(botId)

                    send(conn, this)
                }
            }
        }
    }

    private fun broadcastRoundEndedToObservers(roundNumber: Int, turnNumber: Int) {
        broadcastToObserverAndControllers(RoundEndedEventForObserver().also {
            it.type = Message.Type.ROUND_ENDED_EVENT_FOR_OBSERVER
            it.roundNumber = roundNumber
            it.turnNumber = turnNumber
            it.results = getResultsForObservers()
        })
    }

    private fun sendGameTickToParticipants(roundNumber: Int, turn: ITurn) {
        participants.forEach { conn ->
            participantIds[conn]?.apply {
                TurnToTickEventForBotMapper.map(roundNumber, turn, this)?.apply {
                    send(conn, this)
                }
            }
        }
    }

    private fun broadcastGameTickToObservers(roundNumber: Int, turn: ITurn) {
        broadcastToObserverAndControllers(TurnToTickEventForObserverMapper.map(roundNumber, turn, participantMap))
    }

    private fun checkForSkippedTurns(currentTurnNumber: Int) {
        val botsSkippingTurn = getParticipantsThatSkippedTurn()

        if (botsSkippingTurn.isNotEmpty()) {
            val skippedTurn = SkippedTurnEvent().apply {
                type = Message.Type.SKIPPED_TURN_EVENT
                turnNumber = currentTurnNumber - 1 // last turn number
            }
            val json = gson.toJson(skippedTurn)

            botsSkippingTurn.forEach { bot -> connectionHandler.send(bot, json) }
        }
    }

    private fun getParticipantsThatSkippedTurn(): Collection<WebSocket> =
        mutableListOf<WebSocket>().apply {
            participants.forEach { participant ->
                // Check if no intent was received from the participant during the turn
                if (botIntents[participant] == null) this += participant
            }
        }

    private val botsThatSentIntent = mutableSetOf<WebSocket>()

    private fun updateBotListUpdateMessage() {
        mutableListOf<BotInfo>().also { bots ->
            botListUpdateMessage.bots = bots

            connectionHandler.apply {
                getBotConnections().forEach { conn ->
                    getBotHandshakes()[conn]?.let { botHandshake ->
                        conn.remoteSocketAddress.apply {
                            bots += BotHandshakeToBotInfoMapper.map(botHandshake, hostString, port)
                        }
                    }
                }
            }
        }
    }

    private fun send(conn: WebSocket, msg: Message) {
        requireNotNull(msg.type) { "'type' is required on the message" }
        gson.toJson(msg).also {
            try {
                conn.send(it)
            } catch (ignore: WebsocketNotConnectedException) {
                // Bot cannot receive events and send new intents.
            }
        }
    }

    private fun broadcastToObserverAndControllers(msg: Message) {
        requireNotNull(msg.type) { "'type' is required on the message" }
        connectionHandler.broadcastToObserverAndControllers(gson.toJson(msg))
    }

    private fun broadcastToAll(msg: Message) {
        requireNotNull(msg.type) { "'type' is required on the message" }
        connectionHandler.broadcastToObserverAndControllers(gson.toJson(msg))
        connectionHandler.broadcast(participants, gson.toJson(msg))
    }

    private fun sendBotListUpdateToObservers() {
        broadcastToObserverAndControllers(botListUpdateMessage)
    }

    internal fun sendBotListUpdate(conn: WebSocket) {
        send(conn, botListUpdateMessage)
    }

    internal fun handleBotJoined() {
        updateBotListUpdateMessage()
        sendBotListUpdateToObservers()
    }

    internal fun handleBotLeft(conn: WebSocket) {
        if (participants.remove(conn) && participants.isEmpty() &&
            (serverState === ServerState.GAME_RUNNING || serverState === ServerState.GAME_PAUSED)
        ) {
            handleAbortGame() // Abort the battle when all bots left it!
        }

        // If a bot leaves while in a game, make sure to reset all intent values to zeroes
        botIntents[conn]?.disableMovement()
        updateBotListUpdateMessage()
        sendBotListUpdateToObservers()
    }

    internal fun handleBotReady(conn: WebSocket) {
        if (serverState === ServerState.WAIT_FOR_READY_PARTICIPANTS) {
            readyParticipants += conn
            startGameIfParticipantsReady()
        }
    }

    internal fun handleBotIntent(conn: WebSocket, intent: BotIntent) {
        if (!participants.contains(conn)) return

        // Update bot intent
        (botIntents[conn] ?: dev.robocode.tankroyale.server.model.BotIntent()).apply {
            update(BotIntentMapper.map(intent))
            botIntents[conn] = this
        }

        // If all bot intents have been received, we can start next turn
        botsThatSentIntent += conn
        if (botIntents.size == botsThatSentIntent.size) {
            botsThatSentIntent.clear()
            turnTimeoutTimer?.reset()
            resetTurnTimeout()
        }
    }

    internal fun handleStartGame(gameSetup: GameSetup, botAddresses: Collection<BotAddress>) {
        this.gameSetup = GameSetupMapper.map(gameSetup)

        participants.apply {
            clear()
            this += connectionHandler.getBotConnections(botAddresses)

            if (isNotEmpty()) {
                prepareGame()
            }
        }
    }

    internal fun handleAbortGame() {
        log.info("Aborting game")
        serverState = ServerState.GAME_STOPPED
        broadcastGameAborted()
        cleanupAfterGameStopped()

        // No score is generated for aborted games
    }

    internal fun handlePauseGame() {
        if (serverState === ServerState.GAME_RUNNING) {
            log.info("Pausing game")
            serverState = ServerState.GAME_PAUSED
            turnTimeoutTimer?.pause()
            broadcastGamedPausedToObservers()
        }
    }

    internal fun handleResumeGame() {
        if (serverState === ServerState.GAME_PAUSED) {
            log.info("Resuming game")
            serverState = ServerState.GAME_RUNNING
            turnTimeoutTimer?.resume()
            broadcastGameResumedToObservers()
        }
    }

    internal fun handleNextTurn() {
        if (serverState === ServerState.GAME_PAUSED) {
            handleResumeGame()
            onNextTurn()
            handlePauseGame()
        }
    }

    internal fun handleChangeTps(newTps: Int) {
        if (tps == newTps) return
        tps = newTps

        broadcastTpsChangedToObservers(newTps)

        if (tps == 0) {
            handlePauseGame()
        } else {
            if (serverState === ServerState.GAME_PAUSED) {
                handleResumeGame()
            }
            resetTurnTimeout()
        }
    }

    private fun cleanupAfterGameStopped() {
        turnTimeoutTimer?.stop()
        lastResetTurnTimeoutPeriod = 0

        modelUpdater = null
        System.gc()
    }
}