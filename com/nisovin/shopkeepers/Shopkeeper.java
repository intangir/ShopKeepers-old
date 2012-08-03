package com.nisovin.shopkeepers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityVillager;
import net.minecraft.server.MerchantRecipeList;
import net.minecraft.server.PathfinderGoalLookAtPlayer;
import net.minecraft.server.PathfinderGoalSelector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.entity.CraftVillager;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;

public class Shopkeeper {

	String world;
	int x;
	int y;
	int z;
	int profession;
	List<ItemStack[]> recipes;
	Villager villager;
	
	public Shopkeeper(Location location, int prof) {
		world = location.getWorld().getName();
		x = location.getBlockX();
		y = location.getBlockY();
		z = location.getBlockZ();
		profession = prof;
		recipes = new ArrayList<ItemStack[]>();
		spawn();
	}
	
	public Shopkeeper(ConfigurationSection config) {
		world = config.getString("world");
		x = config.getInt("x");
		y = config.getInt("y");
		z = config.getInt("z");
		profession = config.getInt("prof");
		recipes = new ArrayList<ItemStack[]>();
		List<String> recipeDatas = config.getStringList("recipes");
		if (recipeDatas != null) {
			for (String string : recipeDatas) {
				ItemStack[] recipe = new ItemStack[3];
				String[] items = string.split(";");
				for (int i = 0; i < 3; i++) {
					if (!items[i].isEmpty()) {
						String[] itemData = items[i].split(",");
						recipe[i] = new ItemStack(Integer.parseInt(itemData[0]), Integer.parseInt(itemData[2]), Short.parseShort(itemData[1]));
					}
				}
				recipes.add(recipe);
			}
		}
	}
	
	public void save(ConfigurationSection config) {
		config.set("world", world);
		config.set("x", x);
		config.set("y", y);
		config.set("z", z);
		config.set("prof", profession);
		List<String> recipeDatas = new ArrayList<String>();
		for (ItemStack[] recipe : recipes) {
			String string = recipe[0].getTypeId() + "," + recipe[0].getDurability() + "," + recipe[0].getAmount();
			if (recipe[1] != null) {
				string += ";" + recipe[1].getTypeId() + "," + recipe[1].getDurability() + "," + recipe[1].getAmount();
			} else {
				string += ";";
			}
			string += ";" + recipe[2].getTypeId() + "," + recipe[2].getDurability() + "," + recipe[2].getAmount();
			recipeDatas.add(string);
		}
		config.set("recipes", recipeDatas);
	}
	
	public void spawn() {
		World w = Bukkit.getWorld(world);
		villager = w.spawn(new Location(w, x + .5, y, z + .5), Villager.class);
		villager.setProfession(Profession.getProfession(profession));
		setRecipes();
		overwriteAI();
	}
	
	public void remove() {
		if (villager != null) {
			villager.remove();
		}
	}
	
	public String getChunk() {
		return world + "," + (x >> 4) + "," + (z >> 4);
	}
	
	public int getEntityId() {
		if (villager != null) {
			return villager.getEntityId();
		}
		return 0;
	}
	
	public List<ItemStack[]> getRecipes() {
		return recipes;
	}
	
	public void setRecipes(List<ItemStack[]> recipes) {
		this.recipes = recipes;
		setRecipes();
	}
	
	public boolean isActive() {
		return villager != null;
	}
	
	private void setRecipes() {
		try {
			EntityVillager ev = ((CraftVillager)villager).getHandle();
			
			Field recipeListField = EntityVillager.class.getDeclaredField("i");
			recipeListField.setAccessible(true);
			MerchantRecipeList recipeList = (MerchantRecipeList)recipeListField.get(ev);
			if (recipeList == null) {
				recipeList = new MerchantRecipeList();
				recipeListField.set(ev, recipeList);
			}
			recipeList.clear();
			for (ItemStack[] recipe : recipes) {
				recipeList.a(ShopRecipe.factory(recipe[0], recipe[1], recipe[2]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private void overwriteAI() {
		try {
			EntityVillager ev = ((CraftVillager)villager).getHandle();
			
			Field goalsField = EntityLiving.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
			
			Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
			listField.setAccessible(true);
			@SuppressWarnings("rawtypes")
			List list = (List)listField.get(goals);
			list.clear();
			
			goals.a(1, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 8.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
}
