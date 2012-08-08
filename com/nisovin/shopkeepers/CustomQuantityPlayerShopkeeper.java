package com.nisovin.shopkeepers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class CustomQuantityPlayerShopkeeper extends PlayerShopkeeper {

	private Map<SaleType, Cost> costs;
	
	CustomQuantityPlayerShopkeeper(ConfigurationSection config) {
		super(config);
	}

	public CustomQuantityPlayerShopkeeper(Player owner, Block chest, Location location, int profession) {
		super(owner, chest, location, profession);
		this.costs = new HashMap<CustomQuantityPlayerShopkeeper.SaleType, CustomQuantityPlayerShopkeeper.Cost>();
	}
	
	@Override
	public void load(ConfigurationSection config) {
		super.load(config);		
		costs = new HashMap<CustomQuantityPlayerShopkeeper.SaleType, CustomQuantityPlayerShopkeeper.Cost>();
		ConfigurationSection costsSection = config.getConfigurationSection("costs");
		if (costsSection != null) {
			for (String key : costsSection.getKeys(false)) {
				ConfigurationSection itemSection = costsSection.getConfigurationSection(key);
				SaleType item = new SaleType();
				Cost cost = new Cost();
				item.id = itemSection.getInt("id");
				item.data = (short)itemSection.getInt("data");
				cost.amount = itemSection.getInt("amount");
				cost.cost = itemSection.getInt("cost");
				costs.put(item, cost);
			}
		}
	}
	
	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		ConfigurationSection costsSection = config.createSection("costs");
		int count = 0;
		for (SaleType item : costs.keySet()) {
			Cost cost = costs.get(item);
			ConfigurationSection itemSection = costsSection.createSection(count + "");
			itemSection.set("id", item.id);
			itemSection.set("data", item.data);
			itemSection.set("amount", cost.amount);
			itemSection.set("cost", cost.cost);
			count++;
		}
	}
	
	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		Map<SaleType, Integer> chestItems = getItemsFromChest();
		for (SaleType type : costs.keySet()) {
			if (chestItems.containsKey(type)) {
				Cost cost = costs.get(type);
				int chestAmt = chestItems.get(type);
				if (chestAmt >= cost.amount) {
					ItemStack[] recipe = new ItemStack[3];
					setRecipeCost(recipe, cost.cost);
					recipe[2] = new ItemStack(type.id, cost.amount, type.data);
					recipes.add(recipe);
				}
			}
		}
		return recipes;
	}
	
	@Override
	public boolean onPlayerEdit(Player player) {
		Inventory inv = Bukkit.createInventory(player, 27, ShopkeepersPlugin.editorTitle);
		
		// add the sale types
		Map<SaleType, Integer> typesFromChest = getItemsFromChest();
		int i = 0;
		for (SaleType type : typesFromChest.keySet()) {
			Cost cost = costs.get(type);
			if (cost != null) {
				inv.setItem(i, new ItemStack(type.id, cost.amount, type.data));
				setEditColumnCost(inv, i, cost.cost);
			} else {
				inv.setItem(i, new ItemStack(type.id, 1, type.data));
				setEditColumnCost(inv, i, 0);
			}
			i++;
			if (i > 8) break;
		}
		
		// add the special buttons
		setActionButtons(inv);
		
		player.openInventory(inv);
		
		return true;
	}

	@Override
	public EditorClickResult onEditorClick(InventoryClickEvent event) {
		if (event.getRawSlot() >= 0 && event.getRawSlot() <= 7) {
			event.setCancelled(true);
			// handle changing sell stack size
			ItemStack item = event.getCurrentItem();
			if (item != null && item.getTypeId() != 0) {
				int amt = item.getAmount();
				if (event.isLeftClick()) {
					if (event.isShiftClick()) {
						amt += 10;
					} else {
						amt += 1;
					}
				} else if (event.isRightClick()) {
					if (event.isShiftClick()) {
						amt -= 10;
					} else {
						amt -= 1;
					}
				}
				if (amt <= 0) amt = 1;
				if (amt > item.getMaxStackSize()) amt = item.getMaxStackSize();
				item.setAmount(amt);
			}
			return EditorClickResult.NOTHING;
		} else {
			return super.onEditorClick(event);
		}
	}
	
	@Override
	protected void saveEditor(Inventory inv) {
		for (int i = 0; i < 8; i++) {
			ItemStack item = inv.getItem(i);
			if (item != null && item.getType() != Material.AIR) {
				int cost = getCostFromColumn(inv, i);
				if (cost > 0) {
					costs.put(new SaleType(item), new Cost(item.getAmount(), cost));
				} else {
					costs.remove(new SaleType(item));
				}
			}
		}
	}
	
	@Override
	public void onPurchaseClick(final InventoryClickEvent event) {		
		// prevent shift clicks
		if (event.isShiftClick() || event.isRightClick()) {
			event.setCancelled(true);
			return;
		}
		
		// get type and cost
		ItemStack item = event.getCurrentItem();
		SaleType type = new SaleType(item);
		if (!costs.containsKey(type)) {
			event.setCancelled(true);
			return;
		}
		Cost cost = costs.get(type);
		if (cost.amount != item.getAmount()) {
			event.setCancelled(true);
			return;
		}
		
		// get chest
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() != Material.CHEST) {
			event.setCancelled(true);
			return;
		}
		
		// remove item from chest
		Inventory inv = ((Chest)chest.getState()).getInventory();
		ItemStack[] contents = inv.getContents();
		boolean removed = removeFromInventory(item, contents);
		if (!removed) {
			event.setCancelled(true);
			return;
		}
		
		// add earnings to chest
		if (ShopkeepersPlugin.highCurrencyItem <= 0 || cost.cost <= ShopkeepersPlugin.highCurrencyMinCost) {
			boolean added = addToInventory(new ItemStack(ShopkeepersPlugin.currencyItem, cost.cost, ShopkeepersPlugin.currencyData), contents);
			if (!added) {
				event.setCancelled(true);
				return;
			}
		} else {
			int highCost = cost.cost / ShopkeepersPlugin.highCurrencyValue;
			int lowCost = cost.cost % ShopkeepersPlugin.highCurrencyValue;
			boolean added = false;
			if (highCost > 0) {
				added = addToInventory(new ItemStack(ShopkeepersPlugin.highCurrencyItem, highCost, ShopkeepersPlugin.highCurrencyData), contents);
				if (!added) {
					event.setCancelled(true);
					return;
				}
			}
			if (lowCost > 0) {
				added = addToInventory(new ItemStack(ShopkeepersPlugin.currencyItem, lowCost, ShopkeepersPlugin.currencyData), contents);
				if (!added) {
					event.setCancelled(true);
					return;
				}
			}
		}

		// save chest contents
		inv.setContents(contents);
	}
	
	private boolean removeFromInventory(ItemStack item, ItemStack[] contents) {
		item = item.clone();
		for (int i = 0; i < contents.length; i++) {
			if (contents[i] != null && contents[i].getTypeId() == item.getTypeId() && contents[i].getDurability() == contents[i].getDurability()) {
				if (contents[i].getAmount() > item.getAmount()) {
					contents[i].setAmount(contents[i].getAmount() - item.getAmount());
					return true;
				} else if (contents[i].getAmount() == item.getAmount()) {
					contents[i] = null;
					return true;
				} else {
					item.setAmount(item.getAmount() - contents[i].getAmount());
					contents[i] = null;
				}
			}
		}
		return false;
	}
	
	private boolean addToInventory(ItemStack item, ItemStack[] contents) {
		for (int i = 0; i < contents.length; i++) {
			if (contents[i] == null) {
				contents[i] = item;
				return true;
			} else if (contents[i].getTypeId() == item.getTypeId() && contents[i].getDurability() == item.getDurability() && contents[i].getAmount() != contents[i].getMaxStackSize()) {
				int amt = contents[i].getAmount() + item.getAmount();
				if (amt <= contents[i].getMaxStackSize()) {
					contents[i].setAmount(amt);
					return true;
				} else {
					item.setAmount(amt - contents[i].getMaxStackSize());
					contents[i].setAmount(contents[i].getMaxStackSize());
				}
			}
		}
		return false;
	}
	
	private Map<SaleType, Integer> getItemsFromChest() {
		Map<SaleType, Integer> map = new HashMap<SaleType, Integer>();
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() == Material.CHEST) {
			Inventory inv = ((Chest)chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (ItemStack item : contents) {
				if (item != null && item.getType() != Material.AIR && item.getTypeId() != ShopkeepersPlugin.currencyItem && item.getTypeId() != ShopkeepersPlugin.highCurrencyItem && item.getType() != Material.WRITTEN_BOOK && item.getEnchantments().size() == 0) {
					SaleType si = new SaleType(item);
					if (map.containsKey(si)) {
						map.put(si, map.get(si) + item.getAmount());
					} else {
						map.put(si, item.getAmount());
					}
				}
			}
		}
		return map;
	}
	
	private class SaleType {
		int id;
		short data;
		
		public SaleType() {
			
		}
		
		public SaleType(ItemStack item) {
			id = item.getTypeId();
			data = item.getDurability();
		}
		
		@Override
		public int hashCode() {
			return (id + " " + data).hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof SaleType)) return false;
			SaleType i = (SaleType)o;
			return i.id == this.id && i.data == this.data;
		}
	}
	
	private class Cost {
		int amount;
		int cost;
		
		public Cost() {
			
		}
		
		public Cost(int amount, int cost) {
			this.amount = amount;
			this.cost = cost;
		}
	}
}
