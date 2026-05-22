package kz.bejiihiu.hub.inventory;

public enum LobbyCategory {
	ALL,
	FREE,
	NEAR_FULL,
	SILENT;

	public LobbyCategory next() {
		return switch (this) {
			case ALL -> FREE;
			case FREE -> NEAR_FULL;
			case NEAR_FULL -> SILENT;
			case SILENT -> ALL;
		};
	}
}
