package net.jandie1505.lobbyselector;

import kz.bejiihiu.hub.bootstrap.Main;
import org.bukkit.plugin.java.JavaPlugin;

public class LobbySelector extends JavaPlugin {
	private Main bootstrap;

	@Override
	public void onEnable() {
		this.bootstrap = new Main(this);
		this.bootstrap.enable();
	}

	@Override
	public void onDisable() {
		if (this.bootstrap != null) {
			this.bootstrap.disable();
		}
	}
}
