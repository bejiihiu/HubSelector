package kz.bejiihiu.hub.cloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;

/**
 * Thin CloudNet integration boundary.
 * Provides null-safe accessors for injected CloudNet services.
 */
public class CloudNetFacade {
	/**
	 * Resolves CloudNet service provider from injection layer.
	 */
	public Optional<CloudServiceProvider> cloudServiceProvider() {
		return Optional.ofNullable(InjectionLayer.ext().instance(CloudServiceProvider.class));
	}

	/**
	 * Resolves wrapper configuration for current service metadata.
	 */
	public Optional<WrapperConfiguration> wrapperConfiguration() {
		return Optional.ofNullable(InjectionLayer.ext().instance(WrapperConfiguration.class));
	}

	/**
	 * Returns task services as a mutable snapshot list for safe local processing.
	 */
	public List<ServiceInfoSnapshot> servicesByTask(String taskName) {
		return this.cloudServiceProvider()
				.<List<ServiceInfoSnapshot>>map(provider -> new ArrayList<>(provider.servicesByTask(taskName)))
				.orElseGet(List::of);
	}

	/**
	 * Looks up one service by its runtime name.
	 */
	public Optional<ServiceInfoSnapshot> serviceByName(String serviceName) {
		return this.cloudServiceProvider().map(provider -> provider.serviceByName(serviceName));
	}

	/**
	 * Returns current wrapper service name when available.
	 */
	public Optional<String> currentServiceName() {
		return this.wrapperConfiguration().map(config -> config.serviceInfoSnapshot().name());
	}

	/**
	 * Connects a cloud player to a target service.
	 * Fails safely when bridge/player services are not available.
	 */
	public boolean connectPlayerToService(UUID playerId, String serviceName) {
		ServiceRegistry serviceRegistry = InjectionLayer.ext().instance(ServiceRegistry.class);
		if (serviceRegistry == null) {
			return false;
		}

		PlayerManager playerManager = serviceRegistry.defaultInstance(PlayerManager.class);
		if (playerManager == null) {
			return false;
		}

		CloudPlayer player = playerManager.onlinePlayer(playerId);
		if (player == null) {
			return false;
		}

		PlayerExecutor playerExecutor = playerManager.playerExecutor(player.uniqueId());
		if (playerExecutor == null) {
			return false;
		}

		playerExecutor.connect(serviceName);
		return true;
	}
}
