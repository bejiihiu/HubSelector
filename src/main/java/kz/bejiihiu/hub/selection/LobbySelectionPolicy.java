package kz.bejiihiu.hub.selection;

import kz.bejiihiu.hub.model.ServiceView;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class LobbySelectionPolicy {
	public Optional<ServiceView> selectBest(List<ServiceView> services, String strategy) {
		if (services.isEmpty()) {
			return Optional.empty();
		}
		Comparator<ServiceView> comparator = switch (strategy) {
			case "lowestPing" -> Comparator.comparing(ServiceView::serviceName);
			case "balanced" -> Comparator.comparingDouble(this::fillRatio).thenComparing(ServiceView::serviceName);
			default -> Comparator.comparingInt(ServiceView::players).thenComparing(ServiceView::serviceName);
		};
		return services.stream().filter(this::joinable).min(comparator);
	}

	private boolean joinable(ServiceView view) {
		return !view.currentService() && view.players() < view.maxPlayers();
	}

	private double fillRatio(ServiceView view) {
		if (view.maxPlayers() <= 0) {
			return 1.0;
		}
		double ratio = (double) view.players() / (double) view.maxPlayers();
		return Math.abs(ratio - 0.55);
	}
}
