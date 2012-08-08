package com.nisovin.shopkeepers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
public abstract class PlayerShopkeeper extends Shopkeeper {

	protected String owner;
	protected int chestx;
	protected int chesty;
	protected int chestz;
	
	PlayerShopkeeper(ConfigurationSection config) {
		super(config);
	}
	
	public PlayerShopkeeper(Player owner, Block chest, Location location, int profession) {
		super(location, profession);
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
		return (chest.getWorld().getName().equals(world) && chest.getX() == chestx && chest.getY() == chesty && chest.getZ() == chestz);
	}


	@Override
	public boolean onEdit(Player player) {
		if ((player.getName().equalsIgnoreCase(owner) && player.hasPermission("shopkeeper.player")) || player.hasPermission("shopkeeper.bypass")) {
			return onPlayerEdit(player);
		} else {
			return false;
		}
	}
	
	protected abstract boolean onPlayerEdit(Player player);
	
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
	
	protected abstract void saveEditor(Inventory inv);	

	protected void setRecipeCost(ItemStack[] recipe, int cost) {
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
	}
	
	protected void setEditColumnCost(Inventory inv, int column, int cost) {
		if (cost > 0) {
			if (ShopkeepersPlugin.highCurrencyItem > 0 && cost > ShopkeepersPlugin.highCurrencyMinCost) {
				int highCost = cost / ShopkeepersPlugin.highCurrencyValue;
				int lowCost = cost % ShopkeepersPlugin.highCurrencyValue;
				if (highCost > 0) {
					inv.setItem(column + 9, new ItemStack(ShopkeepersPlugin.highCurrencyItem, highCost, ShopkeepersPlugin.highCurrencyData));
				} else {
					inv.setItem(column + 9, new ItemStack(ShopkeepersPlugin.highZeroItem));
				}
				if (lowCost > 0) {
					inv.setItem(column + 18, new ItemStack(ShopkeepersPlugin.currencyItem, lowCost, ShopkeepersPlugin.currencyData));
				} else {
					inv.setItem(column + 18, new ItemStack(ShopkeepersPlugin.zeroItem));
				}
			} else {
				inv.setItem(column + 18, new ItemStack(ShopkeepersPlugin.currencyItem, cost, ShopkeepersPlugin.currencyData));
				if (ShopkeepersPlugin.highCurrencyItem > 0) {
					inv.setItem(column + 9, new ItemStack(ShopkeepersPlugin.highZeroItem));
				}
			}
		} else {
			inv.setItem(column + 18, new ItemStack(ShopkeepersPlugin.zeroItem));
			if (ShopkeepersPlugin.highCurrencyItem > 0) {
				inv.setItem(column + 9, new ItemStack(ShopkeepersPlugin.highZeroItem));
			}
		}
	}
	
	protected void setActionButtons(Inventory inv) {
		inv.setItem(8, new ItemStack(ShopkeepersPlugin.saveItem));
		inv.setItem(17, new ItemStack(Material.WOOL, 1, getProfessionWoolColor()));
		inv.setItem(26, new ItemStack(ShopkeepersPlugin.deleteItem));
	}
	
	protected int getCostFromColumn(Inventory inv, int column) {
		ItemStack lowCostItem = inv.getItem(column + 18);
		ItemStack highCostItem = inv.getItem(column + 9);
		int cost = 0;
		if (lowCostItem != null && lowCostItem.getTypeId() == ShopkeepersPlugin.currencyItem && lowCostItem.getAmount() > 0) {
			cost += lowCostItem.getAmount();
		}
		if (ShopkeepersPlugin.highCurrencyItem > 0 && highCostItem != null && highCostItem.getTypeId() == ShopkeepersPlugin.highCurrencyItem && highCostItem.getAmount() > 0) {
			cost += highCostItem.getAmount() * ShopkeepersPlugin.highCurrencyValue;
		}
		return cost;
	}
	
}
