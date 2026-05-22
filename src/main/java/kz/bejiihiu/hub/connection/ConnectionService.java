package kz.bejiihiu.hub.connection;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import kz.bejiihiu.hub.cloud.CloudNetFacade;
import kz.bejiihiu.hub.config.SelectorConfig;
import kz.bejiihiu.hub.model.ServiceView;
import kz.bejiihiu.hub.telemetry.ConnectFailureReason;
import kz.bejiihiu.hub.telemetry.TelemetryService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class ConnectionService {
	private final JavaPlugin plugin;
	private final CloudNetFacade cloudNetFacade;
	private final Supplier<SelectorConfig> configSupplier;
	private final TelemetryService telemetry;

	public ConnectionService(JavaPlugin plugin, CloudNetFacade cloudNetFacade, Supplier<SelectorConfig> configSupplier, TelemetryService telemetry) {
		this.plugin = plugin;
		this.cloudNetFacade = cloudNetFacade;
		this.configSupplier = configSupplier;
		this.telemetry = telemetry;
	}

	public ConnectResult connect(Player player, String serviceName, List<ServiceView> alternatives) {
		SelectorConfig config = this.configSupplier.get();
		long started = System.currentTimeMillis();
		this.telemetry.onConnectAttempt();

		Optional<ServiceInfoSnapshot> target = this.cloudNetFacade.serviceByName(serviceName);
		if (target.isEmpty() || !Boolean.TRUE.equals(target.get().readProperty(BridgeDocProperties.IS_ONLINE))) {
			return this.tryFallback(player, alternatives, ConnectFailureReason.SERVICE_OFFLINE, config.messages().serviceOffline(), config);
		}

		ServiceInfoSnapshot service = target.get();
		int players = service.readProperty(BridgeDocProperties.PLAYERS).size();
		int maxPlayers = service.readProperty(BridgeDocProperties.MAX_PLAYERS);
		if (players >= maxPlayers) {
			return this.tryFallback(player, alternatives, ConnectFailureReason.SERVICE_FULL, config.messages().serviceFull(), config);
		}

		if (this.cloudNetFacade.currentServiceName().map(serviceName::equals).orElse(false)) {
			this.telemetry.onConnectFailure(ConnectFailureReason.ALREADY_CURRENT);
			return new ConnectResult(false, ConnectFailureReason.ALREADY_CURRENT, null, config.messages().failed());
		}

		player.sendMessage(colorize(config.messages().connecting().replace("{service}", serviceName)));
		boolean connected = this.connectWithRetry(player.getUniqueId(), serviceName, config);
		if (connected) {
			this.telemetry.onConnectSuccess(System.currentTimeMillis() - started);
			return new ConnectResult(true, null, serviceName, null);
		}

		this.telemetry.onConnectFailure(ConnectFailureReason.BRIDGE_ERROR);
		return this.tryFallback(player, alternatives, ConnectFailureReason.BRIDGE_ERROR, config.messages().failed(), config);
	}

	private ConnectResult tryFallback(Player player, List<ServiceView> alternatives, ConnectFailureReason reason, String baseMessage, SelectorConfig config) {
		this.telemetry.onConnectFailure(reason);
		if (!config.features().fallbackConnect()) {
			return new ConnectResult(false, reason, null, baseMessage);
		}

		Optional<ServiceView> alternative = alternatives.stream()
				.filter(view -> !view.currentService() && view.players() < view.maxPlayers())
				.findFirst();
		if (alternative.isEmpty()) {
			this.telemetry.onConnectFailure(ConnectFailureReason.NO_ALTERNATIVE);
			return new ConnectResult(false, ConnectFailureReason.NO_ALTERNATIVE, null, baseMessage);
		}

		String fallbackService = alternative.get().serviceName();
		player.sendMessage(colorize(config.messages().fallback().replace("{service}", fallbackService)));
		boolean connected = this.connectWithRetry(player.getUniqueId(), fallbackService, config);
		if (connected) {
			return new ConnectResult(true, null, fallbackService, null);
		}
		this.telemetry.onConnectFailure(ConnectFailureReason.BRIDGE_ERROR);
		return new ConnectResult(false, ConnectFailureReason.BRIDGE_ERROR, null, config.messages().failed());
	}

	private boolean connectWithRetry(UUID playerId, String serviceName, SelectorConfig config) {
		if (!config.features().retryConnect()) {
			return this.cloudNetFacade.connectPlayerToService(playerId, serviceName);
		}
		for (int attempt = 0; attempt < 3; attempt++) {
			if (this.cloudNetFacade.connectPlayerToService(playerId, serviceName)) {
				return true;
			}
			if (attempt < 2) {
				try {
					Thread.sleep(100L);
				} catch (InterruptedException interruptedException) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
		}
		return false;
	}

	private net.kyori.adventure.text.Component colorize(String text) {
		return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(text);
	}
}
