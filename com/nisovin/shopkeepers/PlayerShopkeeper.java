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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * A shopkeeper that is managed by a player. This shopkeeper draws its supplies from a chest that it
 * stands on, and will deposit earnings back into that chest.
 *
 */
public class PlayerShopkeeper extends Shopkeeper {

	protected String owner;
	protected int chestx;
	protected int chesty;
	protected int chestz;
	protected HashMap<ItemType, Integer> costs;
	private int unpaid = 0;
	
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
	
	/**
	 * Gets the name of the player who owns this shop.
	 * @return the player name
	 */
	public String getOwner() {
		return owner;
	}
	
	/**
	 * Checks whether this shop uses the indicated chest.
	 * @param chest the chest to check
	 * @return
	 */
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
				if (ShopkeepersPlugin.highCurrencyItem > 0 && cost > ShopkeepersPlugin.highCurrencyMinCost) {
					int highCost = cost / ShopkeepersPlugin.highCurrencyValue;
					int lowCost = cost % ShopkeepersPlugin.highCurrencyValue;
					if (highCost > 0) {
						recipe[0] = new ItemStack(ShopkeepersPlugin.highCurrencyItem, highCost, ShopkeepersPlugin.highCurrencyData);
					}
					if (lowCost > 0) {
						recipe[1] = new ItemStack(ShopkeepersPlugin.currencyItem, lowCost, ShopkeepersPlugin.currencyData);
					}
				} else {
					recipe[0] = new ItemStack(ShopkeepersPlugin.currencyItem, cost, ShopkeepersPlugin.currencyData);
				}
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
					// cost is already set, show it
					int cost = costs.get(type);
					if (ShopkeepersPlugin.highCurrencyItem > 0 && cost > ShopkeepersPlugin.highCurrencyMinCost) {
						int highCost = cost / ShopkeepersPlugin.highCurrencyValue;
						int lowCost = cost % ShopkeepersPlugin.highCurrencyValue;
						if (highCost > 0) {
							inv.setItem(i + 9, new ItemStack(ShopkeepersPlugin.highCurrencyItem, highCost, ShopkeepersPlugin.highCurrencyData));
						} else {
							inv.setItem(i + 9, new ItemStack(ShopkeepersPlugin.highZeroItem));
						}
						if (lowCost > 0) {
							inv.setItem(i + 18, new ItemStack(ShopkeepersPlugin.currencyItem, lowCost, ShopkeepersPlugin.currencyData));
						} else {
							inv.setItem(i + 18, new ItemStack(ShopkeepersPlugin.zeroItem));
						}
					} else {
						inv.setItem(i + 18, new ItemStack(ShopkeepersPlugin.currencyItem, cost, ShopkeepersPlugin.currencyData));
						if (ShopkeepersPlugin.highCurrencyItem > 0) {
							inv.setItem(i + 9, new ItemStack(ShopkeepersPlugin.highZeroItem));
						}
					}
				} else {
					// no cost is set, show zero items
					inv.setItem(i + 18, new ItemStack(ShopkeepersPlugin.zeroItem));
					if (ShopkeepersPlugin.highCurrencyItem > 0) {
						inv.setItem(i + 9, new ItemStack(ShopkeepersPlugin.highZeroItem));
					}
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
			saveEditor(event.getInventory());
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
			// change low cost
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
					if (amount > 64) amount = 64;
					if (amount <= 0) {
						item.setTypeId(ShopkeepersPlugin.zeroItem);
						item.setDurability((short)0);
						item.setAmount(1);
					} else {
						item.setAmount(amount);
					}
				} else if (item.getTypeId() == ShopkeepersPlugin.zeroItem) {
					item.setTypeId(ShopkeepersPlugin.currencyItem);
					item.setDurability(ShopkeepersPlugin.currencyData);
					item.setAmount(1);
				}
			}
		} else if (event.getRawSlot() >= 9 && event.getRawSlot() <= 16) {
			// change high cost
			ItemStack item = event.getCurrentItem();
			if (item != null && ShopkeepersPlugin.highCurrencyItem > 0) {
				if (item.getTypeId() == ShopkeepersPlugin.highCurrencyItem) {
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
						item.setTypeId(ShopkeepersPlugin.highZeroItem);
						item.setDurability((short)0);
						item.setAmount(1);
					} else {
						item.setAmount(amount);
					}
				} else if (item.getTypeId() == ShopkeepersPlugin.highZeroItem) {
					item.setTypeId(ShopkeepersPlugin.highCurrencyItem);
					item.setDurability(ShopkeepersPlugin.highCurrencyData);
					item.setAmount(1);
				}
			}
		}
		return EditorClickResult.NOTHING;
	}

	@Override
	public void onEditorClose(InventoryCloseEvent event) {
		saveEditor(event.getInventory());
	}
	
	private void saveEditor(Inventory inv) {
		for (int i = 0; i < 8; i++) {
			ItemStack item = inv.getItem(i);
			if (item != null && item.getType() != Material.AIR) {
				ItemStack lowCostItem = inv.getItem(i + 18);
				ItemStack highCostItem = inv.getItem(i + 9);
				int cost = 0;
				if (lowCostItem != null && lowCostItem.getTypeId() == ShopkeepersPlugin.currencyItem && lowCostItem.getAmount() > 0) {
					cost += lowCostItem.getAmount();
				}
				if (ShopkeepersPlugin.highCurrencyItem > 0 && highCostItem != null && highCostItem.getTypeId() == ShopkeepersPlugin.highCurrencyItem && highCostItem.getAmount() > 0) {
					cost += highCostItem.getAmount() * ShopkeepersPlugin.highCurrencyValue;
				}
				if (cost > 0) {
					costs.put(new ItemType(item), cost);
				} else {
					costs.remove(new ItemType(item));
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
		for (int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			if (item != null && item.getTypeId() == type.id && item.getDurability() == type.data && item.getAmount() == type.amount) {
				contents[i] = null;
				if (ShopkeepersPlugin.highCurrencyItem <= 0 || cost <= ShopkeepersPlugin.highCurrencyMinCost) {
					addToInventory(new ItemStack(ShopkeepersPlugin.currencyItem, cost, ShopkeepersPlugin.currencyData), contents);
				} else {
					int highCost = cost / ShopkeepersPlugin.highCurrencyValue;
					int lowCost = cost % ShopkeepersPlugin.highCurrencyValue;
					addToInventory(new ItemStack(ShopkeepersPlugin.highCurrencyItem, highCost, ShopkeepersPlugin.highCurrencyData), contents);
					addToInventory(new ItemStack(ShopkeepersPlugin.currencyItem, lowCost, ShopkeepersPlugin.currencyData), contents);
				}
				inv.setContents(contents);				
				return;
			}
		}

		// item not found
		event.setCancelled(true);
		event.getWhoClicked().closeInventory();
	}
	
	/*private boolean removeFromInventory(ItemStack item, ItemStack[] contents) {
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
	}*/
	
	private void addToInventory(ItemStack item, ItemStack[] contents) {
		if (unpaid > 0 && item.getTypeId() == ShopkeepersPlugin.currencyItem) {
			// add previously unpaid amount to this item
			int amt = item.getAmount() + unpaid;
			if (amt > item.getMaxStackSize()) {
				unpaid = amt - item.getMaxStackSize();
				item.setAmount(item.getMaxStackSize());
			} else {
				item.setAmount(amt);
				unpaid = 0;
			}
		}
		for (int i = 0; i < contents.length; i++) {
			if (contents[i] == null) {
				contents[i] = item;
				return;
			} else if (contents[i].getTypeId() == item.getTypeId() && contents[i].getDurability() == item.getDurability() && contents[i].getAmount() != contents[i].getMaxStackSize()) {
				int amt = contents[i].getAmount() + item.getAmount();
				if (amt <= contents[i].getMaxStackSize()) {
					contents[i].setAmount(amt);
					return;
				} else {
					item.setAmount(amt - contents[i].getMaxStackSize());
					contents[i].setAmount(contents[i].getMaxStackSize());
				}
			}
		}
		if (item.getAmount() > 0 && item.getTypeId() == ShopkeepersPlugin.currencyItem) {
			// save unpaid amount
			unpaid += item.getAmount();
		}
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
