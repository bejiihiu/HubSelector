package kz.bejiihiu.hub.inventory;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import kz.bejiihiu.hub.config.SelectorConfig;
import kz.bejiihiu.hub.model.ItemTemplate;
import kz.bejiihiu.hub.model.ServiceView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Renders Bukkit item stacks from service state and item templates.
 */
public record SelectorItemFactory(JavaPlugin plugin, SelectorConfig config, PlaceholderService placeholderService,
		NamespacedKey serviceKey) {
	private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

	/**
	 * Builds clickable selector item and tags it with service name in persistent
	 * metadata.
	 */
	public ItemStack create(ServiceView serviceView, boolean isSilentHub) {
		ItemTemplate template = selectTemplate(serviceView, isSilentHub);
		ItemStack item = this.buildItem(template, serviceView);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			meta = this.plugin.getServer().getItemFactory().getItemMeta(item.getType());
		}
		if (meta == null) {
			return null;
		}

		meta.getPersistentDataContainer().set(this.serviceKey, PersistentDataType.STRING, serviceView.service().name());
		item.setItemMeta(meta);
		return item;
	}

	/**
	 * Picks configured template variant by current/full/silent/default priority.
	 */
	private ItemTemplate selectTemplate(ServiceView serviceView, boolean isSilentHub) {
		if (serviceView.currentService()) {
			return this.config.currentItem();
		}
		if (serviceView.players() >= serviceView.maxPlayers()) {
			return this.config.fullItem();
		}
		if (isSilentHub) {
			return this.config.silentHubItem();
		}
		return this.config.defaultItem();
	}

	/**
	 * Converts template + placeholders into final item stack.
	 * Invalid materials are represented with an explicit barrier fallback.
	 */
	private ItemStack buildItem(ItemTemplate template, ServiceView serviceView) {
		Material type = Material.getMaterial(template.material());
		if (type == null) {
			ItemStack errorItem = new ItemStack(Material.BARRIER);
			ItemMeta errorMeta = this.plugin.getServer().getItemFactory().getItemMeta(errorItem.getType());
			if (errorMeta != null) {
				errorMeta.displayName(Component.text("INVALID ITEM CONFIGURATION"));
				errorItem.setItemMeta(errorMeta);
			}
			return errorItem;
		}

		ItemStack item = new ItemStack(type);
		ItemMeta meta = this.plugin.getServer().getItemFactory().getItemMeta(item.getType());
		if (meta == null) {
			return item;
		}

		String name = this.placeholderService.apply(
				template.name(),
				serviceView.service().name(),
				serviceView.players(),
				serviceView.maxPlayers());
		meta.displayName(LEGACY_SERIALIZER.deserialize(name));

		List<Component> lore = new ArrayList<>();
		for (String loreLine : template.lore()) {
			String line = this.placeholderService.apply(
					loreLine,
					serviceView.service().name(),
					serviceView.players(),
					serviceView.maxPlayers());
			lore.add(LEGACY_SERIALIZER.deserialize(line));
		}
		meta.lore(lore);

		if (template.enchanted()) {
			meta.addEnchant(Enchantment.FORTUNE, 1, true);
			meta.addItemFlags(ItemFlag.values());
		}

		if (template.customModelData() >= 0) {
			meta.setCustomModelData(template.customModelData());
		}

		item.setItemMeta(meta);
		return item.clone();
	}
}
