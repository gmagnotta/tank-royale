using Newtonsoft.Json;

namespace Robocode.TankRoyale.BotApi.Events
{
  /// <summary>
  /// Event occurring when a bot has died.
  /// </summary>
  public sealed class DeathEvent : BotEvent
  {
    /// <summary>The id of the bot that has died.</summary>
    public int VictimId { get; }

    /// <summary>Initializes a new instance of the DeathEvent class.</summary>
    /// <param name="turnNumber">Turn number.</param>
    /// <param name="victimId">Id of the bot that has died.</param>
    [JsonConstructor]
    public DeathEvent(int turnNumber, int victimId) : base(turnNumber) => VictimId = victimId;
  }
}