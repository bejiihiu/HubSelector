package kz.bejiihiu.hub.command;

import kz.bejiihiu.hub.config.SelectorConfig;
import kz.bejiihiu.hub.inventory.LobbyInventoryService;
import kz.bejiihiu.hub.model.PlayerContext;
import kz.bejiihiu.hub.telemetry.TelemetryService;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public record LobbySelectorCommand(JavaPlugin plugin,
									LobbyInventoryService inventoryService,
									TelemetryService telemetryService,
									Runnable reloadAction,
									Supplier<SelectorConfig> configSupplier,
									AtomicBoolean debugFlag) implements TabExecutor {
	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0) {
			return openSelf(sender);
		}
		String sub = args[0].toLowerCase(Locale.ROOT);
		switch (sub) {
			case "reload" -> {
				if (!sender.hasPermission("lobbyselector.admin.reload")) {
					sender.sendMessage(LEGACY.deserialize("&cNo permission"));
					return true;
				}
				this.reloadAction.run();
				sender.sendMessage(LEGACY.deserialize("&aHubSelector config reloaded"));
				return true;
			}
			case "debug" -> {
				if (!sender.hasPermission("lobbyselector.admin.debug")) {
					sender.sendMessage(LEGACY.deserialize("&cNo permission"));
					return true;
				}
				if (args.length < 2) {
					sender.sendMessage(LEGACY.deserialize("&eUsage: /lobbyselector debug <on|off>"));
					return true;
				}
				boolean enabled = args[1].equalsIgnoreCase("on");
				this.debugFlag.set(enabled);
				sender.sendMessage(LEGACY.deserialize(enabled ? "&aDebug enabled" : "&cDebug disabled"));
				return true;
			}
			case "stats" -> {
				if (!sender.hasPermission("lobbyselector.admin.stats")) {
					sender.sendMessage(LEGACY.deserialize("&cNo permission"));
					return true;
				}
				sender.sendMessage(LEGACY.deserialize("&7" + this.telemetryService.summary()));
				return true;
			}
			case "open" -> {
				if (!sender.hasPermission("lobbyselector.admin.open")) {
					sender.sendMessage(LEGACY.deserialize("&cNo permission"));
					return true;
				}
				if (args.length < 2) {
					sender.sendMessage(LEGACY.deserialize("&eUsage: /lobbyselector open <player>"));
					return true;
				}
				Player target = Bukkit.getPlayerExact(args[1]);
				if (target == null) {
					sender.sendMessage(LEGACY.deserialize("&cPlayer not found"));
					return true;
				}
				target.openInventory(this.inventoryService.buildFor(new PlayerContext(target.hasPermission("lobbyselector.silentlobby"))));
				sender.sendMessage(LEGACY.deserialize("&aSelector opened for " + target.getName()));
				return true;
			}
			default -> {
				sender.sendMessage(LEGACY.deserialize("&eUnknown subcommand"));
				return true;
			}
		}
	}

	private boolean openSelf(CommandSender sender) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage(LEGACY.deserialize("&cCommand can only be executed by players"));
			return true;
		}
		if (!sender.hasPermission("lobbyselector.use") && !sender.hasPermission("hub.use")) {
			sender.sendMessage(LEGACY.deserialize("&cNo permission"));
			return true;
		}
		player.openInventory(this.inventoryService.buildFor(new PlayerContext(player.hasPermission("lobbyselector.silentlobby"))));
		return true;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
		if (args.length == 1) {
			return List.of("reload", "debug", "stats", "open");
		}
		if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
			return List.of("on", "off");
		}
		return List.of();
	}
}
