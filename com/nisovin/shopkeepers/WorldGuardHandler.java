package com.nisovin.shopkeepers;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class WorldGuardHandler {

	static boolean canBuild(Player player, Block block) {
		Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
		if (plugin != null) {
			return ((WorldGuardPlugin)plugin).canBuild(player, block);
		} else {
			return true;
		}
	}
	
}
