package kz.bejiihiu.hub.command;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import kz.bejiihiu.hub.inventory.LobbyInventoryService;
import kz.bejiihiu.hub.model.PlayerContext;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Command endpoint that validates permissions and opens selector inventory.
 */
public record LobbySelectorCommand(LobbyInventoryService inventoryService) implements TabExecutor {
	private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

	@Override
	public boolean onCommand(
			@NotNull CommandSender sender,
			@NotNull Command cmd,
			@NotNull String label,
			@NotNull String @NotNull [] args) {
		// Selector GUI is player-only because inventory APIs require a player context.
		if (!(sender instanceof Player player)) {
			sender.sendMessage(LEGACY_SERIALIZER.deserialize("&cCommand can only be executed by players"));
			return true;
		}
		if (!sender.hasPermission("hub.use")) {
			sender.sendMessage(LEGACY_SERIALIZER.deserialize("&cNo permission"));
			return true;
		}

		player.openInventory(this.inventoryService.buildFor(
				new PlayerContext(sender.hasPermission("hub.silentlobby"))));
		return false;
	}

	@Override
	public List<String> onTabComplete(
			@NotNull CommandSender sender,
			@NotNull Command cmd,
			@NotNull String label,
			@NotNull String @NotNull [] args) {
		// No command arguments are currently supported.
		return List.of();
	}
}
