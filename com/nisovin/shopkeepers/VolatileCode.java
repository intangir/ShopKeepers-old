package com.nisovin.shopkeepers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.server.v1_4_5.*;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_4_5.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_4_5.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_4_5.entity.CraftVillager;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class VolatileCode {

	@SuppressWarnings("unchecked")
	public static boolean openTradeWindow(Shopkeeper shopkeeper, Player player) {

		try {
			EntityVillager villager = new EntityVillager(((CraftPlayer)player).getHandle().world, 0);
			
			Field recipeListField = EntityVillager.class.getDeclaredField(Settings.recipeListVar);
			recipeListField.setAccessible(true);
			MerchantRecipeList recipeList = (MerchantRecipeList)recipeListField.get(villager);
			if (recipeList == null) {
				recipeList = new MerchantRecipeList();
				recipeListField.set(villager, recipeList);
			}
			recipeList.clear();
			for (ItemStack[] recipe : shopkeeper.getRecipes()) {
				recipeList.add(createMerchantRecipe(recipe[0], recipe[1], recipe[2]));
			}
			
			villager.a(((CraftPlayer)player).getHandle());
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			EntityLiving ev = ((CraftLivingEntity)entity).getHandle();
			
			Field goalsField = EntityLiving.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
			
			Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
			listField.setAccessible(true);
			List list = (List)listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			list = (List)listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
			goals.a(1, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	@SuppressWarnings("rawtypes")
	public static void overwriteVillagerAI(LivingEntity villager) {
		try {
			EntityVillager ev = ((CraftVillager)villager).getHandle();
			
			Field goalsField = EntityLiving.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
			
			Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
			listField.setAccessible(true);
			List list = (List)listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			list = (List)listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
			goals.a(1, new PathfinderGoalTradeWithPlayer(ev));
			goals.a(1, new PathfinderGoalLookAtTradingPlayer(ev));
			goals.a(2, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public static void setVillagerProfession(Villager villager, int profession) {
		((CraftVillager)villager).getHandle().setProfession(profession);
	}

	// NO LONGER VOLATILE
	public static ItemStack loadItemStack(ConfigurationSection config) {
		ItemStack item = new ItemStack(config.getInt("id"), config.getInt("amt"), (short)config.getInt("data"));
		if (config.contains("name") || config.contains("lore") || config.contains("color")) {
			ItemMeta meta = item.getItemMeta();
			if (config.contains("name")) {
				meta.setDisplayName(config.getString("name"));
			}
			if (config.contains("lore")) {
				List<String> lore = config.getStringList("lore");
				meta.setLore(lore);
			}
			if (config.contains("color") && meta instanceof LeatherArmorMeta) {
				((LeatherArmorMeta)meta).setColor(Color.fromRGB(config.getInt("color")));
			}
			item.setItemMeta(meta);
		}
		if (config.contains("enchants")) {
			List<String> list = config.getStringList("enchants");
			for (String s : list) {
				String[] enchantData = s.split(" ");
				item.addUnsafeEnchantment(Enchantment.getById(Integer.parseInt(enchantData[0])), Integer.parseInt(enchantData[1]));
			}
		}
		if (item.getType() == Material.WRITTEN_BOOK && config.contains("title") && config.contains("author") && config.contains("pages")) {
			BookMeta meta = (BookMeta)item.getItemMeta();
			meta.setTitle(config.getString("title"));
			meta.setAuthor(config.getString("author"));
			meta.setPages(config.getStringList("pages"));
			item.setItemMeta(meta);
		}
		return item;
	}

	// NO LONGER VOLATILE
	public static void saveItemStack(ItemStack item, ConfigurationSection config) {
		config.set("id", item.getTypeId());
		config.set("data", item.getDurability());
		config.set("amt", item.getAmount());
		
		ItemMeta meta = item.getItemMeta();
		// basic meta
		if (meta.hasDisplayName()) {
			config.set("name", meta.getDisplayName());
		}
		if (meta.hasLore()) {
			config.set("lore", meta.getLore());
		}
		if (meta instanceof LeatherArmorMeta) {
			config.set("color", ((LeatherArmorMeta)meta).getColor().asRGB());
		}
		// book meta
		if (meta instanceof BookMeta) {
			BookMeta book = (BookMeta)meta;
			if (book.hasTitle()) {
				config.set("title", book.getTitle());
			}
			if (book.hasAuthor()) {
				config.set("author", book.getAuthor());
			}
			if (book.hasPages()) {
				config.set("pages", book.getPages());
			}
		}
		// enchants
		Map<Enchantment, Integer> enchants = item.getEnchantments();
		if (enchants.size() > 0) {
			List<String> list = new ArrayList<String>();
			for (Enchantment enchant : enchants.keySet()) {
				list.add(enchant.getId() + " " + enchants.get(enchant));
			}
			config.set("enchants", list);
		}
	}
	
	// NO LONGER VOLATILE
	public static String getTitleOfBook(ItemStack book) {
		if (book.getType() == Material.WRITTEN_BOOK && book.hasItemMeta()) {
			BookMeta meta = (BookMeta)book.getItemMeta();
			return meta.getTitle();
		}
		return null;
	}

	// NO LONGER VOLATILE
	public static boolean isBookAuthoredByShopOwner(ItemStack book, String owner) {
		if (book.getType() == Material.WRITTEN_BOOK && book.hasItemMeta()) {
			BookMeta meta = (BookMeta)book.getItemMeta();
			if (meta.hasAuthor() && meta.getAuthor().equalsIgnoreCase(owner)) {
				return true;
			}
		}
		return false;
	}

	// NO LONGER VOLATILE
	public static String getNameOfItem(ItemStack item) {
		if (item.hasItemMeta()) {
			ItemMeta meta = item.getItemMeta();
			if (meta.hasDisplayName()) {
				return meta.getDisplayName();
			}
		}
		return "";
	}

	// NO LONGER VOLATILE
	public static boolean itemNamesEqual(ItemStack item1, ItemStack item2) {
		String name1 = getNameOfItem(item1);
		String name2 = getNameOfItem(item2);
		return (name1.equals(name2));
	}
	
	private static MerchantRecipe createMerchantRecipe(ItemStack item1, ItemStack item2, ItemStack item3) {
		MerchantRecipe recipe = new MerchantRecipe(convertItemStack(item1), convertItemStack(item2), convertItemStack(item3));
		try {
			Field maxUsesField = MerchantRecipe.class.getDeclaredField("maxUses");
			maxUsesField.setAccessible(true);
			maxUsesField.set(recipe, 10000);
		} catch (Exception e) {}
		return recipe;
	}
	
	private static net.minecraft.server.v1_4_5.ItemStack convertItemStack(org.bukkit.inventory.ItemStack item) {
		if (item == null) return null;
		return org.bukkit.craftbukkit.v1_4_5.inventory.CraftItemStack.asNMSCopy(item);
	}

	
}
