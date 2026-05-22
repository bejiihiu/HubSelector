package kz.bejiihiu.hub.config;

import kz.bejiihiu.hub.model.ItemTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed configuration boundary for runtime logic.
 * Converts JSON payload into strongly-typed values consumed by other modules.
 */
public class SelectorConfig {
	public static final String KEY_SERVICE = "service";
	public static final String KEY_ENABLE_SILENT_LOBBY = "enableSilentLobby";
	public static final String KEY_SILENT_LOBBY_TASK = "silentLobbyTask";
	public static final String KEY_LOBBY_TASK = "lobbyTask";
	public static final String KEY_INVENTORY_TITLE = "inventoryTitle";
	public static final String KEY_HIDE_FULL_SERVICES = "hideFullServices";
	public static final String KEY_SERVER_ITEMS = "serverItems";
	public static final String KEY_MATERIAL = "material";
	public static final String KEY_NAME = "name";
	public static final String KEY_LORE = "lore";
	public static final String KEY_ENCHANTED = "enchanted";
	public static final String KEY_CUSTOM_MODEL_DATA = "customModelData";

	public static final String TASK_LOBBY_DEFAULT = "Lobby";
	public static final String TASK_SILENT_LOBBY_DEFAULT = "SilentLobby";

	private final String lobbyTask;
	private final boolean enableSilentLobby;
	private final String silentLobbyTask;
	private final String inventoryTitle;
	private final boolean hideFullServices;
	private final ItemTemplate defaultItem;
	private final ItemTemplate silentHubItem;
	private final ItemTemplate fullItem;
	private final ItemTemplate currentItem;

	public SelectorConfig(JSONObject config) {
		this.lobbyTask = config.optString(KEY_LOBBY_TASK, TASK_LOBBY_DEFAULT);
		this.enableSilentLobby = config.optBoolean(KEY_ENABLE_SILENT_LOBBY, false);
		this.silentLobbyTask = config.optString(KEY_SILENT_LOBBY_TASK, TASK_SILENT_LOBBY_DEFAULT);
		this.inventoryTitle = config.optString(KEY_INVENTORY_TITLE, "Lobby Selector:");
		this.hideFullServices = config.optBoolean(KEY_HIDE_FULL_SERVICES, false);

		JSONObject serverItemsConfig = config.optJSONObject(KEY_SERVER_ITEMS, new JSONObject());
		this.defaultItem = readTemplate(serverItemsConfig.optJSONObject("default"));
		this.silentHubItem = readTemplate(serverItemsConfig.optJSONObject("silentHub"));
		this.fullItem = readTemplate(serverItemsConfig.optJSONObject("full"));
		this.currentItem = readTemplate(serverItemsConfig.optJSONObject("current"));
	}

	public String lobbyTask() {
		return this.lobbyTask;
	}

	public boolean enableSilentLobby() {
		return this.enableSilentLobby;
	}

	public String silentLobbyTask() {
		return this.silentLobbyTask;
	}

	public String inventoryTitle() {
		return this.inventoryTitle;
	}

	public boolean hideFullServices() {
		return this.hideFullServices;
	}

	public ItemTemplate defaultItem() {
		return this.defaultItem;
	}

	public ItemTemplate silentHubItem() {
		return this.silentHubItem;
	}

	public ItemTemplate fullItem() {
		return this.fullItem;
	}

	public ItemTemplate currentItem() {
		return this.currentItem;
	}

	/**
	 * Reads one template variant and applies defensive defaults.
	 */
	private static ItemTemplate readTemplate(JSONObject json) {
		JSONObject source = json == null ? new JSONObject() : json;
		List<String> lore = new ArrayList<>();
		JSONArray loreArray = source.optJSONArray(KEY_LORE);
		if (loreArray != null) {
			for (Object value : loreArray) {
				if (value instanceof String text) {
					lore.add(text);
				}
			}
		}

		return new ItemTemplate(
				source.optString(KEY_MATERIAL, ""),
				source.optString(KEY_NAME, "INVALID STRING VALUE"),
				lore,
				source.optBoolean(KEY_ENCHANTED, false),
				source.optInt(KEY_CUSTOM_MODEL_DATA, -1)
		);
	}
}
