package com.nisovin.shopkeepers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityVillager;
import net.minecraft.server.MerchantRecipeList;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;
import net.minecraft.server.NBTTagString;
import net.minecraft.server.PathfinderGoalLookAtPlayer;
import net.minecraft.server.PathfinderGoalLookAtTradingPlayer;
import net.minecraft.server.PathfinderGoalSelector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.entity.CraftVillager;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

public class Shopkeeper {

	String world;
	int x;
	int y;
	int z;
	int profession;
	List<ItemStack[]> recipes;
	Villager villager;
	
	/**
	 * Creates a new shopkeeper and spawns it in the world. This should be used when a player is
	 * creating a new shopkeeper.
	 * @param location the location to spawn at
	 * @param prof the id of the profession
	 */
	public Shopkeeper(Location location, int prof) {
		world = location.getWorld().getName();
		x = location.getBlockX();
		y = location.getBlockY();
		z = location.getBlockZ();
		profession = prof;
		recipes = new ArrayList<ItemStack[]>();
		spawn();
	}
	
	/**
	 * Loads a shopkeeper from the provided configuration section. This is used when loading a 
	 * shopkeeper from the save file.
	 * @param config the config section the shopkeeper is stored in
	 */
	public Shopkeeper(ConfigurationSection config) {
		world = config.getString("world");
		x = config.getInt("x");
		y = config.getInt("y");
		z = config.getInt("z");
		profession = config.getInt("prof");
		recipes = new ArrayList<ItemStack[]>();
		ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
		for (String key : recipesSection.getKeys(false)) {
			ConfigurationSection recipeSection = recipesSection.getConfigurationSection(key);
			ItemStack[] recipe = new ItemStack[3];
			for (int i = 0; i < 3; i++) {
				if (recipeSection.contains(i + "")) {
					recipe[i] = loadItemStack(recipeSection.getConfigurationSection(i + ""));
				}
			}
			recipes.add(recipe);
		}
	}
	
	/**
	 * Saves the shopkeeper's data to the specified configuration section.
	 * @param config the config section
	 */
	public void save(ConfigurationSection config) {
		config.set("world", world);
		config.set("x", x);
		config.set("y", y);
		config.set("z", z);
		config.set("prof", profession);
		ConfigurationSection recipesSection = config.createSection("recipes");
		int count = 0;
		for (ItemStack[] recipe : recipes) {
			ConfigurationSection recipeSection = recipesSection.createSection(count + "");
			for (int i = 0; i < 3; i++) {
				if (recipe[i] != null) {
					saveItemStack(recipe[i], recipeSection.createSection(i + ""));
				}
			}
			count++;
		}
	}
	
	/**
	 * Spawns the shopkeeper into the world at its spawn location. Also sets the
	 * trade recipes and overwrites the villager AI.
	 */
	public void spawn() {
		World w = Bukkit.getWorld(world);
		villager = w.spawn(new Location(w, x + .5, y, z + .5), Villager.class);
		((CraftVillager)villager).getHandle().setProfession(profession);
		updateRecipes();
		overwriteAI();
	}
	
	/**
	 * Teleports this shopkeeper to its spawn location.
	 */
	public void teleport() {
		if (villager != null) {
			World w = Bukkit.getWorld(world);
			villager.teleport(new Location(w, x + .5, y, z + .5, villager.getLocation().getYaw(), villager.getLocation().getPitch()));
		}
	}
	
	/**
	 * Removes this shopkeeper from the world.
	 */
	public void remove() {
		if (villager != null) {
			villager.remove();
		}
	}
	
	/**
	 * Gets a string identifying the chunk this shopkeeper spawns in, 
	 * in the format world,x,z.
	 * @return the chunk as a string
	 */
	public String getChunk() {
		return world + "," + (x >> 4) + "," + (z >> 4);
	}
	
	/**
	 * Gets the shopkeeper's entity ID.
	 * @return the entity id, or 0 if the shopkeeper is not in the world
	 */
	public int getEntityId() {
		if (villager != null) {
			return villager.getEntityId();
		}
		return 0;
	}
	
	/**
	 * Gets the shopkeeper's trade recipes. This will be a list of ItemStack[3],
	 * where the first two elemets of the ItemStack[] array are the cost, and the third
	 * element is the trade result (the item sold by the shopkeeper).
	 * @return the trade recipes of this shopkeeper
	 */
	public List<ItemStack[]> getRecipes() {
		return recipes;
	}
	
	/**
	 * Sets the shopkeeper's trade recipes. This should be set to a list of ItemStack[3], 
	 * where the first two elemets of the ItemStack[] array are the cost, and the third
	 * element is the trade result (the item sold by the shopkeeper).
	 * @param recipes the trade recipes the shopkeeper should have
	 */
	public void setRecipes(List<ItemStack[]> recipes) {
		this.recipes = recipes;
		updateRecipes();
	}
	
	/**
	 * Checks if the shopkeeper is active (is alive in the world).
	 * @return whether the shopkeeper is active
	 */
	public boolean isActive() {
		return villager != null;
	}
	
	/**
	 * Sets the villager's trade options.
	 */
	@SuppressWarnings("unchecked")
	public void updateRecipes() {
		try {
			EntityVillager ev = ((CraftVillager)villager).getHandle();
			
			Field recipeListField = EntityVillager.class.getDeclaredField(ShopkeepersPlugin.recipeListVar);
			recipeListField.setAccessible(true);
			MerchantRecipeList recipeList = (MerchantRecipeList)recipeListField.get(ev);
			if (recipeList == null) {
				recipeList = new MerchantRecipeList();
				recipeListField.set(ev, recipeList);
			}
			recipeList.clear();
			for (ItemStack[] recipe : recipes) {
				recipeList.add(ShopRecipe.factory(recipe[0], recipe[1], recipe[2]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Overwrites the AI of the villager to not wander, but to just look at customers.
	 */
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
			
			goals.a(1, new PathfinderGoalLookAtTradingPlayer(ev));
			goals.a(2, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Loads an ItemStack from a config section.
	 * @param config
	 * @return
	 */
	private ItemStack loadItemStack(ConfigurationSection config) {
		ItemStack item = new ItemStack(config.getInt("id"), config.getInt("amt"), (short)config.getInt("data"));
		if (config.contains("enchants")) {
			List<String> list = config.getStringList("enchants");
			for (String s : list) {
				String[] enchantData = s.split(" ");
				item.addUnsafeEnchantment(Enchantment.getById(Integer.parseInt(enchantData[0])), Integer.parseInt(enchantData[1]));
			}
		}
		if (item.getType() == Material.WRITTEN_BOOK && config.contains("title") && config.contains("author") && config.contains("pages")) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("title", config.getString("title"));
			tag.setString("author", config.getString("author"));
			List<String> pages = config.getStringList("pages");
			NBTTagList tagPages = new NBTTagList();
			for (String page : pages) {
				NBTTagString tagPage = new NBTTagString(null, page);
				tagPages.add(tagPage);
			}
			tag.set("pages", tagPages);
			item = new CraftItemStack(item);
			((CraftItemStack)item).getHandle().tag = tag;
		}
		return item;
	}
	
	/**
	 * Saves an ItemStack to a config section.
	 * @param item
	 * @param config
	 */
	private void saveItemStack(ItemStack item, ConfigurationSection config) {
		config.set("id", item.getTypeId());
		config.set("data", item.getDurability());
		config.set("amt", item.getAmount());
		Map<Enchantment, Integer> enchants = item.getEnchantments();
		if (enchants.size() > 0) {
			List<String> list = new ArrayList<String>();
			for (Enchantment enchant : enchants.keySet()) {
				list.add(enchant.getId() + " " + enchants.get(enchant));
			}
			config.set("enchants", list);
		}
		if (item.getType() == Material.WRITTEN_BOOK && item instanceof CraftItemStack) {
			NBTTagCompound tag = ((CraftItemStack)item).getHandle().tag;
			if (tag != null && tag.hasKey("title") && tag.hasKey("author") && tag.hasKey("pages")) {
				config.set("title", tag.getString("title"));
				config.set("author", tag.getString("author"));
				List<String> pages = new ArrayList<String>();
				NBTTagList tagPages = (NBTTagList)tag.get("pages");
				for (int i = 0; i < tagPages.size(); i++) {
					NBTTagString tagPage = (NBTTagString)tagPages.get(i);
					if (tagPage.data != null) {
						pages.add(tagPage.data);
					}
				}
				config.set("pages", pages);
			}
		}
	}
	
}
