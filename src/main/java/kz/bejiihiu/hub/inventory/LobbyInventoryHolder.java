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
	private int page;
	private LobbyCategory category = LobbyCategory.ALL;

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

	public int getPage() {
		return this.page;
	}

	public void setPage(int page) {
		this.page = Math.max(0, page);
	}

	public LobbyCategory getCategory() {
		return this.category;
	}

	public void setCategory(LobbyCategory category) {
		this.category = category == null ? LobbyCategory.ALL : category;
	}
}
