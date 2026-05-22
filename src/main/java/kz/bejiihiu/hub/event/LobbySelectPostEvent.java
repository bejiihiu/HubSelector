package kz.bejiihiu.hub.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LobbySelectPostEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Player player;
	private final String serviceName;

	public LobbySelectPostEvent(Player player, String serviceName) {
		this.player = player;
		this.serviceName = serviceName;
	}

	public Player player() { return this.player; }
	public String serviceName() { return this.serviceName; }
	@Override public HandlerList getHandlers() { return HANDLERS; }
	public static HandlerList getHandlerList() { return HANDLERS; }
}
