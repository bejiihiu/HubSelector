package kz.bejiihiu.hub.cloud;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import kz.bejiihiu.hub.config.SelectorConfig;

/**
 * Classifies services against lobby task rules from configuration.
 */
public class ServiceFilter {
	private final SelectorConfig config;

	public ServiceFilter(SelectorConfig config) {
		this.config = config;
	}

	/**
	 * Accepts a service if it belongs to lobby task or, when allowed, silent lobby task.
	 */
	public boolean isValidLobbyService(ServiceInfoSnapshot service, boolean enableSilentHub) {
		if (service.serviceId().taskName().equals(this.config.lobbyTask())) {
			return true;
		}
		return enableSilentHub && this.isSilentHubService(service);
	}

	/**
	 * Checks whether a service belongs to configured silent lobby task.
	 */
	public boolean isSilentHubService(ServiceInfoSnapshot service) {
		return this.config.enableSilentLobby() && service.serviceId().taskName().equals(this.config.silentLobbyTask());
	}
}
