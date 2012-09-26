package com.nisovin.shopkeepers.shopobjects;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.PathfinderGoalFloat;
import net.minecraft.server.PathfinderGoalLookAtPlayer;
import net.minecraft.server.PathfinderGoalSelector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public abstract class LivingEntityShop extends ShopObject {
	
	protected LivingEntity entity;
	private String uuid;
	
	@Override
	public void load(ConfigurationSection config) {
		if (config.contains("uuid")) {
			uuid = config.getString("uuid");
		}
	}

	@Override
	public void save(ConfigurationSection config) {
		if (entity != null) {
			config.set("uuid", entity.getUniqueId().toString());
		}
	}
	
	protected abstract EntityType getEntityType();
	
	@Override
	public boolean needsSpawned() {
		return true;
	}
	
	@Override
	public boolean spawn(String world, int x, int y, int z) {
		// prepare location
		World w = Bukkit.getWorld(world);
		Location loc = new Location(w, x + .5, y + .5, z + .5);
		// find old villager
		if (uuid != null && !uuid.isEmpty()) {
			Entity[] entities = loc.getChunk().getEntities();
			for (Entity e : entities) {
				if (e.getType() == getEntityType() && e.getUniqueId().toString().equalsIgnoreCase(uuid) && !e.isDead()) {
					entity = (LivingEntity)e;
					entity.teleport(loc);
					break;
				}
			}
		}
		// spawn villager
		if (entity == null) {
			entity = (LivingEntity)w.spawnEntity(loc, getEntityType());
		}
		if (entity != null && !entity.isDead()) {
			overwriteAI();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isActive() {
		return entity != null && !entity.isDead();
	}

	@Override
	public String getId() {
		if (entity != null) {
			return "entity" + entity.getEntityId();
		}
		return null;
	}
	
	@Override
	public void check(String world, int x, int y, int z) {
		if (entity == null || entity.isDead()) {
			spawn(world, x, y, z);
		} else {
			World w = Bukkit.getWorld(world);
			Location loc = new Location(w, x + .5, y, z + .5, entity.getLocation().getYaw(), entity.getLocation().getPitch());
			if (entity.getLocation().distanceSquared(loc) > .25) {
				entity.teleport(loc);
			}
		}
	}

	@Override
	public void despawn() {
		if (entity != null) {
			entity.remove();
			entity.setHealth(0);
			entity = null;
		}
	}
	
	@Override
	public void delete() {
		despawn();
	}
	
	protected void overwriteAI() {
		try {
			EntityLiving ev = ((CraftLivingEntity)entity).getHandle();
			
			Field goalsField = EntityLiving.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
			
			Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
			listField.setAccessible(true);
			@SuppressWarnings("rawtypes")
			List list = (List)listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
			goals.a(1, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
}
