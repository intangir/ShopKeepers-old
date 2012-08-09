package com.nisovin.shopkeepers.shoptypes;

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

import com.nisovin.shopkeepers.EditorClickResult;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopkeeperType;


public class BuyingPlayerShopkeeper extends PlayerShopkeeper {

	private Map<SaleType, Cost> costs;
	
	public BuyingPlayerShopkeeper(ConfigurationSection config) {
		super(config);
	}

	public BuyingPlayerShopkeeper(Player owner, Block chest, Location location, int profession) {
		super(owner, chest, location, profession);
		costs = new HashMap<SaleType, Cost>();
	}

	@Override
	public void load(ConfigurationSection config) {
		super.load(config);
		costs = new HashMap<SaleType, Cost>();
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
		config.set("type", "buy");
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
	public ShopkeeperType getType() {
		return ShopkeeperType.PLAYER_BUY;
	}
	
	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		List<SaleType> chestItems = getTypesFromChest();
		int chestTotal = getCurrencyInChest();
		for (SaleType type : costs.keySet()) {
			if (chestItems.contains(type)) {
				Cost cost = costs.get(type);
				if (chestTotal >= cost.cost) {
					ItemStack[] recipe = new ItemStack[3];
					recipe[0] = new ItemStack(type.id, cost.amount, type.data);
					recipe[2] = new ItemStack(Settings.currencyItem, cost.cost, Settings.currencyData);
					recipes.add(recipe);
				}
			}
		}
		return recipes;
	}

	@Override
	protected boolean onPlayerEdit(Player player) {
		Inventory inv = Bukkit.createInventory(player, 27, Settings.editorTitle);
		
		List<SaleType> types = getTypesFromChest();
		for (int i = 0; i < types.size() && i < 8; i++) {
			SaleType type = types.get(i);
			Cost cost = costs.get(type);
			
			if (cost != null) {
				if (cost.cost == 0) {
					inv.setItem(i, new ItemStack(Settings.zeroItem));
				} else {
					inv.setItem(i, new ItemStack(Settings.currencyItem, cost.cost, Settings.currencyData));
				}
				int amt = cost.amount;
				if (amt <= 0) amt = 1;
				inv.setItem(i + 18, new ItemStack(type.id, amt, type.data));
			} else {
				inv.setItem(i, new ItemStack(Settings.zeroItem));
				inv.setItem(i + 18, new ItemStack(type.id, 1, type.data));
			}
		}
		
		setActionButtons(inv);
		
		player.openInventory(inv);
		
		return true;
	}

	@Override
	public EditorClickResult onEditorClick(InventoryClickEvent event) {
		if (event.getRawSlot() >= 0 && event.getRawSlot() <= 7) {
			// modifying cost
			ItemStack item = event.getCurrentItem();
			if (item != null) {
				if (item.getTypeId() == Settings.currencyItem) {
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
					if (amount > 64) amount = 64;
					if (amount <= 0) {
						item.setTypeId(Settings.zeroItem);
						item.setDurability((short)0);
						item.setAmount(1);
					} else {
						item.setAmount(amount);
					}
				} else if (item.getTypeId() == Settings.zeroItem) {
					item.setTypeId(Settings.currencyItem);
					item.setDurability(Settings.currencyData);
					item.setAmount(1);
				}
			}
			
		} else if (event.getRawSlot() >= 18 && event.getRawSlot() <= 25) {
			// modifying quantity
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
			
		} else if (event.getRawSlot() >= 9 && event.getRawSlot() <= 16) {
		} else {
			return super.onEditorClick(event);
		}
		event.setCancelled(true);
		return EditorClickResult.NOTHING;
	}
	
	@Override
	protected void saveEditor(Inventory inv) {
		for (int i = 0; i < 8; i++) {
			ItemStack item = inv.getItem(i + 18);
			if (item != null) {
				ItemStack costItem = inv.getItem(i);
				if (costItem != null && costItem.getTypeId() == Settings.currencyItem && costItem.getAmount() > 0) {
					costs.put(new SaleType(item), new Cost(item.getAmount(), costItem.getAmount()));
				} else {
					costs.remove(new SaleType(item));
				}
			}
		}
	}

	@Override
	public void onPurchaseClick(InventoryClickEvent event) {
		// prevent shift clicks
		if (event.isShiftClick() || event.isRightClick()) {
			event.setCancelled(true);
			return;
		}
		
		// get type and cost
		ItemStack item = event.getInventory().getItem(0);
		SaleType type = new SaleType(item);
		if (!costs.containsKey(type)) {
			event.setCancelled(true);
			return;
		}
		Cost cost = costs.get(type);
		if (cost.amount > item.getAmount()) {
			event.setCancelled(true);
			return;
		}
		
		// get chest
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() != Material.CHEST) {
			event.setCancelled(true);
			return;
		}
		
		// remove currency from chest
		Inventory inv = ((Chest)chest.getState()).getInventory();
		ItemStack[] contents = inv.getContents();
		boolean removed = removeFromInventory(new ItemStack(Settings.currencyItem, cost.cost, Settings.currencyData), contents);
		if (!removed) {
			event.setCancelled(true);
			return;
		}
		
		// add items to chest
		System.out.println(item);
		boolean added = addToInventory(new ItemStack(type.id, cost.amount, type.data), contents);
		if (!added) {
			event.setCancelled(true);
			return;
		}

		// save chest contents
		inv.setContents(contents);
	}
	
	private List<SaleType> getTypesFromChest() {
		List<SaleType> list = new ArrayList<SaleType>();
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() == Material.CHEST) {
			Inventory inv = ((Chest)chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (ItemStack item : contents) {
				if (item != null && item.getType() != Material.AIR && item.getTypeId() != Settings.currencyItem && item.getTypeId() != Settings.highCurrencyItem && item.getType() != Material.WRITTEN_BOOK && item.getEnchantments().size() == 0) {
					SaleType si = new SaleType(item);
					if (!list.contains(si)) {
						list.add(si);
					}
				}
			}
		}
		return list;
	}
	
	private int getCurrencyInChest() {
		int total = 0;
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() == Material.CHEST) {
			Inventory inv = ((Chest)chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (ItemStack item : contents) {
				if (item != null && item.getTypeId() == Settings.currencyItem && item.getDurability() == Settings.currencyData) {
					total += item.getAmount();
				}
			}
		}
		return total;
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
