package kz.bejiihiu.hub.cloud;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import kz.bejiihiu.hub.config.SelectorConfig;

import java.util.function.Supplier;

/**
 * Classifies services against lobby task rules from configuration.
 */
public class ServiceFilter {
	private final Supplier<SelectorConfig> configSupplier;

	public ServiceFilter(Supplier<SelectorConfig> configSupplier) {
		this.configSupplier = configSupplier;
	}

	/**
	 * Accepts a service if it belongs to lobby task or, when allowed, silent lobby task.
	 */
	public boolean isValidLobbyService(ServiceInfoSnapshot service, boolean enableSilentHub) {
		SelectorConfig config = this.configSupplier.get();
		if (service.serviceId().taskName().equals(config.lobbyTask())) {
			return true;
		}
		return enableSilentHub && this.isSilentHubService(service);
	}

	/**
	 * Checks whether a service belongs to configured silent lobby task.
	 */
	public boolean isSilentHubService(ServiceInfoSnapshot service) {
		SelectorConfig config = this.configSupplier.get();
		return config.enableSilentLobby() && service.serviceId().taskName().equals(config.silentLobbyTask());
	}
}
