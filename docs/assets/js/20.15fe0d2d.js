(window.webpackJsonp=window.webpackJsonp||[]).push([[20],{312:function(e,t,a){"use strict";a.r(t);var o=a(13),n=Object(o.a)({},(function(){var e=this,t=e._self._c;return t("ContentSlotsDistributor",{attrs:{"slot-key":e.$parent.slotKey}},[t("h1",{attrs:{id:"tank-royale-vs-original-robocode"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#tank-royale-vs-original-robocode"}},[e._v("#")]),e._v(" Tank Royale vs original Robocode")]),e._v(" "),t("h2",{attrs:{id:"same-but-different"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#same-but-different"}},[e._v("#")]),e._v(" Same, but different")]),e._v(" "),t("p",[t("strong",[e._v("Robocode Tank Royale")]),e._v(" is a new version of the "),t("a",{attrs:{href:"https://robocode.sourceforge.io/",target:"_blank",rel:"noopener noreferrer"}},[e._v("original version"),t("OutboundLink")],1),e._v(' of Robocode. Tank\nRoyale has very similar rules as the original Robocode game. However, there are differences and the goal of Tank Royale\nis not to be compatible with the old version. But instead, it is intended to be a better and improved version of the\noriginal game and be able to run Robocode on "any" programming language and via the Internet as well. Note that '),t("em",[e._v("any")]),e._v("\nprogramming language can be used as long as it can use\n"),t("a",{attrs:{href:"https://en.wikipedia.org/wiki/WebSocket",target:"_blank",rel:"noopener noreferrer"}},[e._v("WebSocket"),t("OutboundLink")],1),e._v(".")]),e._v(" "),t("h2",{attrs:{id:"websocket-based"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#websocket-based"}},[e._v("#")]),e._v(" WebSocket based")]),e._v(" "),t("p",[e._v("Tank Royale is written from scratch as it is based on an entirely different architecture and technology stack. The game\nis now using a protocol on top of "),t("a",{attrs:{href:"https://en.wikipedia.org/wiki/WebSocket",target:"_blank",rel:"noopener noreferrer"}},[e._v("WebSocket"),t("OutboundLink")],1),e._v(" for communication between a\nserver, the bots, and observers/controllers like e.g. a UI for displaying and controlling battles.")]),e._v(" "),t("h2",{attrs:{id:"modularized"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#modularized"}},[e._v("#")]),e._v(" Modularized")]),e._v(" "),t("p",[e._v("The game components have been written for the "),t("a",{attrs:{href:"https://www.oracle.com/java/",target:"_blank",rel:"noopener noreferrer"}},[e._v("Java platform"),t("OutboundLink")],1),e._v(" with the\n"),t("a",{attrs:{href:"https://kotlinlang.org/",target:"_blank",rel:"noopener noreferrer"}},[e._v("Kotlin"),t("OutboundLink")],1),e._v(" programming language. But each component can be replaced with another platform and\nprogramming language. This allows components to be replaced by any developer(s) to make variants or e.g. improved UI's\nfor the game etc., and it is possible to develop and "),t("em",[e._v("plug-in")]),e._v(" observers e.g. for analyzing or recording battles.")]),e._v(" "),t("h2",{attrs:{id:"changed-to-use-normal-maths"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#changed-to-use-normal-maths"}},[e._v("#")]),e._v(' Changed to use "normal" maths')]),e._v(" "),t("p",[e._v("Tank Royale is following the same basic rules as the original game. However, game physics have been changed to fit\nnormal mathematics for angles and coordinate systems.")]),e._v(" "),t("p",[e._v("Another goal of the new version Robocode is to make it much easier to develop and maintain as a platform. It should also\nbe possible to make a much more advanced version of Tank Royale in the future (version 2?), e.g. with obstacles and\nitems on the arena.")]),e._v(" "),t("h2",{attrs:{id:"game-components"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#game-components"}},[e._v("#")]),e._v(" Game components")]),e._v(" "),t("p",[e._v("The game has been split into separate components:")]),e._v(" "),t("ul",[t("li",[e._v("Server, which is used for running games and enforcing the rules.")]),e._v(" "),t("li",[e._v("Graphical User Interface (UI) for starting, viewing, and controlling battles.")]),e._v(" "),t("li",[e._v("Booter for booting up bots from a local machine.")])]),e._v(" "),t("h2",{attrs:{id:"run-bots-from-anywhere"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#run-bots-from-anywhere"}},[e._v("#")]),e._v(" Run bots from anywhere")]),e._v(" "),t("p",[e._v("Bots can be run from anywhere as long as they have access to a "),t("a",{attrs:{href:"https://en.wikipedia.org/wiki/WebSocket",target:"_blank",rel:"noopener noreferrer"}},[e._v("WebSocket"),t("OutboundLink")],1),e._v(" and a server. Note that bots are not running\non the server like with the original Robocode game. The server is only taking care of hosting the game and its state.\nThe server receives "),t("strong",[e._v("intents")]),e._v(" from bots and sends out the current game state for bots, observers, and controllers.")]),e._v(" "),t("p",[e._v("Each bot is running in a process somewhere else besides the server. This could be on the same local machine, but it\ncould also be running within a browser session, a cloud server, or another user's machine or device.")]),e._v(" "),t("h2",{attrs:{id:"turn-based"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#turn-based"}},[e._v("#")]),e._v(" Turn-based")]),e._v(" "),t("p",[e._v('Tank Royale is turn-based like the original version of Robocode. However, each turn in Tank Royale is deterministic due\nto the fact, that bots are no longer running in independent threads inside a Java VM, where commands from different\nbots for a turn were executed at "random". Instead, Tank Royale evaluates the commands (intents) from all bots for a\nspecific turn and outputs a new game state.')]),e._v(" "),t("p",[e._v("Hence, the game is stateful and deterministic, and no bot will get an advantage over other bots if a bot thread gets\nmore CPU or memory resources than its competitors on Java VM serving the bot. This also makes the test more stable if\nbots behave in a truly deterministic manner for every turn.")]),e._v(" "),t("h2",{attrs:{id:"bot-intents"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#bot-intents"}},[e._v("#")]),e._v(" Bot intents")]),e._v(" "),t("p",[e._v("For each turn, bots will receive the state of the game. Note that the bots will only see the part of the world that they\nare scanning using their radar. So bots will not be able to see "),t("em",[e._v("all")]),e._v(" other bots on the arena—only the bots they have\njust scanned.")]),e._v(" "),t("p",[e._v("Bot commands are expressed by an "),t("strong",[e._v("intent")]),e._v(" containing these properties:")]),e._v(" "),t("ul",[t("li",[t("strong",[e._v("Turn rate")]),e._v(" of the body in degrees per turn.")]),e._v(" "),t("li",[t("strong",[e._v("Gun turn rate")]),e._v(" in degrees per turn.")]),e._v(" "),t("li",[t("strong",[e._v("Radar turn rate")]),e._v(" in degrees per turn.")]),e._v(" "),t("li",[t("strong",[e._v("Target speed")]),e._v(" in units per turn.")]),e._v(" "),t("li",[t("strong",[e._v("Firepower")]),e._v(" is used for firing the gun if there is no gun heat.")])]),e._v(" "),t("p",[e._v("Setting one of these properties on a bot intent for a new turn means that the property should be changed. If the\nproperty is not set (is omitted) it means that the last value for the property should be used for the next turn, i.e. no\nchange.")]),e._v(" "),t("p",[e._v("The turn rates can be both positive and negative, where a positive value means "),t("em",[e._v("turn to the left")]),e._v(" and a negative value\nmeans "),t("em",[e._v("turn to the right")]),e._v(". The target speed of the bot can be positive and negative as well, where a positive speed\nmeans "),t("em",[e._v("move forward")]),e._v(" and a negative speed mean "),t("em",[e._v("move backward")]),e._v(".")]),e._v(" "),t("h2",{attrs:{id:"time-limit-for-a-turn"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#time-limit-for-a-turn"}},[e._v("#")]),e._v(" Time limit for a turn")]),e._v(" "),t("p",[e._v('With Tank Royale, it is possible to specify the turn time. The turn time is specified in microseconds (μs) and can be\nconfigured for a battle. If a bot exceeds the turn time, it will be punished with "a skipped turn", and its intention\nfor the turn will not be executed until the next or a later turn depending on how much time the bot spends before\nsending its intent to the server.')]),e._v(" "),t("h2",{attrs:{id:"cannot-limit-bot-resources"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#cannot-limit-bot-resources"}},[e._v("#")]),e._v(" Cannot limit bot resources")]),e._v(" "),t("p",[e._v("Since bots are running in their processes independent of the server, the Tank Royale game will not be able to constrain\nhow much CPU, RAM, disk space, etc. the bot is allowed to use. This was somewhat possible to some extent with the\noriginal Robocode game, as all bots were running within the same Java VM sharing the same resources.")]),e._v(" "),t("h2",{attrs:{id:"no-editor-or-compiler-is-built-in"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#no-editor-or-compiler-is-built-in"}},[e._v("#")]),e._v(" No editor or compiler is built-in")]),e._v(" "),t("p",[e._v("The GUI for Tank Royale does not provide a source code editor and a compiler like the original Robocode. It is up to the\nbot developers to find a suitable editor, and the compiler depends on which programming language will be used for\ndeveloping a bot. With the original Robocode, Java was the primary programming language and platform.")]),e._v(" "),t("h2",{attrs:{id:"no-robot-packager-is-available"}},[t("a",{staticClass:"header-anchor",attrs:{href:"#no-robot-packager-is-available"}},[e._v("#")]),e._v(" No Robot Packager is available")]),e._v(" "),t("p",[e._v("The original Robocode provided a Robot Packager, where Java robots are packaged into robot Java archive files as\n"),t("em",[e._v("Robot Packages")]),e._v(" and .Net bots were packaged into DLL files.")]),e._v(" "),t("p",[e._v("With Robocode Tank Royale, you should consider packaging all required files for a bot into a zipped file and provide\nscripts for starting up your bot. You can read more about this with the "),t("RouterLink",{attrs:{to:"/articles/booter.html"}},[e._v("booter")]),e._v(".")],1),e._v(" "),t("p",[e._v("You should also consider using to use containerization with a container technology if the setup for your bot(s)\nis complex. But this is out of scope for Tank Royale.")])])}),[],!1,null,null,null);t.default=n.exports}}]);