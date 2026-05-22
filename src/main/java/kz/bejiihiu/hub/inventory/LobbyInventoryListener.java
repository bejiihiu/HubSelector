package kz.bejiihiu.hub.inventory;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import kz.bejiihiu.hub.cloud.CloudNetFacade;
import kz.bejiihiu.hub.cloud.ServiceFilter;
import kz.bejiihiu.hub.config.SelectorConfig;
import kz.bejiihiu.hub.connection.ConnectResult;
import kz.bejiihiu.hub.connection.ConnectionService;
import kz.bejiihiu.hub.event.LobbyConnectFailureEvent;
import kz.bejiihiu.hub.event.LobbyConnectSuccessEvent;
import kz.bejiihiu.hub.event.LobbySelectPostEvent;
import kz.bejiihiu.hub.event.LobbySelectPreEvent;
import kz.bejiihiu.hub.model.PlayerContext;
import kz.bejiihiu.hub.telemetry.TelemetryService;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public record LobbyInventoryListener(JavaPlugin plugin,
									Supplier<SelectorConfig> configSupplier,
									CloudNetFacade cloudNetFacade,
									ServiceFilter serviceFilter,
									LobbyInventoryService inventoryService,
									ConnectionService connectionService,
									TelemetryService telemetry,
									NamespacedKey serviceKey,
									NamespacedKey actionKey) implements Listener {
	private static final Set<UUID> CONNECTING_PLAYERS = ConcurrentHashMap.newKeySet();

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getInventory().getHolder() instanceof LobbyInventoryHolder holder)) {
			return;
		}
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		event.setCancelled(true);

		ItemMeta clickedMeta = event.getCurrentItem() == null ? null : event.getCurrentItem().getItemMeta();
		if (clickedMeta == null) {
			return;
		}

		String action = clickedMeta.getPersistentDataContainer().get(this.actionKey, PersistentDataType.STRING);
		if (action != null && handleAction(player, holder, action)) {
			return;
		}

		String serviceName = clickedMeta.getPersistentDataContainer().get(this.serviceKey, PersistentDataType.STRING);
		if (serviceName == null || serviceName.isBlank()) {
			return;
		}
		ServiceInfoSnapshot service = this.cloudNetFacade.serviceByName(serviceName).orElse(null);
		if (service == null || !this.serviceFilter.isValidLobbyService(service, player.hasPermission("lobbyselector.silentlobby"))) {
			return;
		}

		connectTo(player, serviceName);
	}

	private boolean handleAction(Player player, LobbyInventoryHolder holder, String action) {
		PlayerContext context = new PlayerContext(player.hasPermission("lobbyselector.silentlobby"));
		switch (action) {
			case "page_prev" -> player.openInventory(this.inventoryService.buildFor(context, holder.getPage() - 1, holder.getCategory()));
			case "page_next" -> player.openInventory(this.inventoryService.buildFor(context, holder.getPage() + 1, holder.getCategory()));
			case "category_next" -> player.openInventory(this.inventoryService.buildFor(context, 0, holder.getCategory().next()));
			case "smart_join" -> {
				this.telemetry.onSmartJoinClick();
				String target = this.inventoryService.smartJoinTarget(context).orElse(null);
				if (target == null) {
					player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize("&cNo joinable lobbies found"));
					return true;
				}
				connectTo(player, target);
			}
			default -> { return false; }
		}
		return true;
	}

	private void connectTo(Player player, String serviceName) {
		if (!CONNECTING_PLAYERS.add(player.getUniqueId())) {
			player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
					.deserialize(this.configSupplier.get().messages().alreadyConnecting()));
			return;
		}
		try {
			this.plugin.getServer().getPluginManager().callEvent(new LobbySelectPreEvent(player, serviceName));
			ConnectResult result = this.connectionService.connect(
					player,
					serviceName,
					this.inventoryService.allJoinable(new PlayerContext(player.hasPermission("lobbyselector.silentlobby")))
			);
			this.plugin.getServer().getPluginManager().callEvent(new LobbySelectPostEvent(player, serviceName));
			if (result.success()) {
				this.plugin.getServer().getPluginManager().callEvent(new LobbyConnectSuccessEvent(player, result.connectedService()));
				player.closeInventory();
				return;
			}
			if (result.playerMessage() != null && !result.playerMessage().isBlank()) {
				player.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(result.playerMessage()));
			}
			this.plugin.getServer().getPluginManager().callEvent(new LobbyConnectFailureEvent(player, serviceName, result.failureReason()));
		} finally {
			CONNECTING_PLAYERS.remove(player.getUniqueId());
		}
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		if (!(event.getInventory().getHolder() instanceof LobbyInventoryHolder)) {
			return;
		}
		event.setCancelled(true);
	}
}
