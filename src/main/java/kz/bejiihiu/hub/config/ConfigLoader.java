package kz.bejiihiu.hub.config;

import net.jandie1505.configmanager.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

/**
 * Handles disk-backed config loading through JSONConfigManager.
 */
public class ConfigLoader {
	private final ConfigManager configManager;

	public ConfigLoader(JavaPlugin plugin, JSONObject defaults) {
		this.configManager = new ConfigManager(defaults, false, plugin.getDataFolder(), "config.json");
	}

	/**
	 * Reloads config file and maps it to typed selector configuration.
	 */
	public SelectorConfig load() {
		this.configManager.reloadConfig();
		return new SelectorConfig(this.configManager.getConfig());
	}
}
