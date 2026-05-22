package kz.bejiihiu.hub.event;

import kz.bejiihiu.hub.telemetry.ConnectFailureReason;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LobbyConnectFailureEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Player player;
	private final String serviceName;
	private final ConnectFailureReason reason;

	public LobbyConnectFailureEvent(Player player, String serviceName, ConnectFailureReason reason) {
		this.player = player;
		this.serviceName = serviceName;
		this.reason = reason;
	}

	public Player player() { return this.player; }
	public String serviceName() { return this.serviceName; }
	public ConnectFailureReason reason() { return this.reason; }
	@Override public HandlerList getHandlers() { return HANDLERS; }
	public static HandlerList getHandlerList() { return HANDLERS; }
}
