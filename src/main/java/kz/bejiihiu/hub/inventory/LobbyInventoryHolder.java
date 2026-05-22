package kz.bejiihiu.hub.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Marker holder for selector inventories.
 * Allows listeners to distinguish plugin-managed inventories from others.
 */
public class LobbyInventoryHolder implements InventoryHolder {
	private Inventory inventory;

	/**
	 * Injects created Bukkit inventory reference for holder identity checks.
	 */
	public void setInventory(Inventory inventory) {
		this.inventory = inventory;
	}

	@Override
	public @NotNull Inventory getInventory() {
		return this.inventory;
	}
}
