package kz.bejiihiu.hub.inventory;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import kz.bejiihiu.hub.cloud.CloudNetFacade;
import kz.bejiihiu.hub.cloud.ServiceFilter;

/**
 * Handles click/drag interactions in selector inventories.
 */
public record LobbyInventoryListener(CloudNetFacade cloudNetFacade, ServiceFilter serviceFilter,
		NamespacedKey serviceKey) implements Listener {

	/**
	 * Cancels inventory modifications and routes player to selected target service.
	 */
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getInventory().getHolder() instanceof LobbyInventoryHolder)) {
			return;
		}
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		event.setCancelled(true);

		ItemMeta clickedMeta = event.getCurrentItem() == null ? null : event.getCurrentItem().getItemMeta();
		if (clickedMeta == null || !clickedMeta.getPersistentDataContainer().has(this.serviceKey)) {
			return;
		}

		String serviceName = clickedMeta.getPersistentDataContainer().getOrDefault(
				this.serviceKey,
				PersistentDataType.STRING,
				"");
		ServiceInfoSnapshot service = this.cloudNetFacade.serviceByName(serviceName).orElse(null);
		if (service == null) {
			return;
		}

		if (!this.serviceFilter.isValidLobbyService(service, player.hasPermission("hub.silenthub"))) {
			return;
		}
		if (this.cloudNetFacade.currentServiceName().map(serviceName::equals).orElse(false)) {
			return;
		}

		boolean connected = this.cloudNetFacade.connectPlayerToService(player.getUniqueId(), service.name());
		if (connected) {
			player.closeInventory();
		}
	}

	/**
	 * Prevents drag edits inside selector inventory.
	 */
	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		if (!(event.getInventory().getHolder() instanceof LobbyInventoryHolder)) {
			return;
		}
		event.setCancelled(true);
	}
}
