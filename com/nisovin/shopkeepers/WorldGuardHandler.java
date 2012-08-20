package com.nisovin.shopkeepers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class WorldGuardHandler {

	static boolean canBuild(Player player, Location loc) {
		Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
		if (plugin != null) {
			return ((WorldGuardPlugin)plugin).canBuild(player, loc);
		} else {
			return true;
		}
	}
	
}
