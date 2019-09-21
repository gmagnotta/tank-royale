package dev.robocode.tankroyale.server.events;

import lombok.Value;

/**
 * Event sent when a bot collides with another bot.
 * 
 * @author Flemming N. Larsen
 */
@Value
public class BotHitBotEvent implements Event {

	/** Turn number when event occurred */
	int turnNumber;

	/** Bot id of the bot hitting another bot */
	int botId;

	/** Bot id of the victim bot that got hit */
	int victimId;
	
	/** Energy level of the victim */
	double energy;
	
	/** X coordinate of the victim */
	double x;

	/** Y coordinate of the victim */
	double y;

	/** Flag specifying if the victim was rammed */
	boolean rammed;
}