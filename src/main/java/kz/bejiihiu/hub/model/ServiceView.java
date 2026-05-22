package kz.bejiihiu.hub.model;

import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;

/**
 * Runtime service view model used by inventory rendering.
 */
public record ServiceView(
		ServiceInfoSnapshot service,
		boolean currentService,
		int players,
		int maxPlayers
) {
}
