package kz.bejiihiu.hub.config;

import kz.bejiihiu.hub.model.ItemTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
	public static final String KEY_FEATURES = "features";
	public static final String KEY_SELECTION_POLICY = "selectionPolicy";
	public static final String KEY_MESSAGES = "messages";
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
	private final Features features;
	private final SelectionPolicy selectionPolicy;
	private final Messages messages;

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
		this.features = readFeatures(config.optJSONObject(KEY_FEATURES));
		this.selectionPolicy = readSelectionPolicy(config.optJSONObject(KEY_SELECTION_POLICY));
		this.messages = readMessages(config.optJSONObject(KEY_MESSAGES));
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

	public Features features() {
		return this.features;
	}

	public SelectionPolicy selectionPolicy() {
		return this.selectionPolicy;
	}

	public Messages messages() {
		return this.messages;
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

	private static Features readFeatures(JSONObject json) {
		JSONObject source = json == null ? new JSONObject() : json;
		return new Features(
				source.optBoolean("pagination", true),
				source.optBoolean("smartJoin", true),
				source.optBoolean("retryConnect", true),
				source.optBoolean("fallbackConnect", true),
				source.optBoolean("telemetry", true),
				source.optBoolean("debug", false)
		);
	}

	private static SelectionPolicy readSelectionPolicy(JSONObject json) {
		JSONObject source = json == null ? new JSONObject() : json;
		String mode = normalizeMode(source.optString("mode", "manual"));
		String strategy = normalizeStrategy(source.optString("strategy", "leastPlayers"));
		return new SelectionPolicy(mode, strategy);
	}

	private static Messages readMessages(JSONObject json) {
		JSONObject source = json == null ? new JSONObject() : json;
		return new Messages(
				source.optString("connecting", "&aConnecting to {service}..."),
				source.optString("alreadyConnecting", "&eConnection already in progress"),
				source.optString("serviceFull", "&cService is full"),
				source.optString("serviceOffline", "&cService is offline"),
				source.optString("retry", "&eRetrying connection..."),
				source.optString("fallback", "&eSelected lobby unavailable. Trying {service}"),
				source.optString("failed", "&cConnection failed. Try again")
		);
	}

	private static String normalizeMode(String mode) {
		String value = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
		if (value.equals("smart")) {
			return "smart";
		}
		return "manual";
	}

	private static String normalizeStrategy(String strategy) {
		String value = strategy == null ? "" : strategy.trim().toLowerCase(Locale.ROOT);
		if (value.equals("lowestping")) {
			return "lowestPing";
		}
		if (value.equals("balanced")) {
			return "balanced";
		}
		return "leastPlayers";
	}

	public record Features(
			boolean pagination,
			boolean smartJoin,
			boolean retryConnect,
			boolean fallbackConnect,
			boolean telemetry,
			boolean debug
	) {
	}

	public record SelectionPolicy(String mode, String strategy) {
	}

	public record Messages(
			String connecting,
			String alreadyConnecting,
			String serviceFull,
			String serviceOffline,
			String retry,
			String fallback,
			String failed
	) {
	}
}
