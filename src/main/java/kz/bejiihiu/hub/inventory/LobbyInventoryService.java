package kz.bejiihiu.hub.inventory;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import kz.bejiihiu.hub.cloud.CloudNetFacade;
import kz.bejiihiu.hub.cloud.ServiceFilter;
import kz.bejiihiu.hub.config.SelectorConfig;
import kz.bejiihiu.hub.model.PlayerContext;
import kz.bejiihiu.hub.model.ServiceView;
import kz.bejiihiu.hub.selection.LobbySelectionPolicy;
import kz.bejiihiu.hub.telemetry.TelemetryService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public record LobbyInventoryService(JavaPlugin plugin,
										Supplier<SelectorConfig> configSupplier,
										CloudNetFacade cloudNetFacade,
										ServiceFilter serviceFilter,
										SelectorItemFactory itemFactory,
										LobbySelectionPolicy selectionPolicy,
										TelemetryService telemetry,
										NamespacedKey actionKey) {
	private static final int PAGE_SIZE = 45;
	private static final int MIN_INVENTORY_SIZE = 54;
	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

	public Inventory buildFor(PlayerContext context, int page, LobbyCategory category) {
		this.telemetry.onInventoryOpen();
		SelectorConfig config = this.configSupplier.get();
		if (this.cloudNetFacade.cloudServiceProvider().isEmpty() || this.cloudNetFacade.wrapperConfiguration().isEmpty()) {
			return fallbackInventory();
		}

		List<ServiceView> views = this.collectViews(context.allowSilentLobby(), category);
		this.sortViews(views, config.selectionPolicy().strategy());
		int maxPage = Math.max(0, (views.size() - 1) / PAGE_SIZE);
		int resolvedPage = Math.max(0, Math.min(page, maxPage));
		int from = resolvedPage * PAGE_SIZE;
		int to = Math.min(from + PAGE_SIZE, views.size());

		LobbyInventoryHolder holder = new LobbyInventoryHolder();
		holder.setPage(resolvedPage);
		holder.setCategory(category);
		Inventory inventory = this.plugin.getServer().createInventory(holder, MIN_INVENTORY_SIZE, LEGACY.deserialize(config.inventoryTitle()));
		holder.setInventory(inventory);

		List<ServiceView> pageServices = views.subList(from, to);
		List<ItemStack> items = pageServices.stream()
				.map(view -> this.itemFactory.create(view, this.serviceFilter.isSilentHubService(view.service())))
				.filter(Objects::nonNull)
				.toList();
		for (int i = 0; i < items.size(); i++) {
			inventory.setItem(i, items.get(i));
		}
		setControlItems(inventory, config, context, pageServices, resolvedPage, maxPage, category, views);
		return inventory;
	}

	public Inventory buildFor(PlayerContext context) {
		return buildFor(context, 0, LobbyCategory.ALL);
	}

	public Optional<String> smartJoinTarget(PlayerContext context) {
		SelectorConfig config = this.configSupplier.get();
		List<ServiceView> views = collectViews(context.allowSilentLobby(), LobbyCategory.ALL);
		return this.selectionPolicy.selectBest(views, config.selectionPolicy().strategy()).map(ServiceView::serviceName);
	}

	public List<ServiceView> allJoinable(PlayerContext context) {
		return this.collectViews(context.allowSilentLobby(), LobbyCategory.ALL);
	}

	private void sortViews(List<ServiceView> views, String strategy) {
		if ("balanced".equals(strategy)) {
			views.sort(Comparator.comparingDouble(this::balanceRatio));
			return;
		}
		views.sort(Comparator.comparingInt(ServiceView::players).thenComparing(ServiceView::serviceName));
	}

	private double balanceRatio(ServiceView view) {
		if (view.maxPlayers() <= 0) {
			return 1.0;
		}
		return Math.abs(((double) view.players() / view.maxPlayers()) - 0.55);
	}

	private void setControlItems(Inventory inventory, SelectorConfig config, PlayerContext context, List<ServiceView> pageServices,
									 int page, int maxPage, LobbyCategory category, List<ServiceView> allViews) {
		inventory.setItem(45, actionItem(Material.ARROW, "&ePrev", "page_prev"));
		inventory.setItem(46, actionItem(Material.COMPASS, "&bCategory: " + category.name(), "category_next"));
		inventory.setItem(47, actionItem(Material.ARROW, "&eNext", "page_next"));
		if (config.features().smartJoin()) {
			inventory.setItem(49, actionItem(Material.NETHER_STAR, "&aSmart Join", "smart_join"));
		}
		inventory.setItem(53, actionItem(Material.PAPER, "&7Page " + (page + 1) + "/" + (maxPage + 1), "noop"));
	}

	private ItemStack actionItem(Material material, String name, String action) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.displayName(LEGACY.deserialize(name));
		meta.getPersistentDataContainer().set(this.actionKey, PersistentDataType.STRING, action);
		item.setItemMeta(meta);
		return item;
	}

	private Inventory fallbackInventory() {
		LobbyInventoryHolder holder = new LobbyInventoryHolder();
		Inventory inventory = this.plugin.getServer().createInventory(holder, 9, LEGACY.deserialize("&c&mLobby Selector"));
		holder.setInventory(inventory);
		return inventory;
	}

	private List<ServiceView> collectViews(boolean allowSilentLobby, LobbyCategory category) {
		SelectorConfig config = this.configSupplier.get();
		List<ServiceInfoSnapshot> services = new ArrayList<>();
		if (allowSilentLobby && config.enableSilentLobby()) {
			services.addAll(this.cloudNetFacade.servicesByTask(config.silentLobbyTask()));
		}
		services.addAll(this.cloudNetFacade.servicesByTask(config.lobbyTask()));
		Collections.reverse(services);

		return services.stream()
				.map(this::toServiceView)
				.filter(Objects::nonNull)
				.filter(view -> !config.hideFullServices() || view.players() < view.maxPlayers() || view.currentService())
				.filter(view -> matchesCategory(view, category))
				.toList();
	}

	private boolean matchesCategory(ServiceView view, LobbyCategory category) {
		if (category == LobbyCategory.ALL) {
			return true;
		}
		if (category == LobbyCategory.FREE) {
			return view.players() < view.maxPlayers();
		}
		if (category == LobbyCategory.NEAR_FULL) {
			if (view.maxPlayers() <= 0) {
				return false;
			}
			return ((double) view.players() / view.maxPlayers()) >= 0.8;
		}
		return this.serviceFilter.isSilentHubService(view.service());
	}

	private ServiceView toServiceView(ServiceInfoSnapshot service) {
		if (!Boolean.TRUE.equals(service.readProperty(BridgeDocProperties.IS_ONLINE))) {
			return null;
		}
		boolean current = this.cloudNetFacade.currentServiceName().map(name -> name.equals(service.name())).orElse(false);
		int players = service.readProperty(BridgeDocProperties.PLAYERS).size();
		int maxPlayers = service.readProperty(BridgeDocProperties.MAX_PLAYERS);
		return new ServiceView(service, current, players, maxPlayers);
	}
}
