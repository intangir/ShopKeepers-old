package com.nisovin.shopkeepers;

import org.bukkit.block.Block;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class TownyHandler {

	static boolean isCommercialArea(Block block) {
		TownBlock townBlock = TownyUniverse.getTownBlock(block.getLocation());
		return townBlock.getType() == TownBlockType.COMMERCIAL;
	}
	
}
