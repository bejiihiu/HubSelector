package kz.bejiihiu.hub.model;

import java.util.List;

/**
 * Immutable item-template projection from config.
 */
public record ItemTemplate(
		String material,
		String name,
		List<String> lore,
		boolean enchanted,
		int customModelData
) {
}
