package kz.bejiihiu.hub.inventory;

/**
 * Applies runtime placeholders to configured text templates.
 */
public class PlaceholderService {
	/**
	 * Replaces service placeholders used by item name and lore templates.
	 */
	public String apply(String input, String serviceName, int players, int maxPlayers) {
		String result = input.replace("{service}", serviceName);
		result = result.replace("{players}", String.valueOf(players));
		result = result.replace("{max_players}", String.valueOf(maxPlayers));
		result = result.replace("{ping}", "n/a");
		result = result.replace("{region}", "global");
		result = result.replace("{eta}", "0s");
		result = result.replace("{queue_pos}", "-");
		return result;
	}
}
