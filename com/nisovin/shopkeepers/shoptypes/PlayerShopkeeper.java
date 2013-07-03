package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.EditorClickResult;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shopobjects.ShopObject;

/**
 * A shopkeeper that is managed by a player. This shopkeeper draws its supplies from a chest that it
 * stands on, and will deposit earnings back into that chest.
 *
 */
public abstract class PlayerShopkeeper extends Shopkeeper {

	protected String owner;
	protected int chestx;
	protected int chesty;
	protected int chestz;
	
	public PlayerShopkeeper(ConfigurationSection config) {
		super(config);
	}
	
	public PlayerShopkeeper(Player owner, Block chest, Location location, ShopObject shopObject) {
		super(location, shopObject);
		this.owner = owner.getName().toLowerCase();
		this.chestx = chest.getX();
		this.chesty = chest.getY();
		this.chestz = chest.getZ();
	}
	
	@Override
	public void load(ConfigurationSection config) {
		super.load(config);
		owner = config.getString("owner");
		chestx = config.getInt("chestx");
		chesty = config.getInt("chesty");
		chestz = config.getInt("chestz");
	}
	
	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		config.set("type", "player");
		config.set("owner", owner);
		config.set("chestx", chestx);
		config.set("chesty", chesty);
		config.set("chestz", chestz);
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
		if (!chest.getWorld().getName().equals(world)) 
			return false;
		int x = chest.getX();
		int y = chest.getY();
		int z = chest.getZ();
		if (x == chestx && y == chesty && z == chestz) 
			return true;
		if (x == chestx + 1 && y == chesty && z == chestz) 
			return true;
		if (x == chestx - 1 && y == chesty && z == chestz) 
			return true;
		if (x == chestx && y == chesty && z == chestz + 1) 
			return true;
		if (x == chestx && y == chesty && z == chestz - 1) 
			return true;
		return false;
	}


	@Override
	public boolean onEdit(Player player) {
		if ((player.getName().equalsIgnoreCase(owner) && player.hasPermission("shopkeeper." + getType().getPermission())) || player.hasPermission("shopkeeper.bypass")) {
			return onPlayerEdit(player);
		} else {
			return false;
		}
	}

	/**
	 * Called when a player shift-right-clicks on the player shopkeeper villager in an attempt to edit
	 * the shopkeeper information. This method should open the editing interface. The permission and owner
	 * check has already occurred before this is called.
	 * @param player the player doing the edit
	 * @return whether the player is now editing (returns false if permission fails)
	 */
	protected abstract boolean onPlayerEdit(Player player);
	
	@Override
	public EditorClickResult onEditorClick(InventoryClickEvent event) {
		// prevent shift clicks on player inventory items
		if (event.getRawSlot() > 27 && event.isShiftClick()) {
			event.setCancelled(true);
			return EditorClickResult.NOTHING;
		}
		if (event.getRawSlot() >= 18 && event.getRawSlot() <= 25) {
			// change low cost
			event.setCancelled(true);
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
					} else if (event.isShiftClick() && event.getClick() == ClickType.MIDDLE) {
						amount = 64;
					} else if (event.getClick() == ClickType.MIDDLE) {
						amount = 1;
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
					item.setDurability(Settings.currencyItemData);
					item.setAmount(1);
				}
			}
			return EditorClickResult.NOTHING;
		} else if (event.getRawSlot() >= 9 && event.getRawSlot() <= 16) {
			// change high cost
			event.setCancelled(true);
			ItemStack item = event.getCurrentItem();
			if (item != null && Settings.highCurrencyItem > 0) {
				if (item.getTypeId() == Settings.highCurrencyItem) {
					int amount = item.getAmount();
					if (event.isShiftClick() && event.isLeftClick()) {
						amount += 10;
					} else if (event.isShiftClick() && event.isRightClick()) {
						amount -= 10;
					} else if (event.isLeftClick()) {
						amount += 1;
					} else if (event.isRightClick()) {
						amount -= 1;
					} else if (event.isShiftClick() && event.getClick() == ClickType.MIDDLE) {
						amount = 64;
					} else if (event.getClick() == ClickType.MIDDLE) {
						amount = 1;
					}
					if (amount > 64) amount = 64;
					if (amount <= 0) {
						item.setTypeId(Settings.highZeroItem);
						item.setDurability((short)0);
						item.setAmount(1);
					} else {
						item.setAmount(amount);
					}
				} else if (item.getTypeId() == Settings.highZeroItem) {
					item.setTypeId(Settings.highCurrencyItem);
					item.setDurability(Settings.highCurrencyItemData);
					item.setAmount(1);
				}
			}
			return EditorClickResult.NOTHING;
		} else {
			return super.onEditorClick(event);
		}
	}

	@Override
	public void onEditorClose(InventoryCloseEvent event) {
		saveEditor(event.getInventory(), null);
	}
	
	@Override
	public final void onPurchaseClick(InventoryClickEvent event) {
		if (Settings.preventTradingWithOwnShop && event.getWhoClicked().getName().equalsIgnoreCase(owner) && !event.getWhoClicked().isOp()) {
			event.setCancelled(true);
			ShopkeepersPlugin.debug("Cancelled trade from " + event.getWhoClicked().getName() + " because he can't trade with his own shop");
		} else {
			// prevent unwanted special clicks
			if (!event.isLeftClick() || event.isShiftClick()) {
				event.setCancelled(true);
				return;
			}
			onPlayerPurchaseClick(event);
		}
	}
	
	protected abstract void onPlayerPurchaseClick(InventoryClickEvent event);

	protected void setRecipeCost(ItemStack[] recipe, int cost) {
		if (Settings.highCurrencyItem > 0 && cost > Settings.highCurrencyMinCost) {
			int highCost = cost / Settings.highCurrencyValue;
			int lowCost = cost % Settings.highCurrencyValue;
			if (highCost > 0) {
				recipe[0] = new ItemStack(Settings.highCurrencyItem, highCost, Settings.highCurrencyItemData);
				if (highCost > recipe[0].getMaxStackSize()) {
					lowCost += (highCost - recipe[0].getMaxStackSize()) * Settings.highCurrencyValue;
					recipe[0].setAmount(recipe[0].getMaxStackSize());
				}
			}
			if (lowCost > 0) {
				recipe[1] = new ItemStack(Settings.currencyItem, lowCost, Settings.currencyItemData);
				if (lowCost > recipe[1].getMaxStackSize()) {
					ShopkeepersPlugin.warning("Shopkeeper at " + world + "," + x + "," + y + "," + z + " owned by " + owner + " has an invalid cost!");
				}
			}
		} else {
			recipe[0] = new ItemStack(Settings.currencyItem, cost, Settings.currencyItemData);
		}
	}
	
	protected void setEditColumnCost(Inventory inv, int column, int cost) {
		if (cost > 0) {
			if (Settings.highCurrencyItem > 0 && cost > Settings.highCurrencyMinCost) {
				int highCost = cost / Settings.highCurrencyValue;
				int lowCost = cost % Settings.highCurrencyValue;
				if (highCost > 0) {
					ItemStack item = new ItemStack(Settings.highCurrencyItem, highCost, Settings.highCurrencyItemData);
					if (highCost > item.getMaxStackSize()) {
						lowCost += (highCost - item.getMaxStackSize()) * Settings.highCurrencyValue;
						item.setAmount(item.getMaxStackSize());
					}
					inv.setItem(column + 9, item);
				} else {
					inv.setItem(column + 9, new ItemStack(Settings.highZeroItem));
				}
				if (lowCost > 0) {
					inv.setItem(column + 18, new ItemStack(Settings.currencyItem, lowCost, Settings.currencyItemData));
				} else {
					inv.setItem(column + 18, new ItemStack(Settings.zeroItem));
				}
			} else {
				inv.setItem(column + 18, new ItemStack(Settings.currencyItem, cost, Settings.currencyItemData));
				if (Settings.highCurrencyItem > 0) {
					inv.setItem(column + 9, new ItemStack(Settings.highZeroItem));
				}
			}
		} else {
			inv.setItem(column + 18, new ItemStack(Settings.zeroItem));
			if (Settings.highCurrencyItem > 0) {
				inv.setItem(column + 9, new ItemStack(Settings.highZeroItem));
			}
		}
	}
	
	protected int getCostFromColumn(Inventory inv, int column) {
		ItemStack lowCostItem = inv.getItem(column + 18);
		ItemStack highCostItem = inv.getItem(column + 9);
		int cost = 0;
		if (lowCostItem != null && lowCostItem.getTypeId() == Settings.currencyItem && lowCostItem.getAmount() > 0) {
			cost += lowCostItem.getAmount();
		}
		if (Settings.highCurrencyItem > 0 && highCostItem != null && highCostItem.getTypeId() == Settings.highCurrencyItem && highCostItem.getAmount() > 0) {
			cost += highCostItem.getAmount() * Settings.highCurrencyValue;
		}
		return cost;
	}
	
	protected boolean removeFromInventory(ItemStack item, ItemStack[] contents) {
		item = item.clone();
		for (int i = 0; i < contents.length; i++) {
			if (contents[i] != null && item.isSimilar(contents[i])) {
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
	
	protected boolean addToInventory(ItemStack item, ItemStack[] contents) {
		item = item.clone();
		for (int i = 0; i < contents.length; i++) {
			if (contents[i] == null) {
				contents[i] = item;
				return true;
			} else if (item.isSimilar(contents[i]) && contents[i].getAmount() != contents[i].getMaxStackSize()) {
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
	
}
