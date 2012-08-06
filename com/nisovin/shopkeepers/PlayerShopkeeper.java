package com.nisovin.shopkeepers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PlayerShopkeeper extends Shopkeeper {

	private String owner;
	private int chestx;
	private int chesty;
	private int chestz;
	private HashMap<ItemType, Integer> costs;
	
	PlayerShopkeeper(ConfigurationSection config) {
		super(config);
	}
	
	public PlayerShopkeeper(Player owner, Block chest, Location location, int profession) {
		super(location, profession);
		this.owner = owner.getName().toLowerCase();
		this.chestx = chest.getX();
		this.chesty = chest.getY();
		this.chestz = chest.getZ();
		this.costs = new HashMap<PlayerShopkeeper.ItemType, Integer>();
	}
	
	@Override
	public void load(ConfigurationSection config) {
		super.load(config);
		owner = config.getString("owner");
		chestx = config.getInt("chestx");
		chesty = config.getInt("chesty");
		chestz = config.getInt("chestz");
		costs = new HashMap<PlayerShopkeeper.ItemType, Integer>();
		ConfigurationSection costsSection = config.getConfigurationSection("costs");
		for (String key : costsSection.getKeys(false)) {
			ConfigurationSection itemSection = costsSection.getConfigurationSection(key);
			ItemType type = new ItemType();
			type.id = itemSection.getInt("id");
			type.data = (short)itemSection.getInt("data");
			type.amount = itemSection.getInt("amount");
			int cost = itemSection.getInt("cost");
			costs.put(type, cost);
		}
	}
	
	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		config.set("owner", owner);
		config.set("chestx", chestx);
		config.set("chesty", chesty);
		config.set("chestz", chestz);
		ConfigurationSection costsSection = config.createSection("costs");
		int count = 0;
		for (ItemType type : costs.keySet()) {
			ConfigurationSection itemSection = costsSection.createSection(count + "");
			itemSection.set("id", type.id);
			itemSection.set("data", type.data);
			itemSection.set("amount", type.amount);
			itemSection.set("cost", costs.get(type));
			count++;
		}
	}
	
	public String getOwner() {
		return owner;
	}
	
	public boolean usesChest(Block chest) {
		return (chest.getWorld().getName().equals(world) && chest.getX() == chestx && chest.getY() == chesty && chest.getZ() == chestz);
	}
	
	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		List<ItemType> types = getTypesFromChest();
		for (ItemType type : types) {
			if (costs.containsKey(type)) {
				ItemStack[] recipe = new ItemStack[3];
				int cost = costs.get(type);
				recipe[0] = new ItemStack(ShopkeepersPlugin.currencyItem, cost, ShopkeepersPlugin.currencyData);
				recipe[2] = type.getItemStack();
				recipes.add(recipe);
			}
		}
		return recipes;
	}

	@Override
	public boolean onEdit(Player player) {
		if ((player.getName().equalsIgnoreCase(owner) && player.hasPermission("shopkeeper.player")) || player.hasPermission("shopkeeper.bypass")) {
			Inventory inv = Bukkit.createInventory(player, 27, ShopkeepersPlugin.editorTitle);
			// show types
			List<ItemType> types = getTypesFromChest();
			for (int i = 0; i < types.size() && i < 8; i++) {
				ItemType type = types.get(i);
				inv.setItem(i, type.getItemStack());
				if (costs.containsKey(type)) {
					inv.setItem(i + 18, new ItemStack(ShopkeepersPlugin.currencyItem, costs.get(type), ShopkeepersPlugin.currencyData));
				} else {
					inv.setItem(i + 18, new ItemStack(ShopkeepersPlugin.zeroItem));
				}
			}
			// add the special buttons
			inv.setItem(8, new ItemStack(ShopkeepersPlugin.saveItem));
			inv.setItem(17, new ItemStack(Material.WOOL, 1, getProfessionWoolColor()));
			inv.setItem(26, new ItemStack(ShopkeepersPlugin.deleteItem));
			// show editing inventory
			player.openInventory(inv);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public EditorClickResult onEditorClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getRawSlot() == 8) {
			// save
			for (int i = 0; i < 8; i++) {
				ItemStack item = event.getInventory().getItem(i);
				if (item != null && item.getType() != Material.AIR) {
					ItemStack costItem = event.getInventory().getItem(i + 18);
					if (costItem.getTypeId() == ShopkeepersPlugin.currencyItem && costItem.getAmount() > 0) {
						costs.put(new ItemType(item), costItem.getAmount());
					}
				}
			}
			return EditorClickResult.DONE_EDITING;
		} else if (event.getRawSlot() == 17) {
			// change profession
			profession += 1;
			if (profession > 5) profession = 0;
			setProfession();
			event.getInventory().setItem(17, new ItemStack(Material.WOOL, 1, getProfessionWoolColor()));
			return EditorClickResult.SAVE_AND_CONTINUE;
		} else if (event.getRawSlot() == 26) {
			// delete
			remove();
			return EditorClickResult.DELETE_SHOPKEEPER;
		} else if (event.getRawSlot() >= 18 && event.getRawSlot() <= 25) {
			// change cost
			ItemStack item = event.getCurrentItem();
			if (item != null) {
				if (item.getTypeId() == ShopkeepersPlugin.currencyItem) {
					int amount = item.getAmount();
					if (event.isShiftClick() && event.isLeftClick()) {
						amount += 10;
					} else if (event.isShiftClick() && event.isRightClick()) {
						amount -= 10;
					} else if (event.isLeftClick()) {
						amount += 1;
					} else if (event.isRightClick()) {
						amount -= 1;
					}
					if (amount < 1) amount = 1;
					if (amount > 64) amount = 64;
					item.setAmount(amount);
					event.setCurrentItem(item);
				} else if (item.getTypeId() == ShopkeepersPlugin.zeroItem) {
					item.setTypeId(ShopkeepersPlugin.currencyItem);
					item.setDurability(ShopkeepersPlugin.currencyData);
					item.setAmount(1);
					event.setCurrentItem(item);
				}
			}
		}
		return EditorClickResult.NOTHING;
	}
	
	@Override
	public void onPurchaseClick(final InventoryClickEvent event) {		
		// prevent shift clicks
		if (event.isShiftClick() || event.isRightClick()) {
			event.setCancelled(true);
			return;
		}
		
		// get type and cost
		ItemType type = new ItemType(event.getCurrentItem());
		if (!costs.containsKey(type)) {
			event.setCancelled(true);
			return;
		}
		int cost = costs.get(type);
		
		// get chest
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() != Material.CHEST) {
			event.setCancelled(true);
			return;
		}
		
		// find item in chest
		Inventory inv = ((Chest)chest.getState()).getInventory();
		ItemStack[] contents = inv.getContents();
		for (ItemStack item : contents) {
			if (item != null && item.getTypeId() == type.id && item.getDurability() == type.data && item.getAmount() == type.amount) {
				item.setTypeId(ShopkeepersPlugin.currencyItem);
				item.setDurability(ShopkeepersPlugin.currencyData);
				item.setAmount(cost);
				inv.setContents(contents);				
				return;
			}
		}

		// item not found
		event.setCancelled(true);
		event.getWhoClicked().closeInventory();
	}
	
	private List<ItemType> getTypesFromChest() {
		List<ItemType> types = new ArrayList<ItemType>();
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() == Material.CHEST) {
			Inventory inv = ((Chest)chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (ItemStack item : contents) {
				if (item != null && item.getType() != Material.AIR && item.getTypeId() != ShopkeepersPlugin.currencyItem && item.getType() != Material.WRITTEN_BOOK && item.getEnchantments().size() == 0) {
					ItemType type = new ItemType(item);
					if (!types.contains(type)) {
						types.add(type);
					}
				}
			}
		}
		return types;
	}
	
	private class ItemType {
		int id;
		short data;
		int amount;
		
		ItemType() {
			
		}
		
		ItemType(ItemStack item) {
			id = item.getTypeId();
			data = item.getDurability();
			amount = item.getAmount();
		}
		
		ItemStack getItemStack() {
			return new ItemStack(id, amount, data);
		}
		
		@Override
		public int hashCode() {
			return (id + " " + data + " " + amount).hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof ItemType) {
				ItemType i = (ItemType)o;
				return i.id == this.id && i.data == this.data && i.amount == this.amount;
			}
			return false;
		}
	}

}
