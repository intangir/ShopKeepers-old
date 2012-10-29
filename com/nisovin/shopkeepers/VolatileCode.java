package com.nisovin.shopkeepers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityVillager;
import net.minecraft.server.MerchantRecipe;
import net.minecraft.server.MerchantRecipeList;
import net.minecraft.server.NBTBase;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;
import net.minecraft.server.NBTTagString;
import net.minecraft.server.PathfinderGoalFloat;
import net.minecraft.server.PathfinderGoalLookAtPlayer;
import net.minecraft.server.PathfinderGoalLookAtTradingPlayer;
import net.minecraft.server.PathfinderGoalSelector;
import net.minecraft.server.PathfinderGoalTradeWithPlayer;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftVillager;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

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
			
			villager.c(((CraftPlayer)player).getHandle());
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static void overwriteLivingEntityAI(LivingEntity entity) {
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
	
	public static void overwriteVillagerAI(LivingEntity villager) {
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
	
	public static ItemStack loadItemStack(ConfigurationSection config) {
		CraftItemStack item = new CraftItemStack(config.getInt("id"), config.getInt("amt"), (short)config.getInt("data"));
		if (config.contains("nbtdata")) {
			try {
				Object nbtData = config.get("nbtdata");
				ByteArrayInputStream stream = new ByteArrayInputStream((byte[]) nbtData);
				NBTBase tag = NBTBase.b(new DataInputStream(stream));
				if (tag instanceof NBTTagCompound) {
					item.getHandle().tag = (NBTTagCompound)tag;
				}
			} catch (Exception e) {
				ShopkeepersPlugin.debug("Error loading item NBT data");
			}
		}
		// rest of code left for backwards compatibility and for just-in-case
		if (config.contains("name") || config.contains("lore")) {
			NBTTagCompound tag = ((CraftItemStack)item).getHandle().tag;
			if (tag == null) {
				tag = new NBTTagCompound();
				item.getHandle().tag = tag;
			}
			NBTTagCompound display = tag.getCompound("display");
			if (display == null) {
				display = new NBTTagCompound();
				tag.setCompound("display", display);
			}
			if (config.contains("name")) {
				display.setString("Name", config.getString("name"));
			}
			if (config.contains("lore")) {
				List<String> lore = config.getStringList("lore");
				NBTTagList list = new NBTTagList();
				for (String l : lore) {
					list.add(new NBTTagString(l));
				}
				display.set("Lore", list);
			}
		}
		if (config.contains("enchants")) {
			List<String> list = config.getStringList("enchants");
			for (String s : list) {
				String[] enchantData = s.split(" ");
				item.addUnsafeEnchantment(Enchantment.getById(Integer.parseInt(enchantData[0])), Integer.parseInt(enchantData[1]));
			}
		}
		if (item.getType() == Material.WRITTEN_BOOK && config.contains("title") && config.contains("author") && config.contains("pages")) {
			NBTTagCompound tag = ((CraftItemStack)item).getHandle().tag;
			if (tag == null) {
				tag = new NBTTagCompound();
				item.getHandle().tag = tag;
			}
			tag.setString("title", config.getString("title"));
			tag.setString("author", config.getString("author"));
			List<String> pages = config.getStringList("pages");
			NBTTagList tagPages = new NBTTagList();
			for (String page : pages) {
				NBTTagString tagPage = new NBTTagString(null, page);
				tagPages.add(tagPage);
			}
			tag.set("pages", tagPages);
		}
		if (config.contains("extra")) {
			NBTTagCompound tag = ((CraftItemStack)item).getHandle().tag;
			if (tag == null) {
				tag = new NBTTagCompound();
				item.getHandle().tag = tag;
			}
			ConfigurationSection extraDataSection = config.getConfigurationSection("extra");
			for (String key : extraDataSection.getKeys(false)) {
				tag.setString(key, extraDataSection.getString(key));
			}
		}
		return item;
	}
	
	public static void saveItemStack(ItemStack item, ConfigurationSection config) {
		config.set("id", item.getTypeId());
		config.set("data", item.getDurability());
		config.set("amt", item.getAmount());
		if (item instanceof CraftItemStack) {
			NBTTagCompound tag = ((CraftItemStack)item).getHandle().tag;
			if (tag != null) {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				NBTBase.a(tag, new DataOutputStream(stream));
				config.set("nbtdata", stream.toByteArray());
				// rest of code left for backwards compatibility and for just-in-case 
				if (tag.hasKey("display")) {
					NBTTagCompound display = tag.getCompound("display");
					if (display.hasKey("Name")) {
						config.set("name", display.getString("Name"));
					}
					if (display.hasKey("Lore")) {
						NBTTagList list = display.getList("Lore");
						String[] lore = new String[list.size()];
						for (int i = 0; i < list.size(); i++) {
							lore[i] = ((NBTTagString)list.get(i)).data;
						}
						config.set("lore", lore);
					}
				}
				Map<Enchantment, Integer> enchants = item.getEnchantments();
				if (enchants.size() > 0) {
					List<String> list = new ArrayList<String>();
					for (Enchantment enchant : enchants.keySet()) {
						list.add(enchant.getId() + " " + enchants.get(enchant));
					}
					config.set("enchants", list);
				}
				if (item.getType() == Material.WRITTEN_BOOK && tag.hasKey("title") && tag.hasKey("author") && tag.hasKey("pages")) {
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
				Map<String, String> extraData = new HashMap<String, String>();
				for (Object o : tag.c()) {
					if (o instanceof NBTTagString) {
						NBTTagString s = (NBTTagString)o;
						String name = s.getName();
						if (!name.equals("title") && !name.equals("author")) {
							extraData.put(name, s.data);
						}
					}
				}
				if (extraData.size() > 0) {
					ConfigurationSection extraDataSection = config.createSection("extra");
					for (String key : extraData.keySet()) {
						extraDataSection.set(key, extraData.get(key));
					}
				}
			}
		}
	}
	
	public static String getTitleOfBook(ItemStack book) {
		if (book instanceof CraftItemStack) {
			NBTTagCompound tag = ((CraftItemStack)book).getHandle().tag;
			if (tag != null && tag.hasKey("title")) {
				return tag.getString("title");
			}
		}
		return null;
	}
	
	public static boolean isBookAuthoredByShopOwner(ItemStack book, String owner) {
		if (book instanceof CraftItemStack) {
			NBTTagCompound tag = ((CraftItemStack)book).getHandle().tag;
			if (tag != null && tag.hasKey("author")) {
				return tag.getString("author").equalsIgnoreCase(owner);
			}
		}
		return false;
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
	
	private static net.minecraft.server.ItemStack convertItemStack(org.bukkit.inventory.ItemStack item) {
		if (item == null) return null;
		return org.bukkit.craftbukkit.inventory.CraftItemStack.createNMSItemStack(item);
	}

	
}
