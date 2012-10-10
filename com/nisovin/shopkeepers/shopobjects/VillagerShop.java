package com.nisovin.shopkeepers.shopobjects;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityVillager;
import net.minecraft.server.PathfinderGoalFloat;
import net.minecraft.server.PathfinderGoalLookAtPlayer;
import net.minecraft.server.PathfinderGoalLookAtTradingPlayer;
import net.minecraft.server.PathfinderGoalSelector;
import net.minecraft.server.PathfinderGoalTradeWithPlayer;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.entity.CraftVillager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

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
		if (spawned && entity != null && !entity.isDead()) {
			villager = (Villager)entity;
			((CraftVillager)villager).getHandle().setProfession(profession);
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
		((CraftVillager)villager).getHandle().setProfession(profession);
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
		try {
			EntityVillager ev = ((CraftVillager)entity).getHandle();
			
			Field goalsField = EntityLiving.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
			
			Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
			listField.setAccessible(true);
			@SuppressWarnings("rawtypes")
			List list = (List)listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
			goals.a(1, new PathfinderGoalTradeWithPlayer(ev));
			goals.a(1, new PathfinderGoalLookAtTradingPlayer(ev));
			goals.a(2, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

}
