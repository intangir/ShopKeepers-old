package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.VolatileCode;

public class VillagerShop extends LivingEntityShop {

	private Villager villager;
	private int profession;
	
	@Override
	public void load(ConfigurationSection config) {
		super.load(config);
		profession = config.getInt("prof");
	}

	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		config.set("prof", profession);
		config.set("object", "villager");
	}

	@Override
	protected EntityType getEntityType() {
		return EntityType.VILLAGER;
	}
	
	@Override
	public boolean spawn(String world, int x, int y, int z) {
		boolean spawned = super.spawn(world, x, y, z);
		if (spawned && entity != null && entity.isValid()) {
			villager = (Villager)entity;
			VolatileCode.setVillagerProfession(villager, profession);
			villager.setBreed(false);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ItemStack getTypeItem() {
		return new ItemStack(Material.WOOL, 1, getProfessionWoolColor());
	}

	@Override
	public void cycleType() {
		profession += 1;
		if (profession > 5) profession = 0;
		VolatileCode.setVillagerProfession(villager, profession);
	}	

	private short getProfessionWoolColor() {
		switch (profession) {
		case 0: return 12;
		case 1: return 0;
		case 2: return 2;
		case 3: return 7;
		case 4: return 8;
		case 5: return 5;
		default: return 14;
		}
	}
	
	@Override
	protected void overwriteAI() {
		VolatileCode.overwriteVillagerAI(entity);
	}

}
