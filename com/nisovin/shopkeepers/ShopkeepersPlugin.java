package com.nisovin.shopkeepers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.shopkeepers.events.CreatePlayerShopkeeperEvent;
import com.nisovin.shopkeepers.events.OpenTradeEvent;
import com.nisovin.shopkeepers.shoptypes.AdminShopkeeper;
import com.nisovin.shopkeepers.shoptypes.BuyingPlayerShopkeeper;
import com.nisovin.shopkeepers.shoptypes.CustomQuantityPlayerShopkeeper;
import com.nisovin.shopkeepers.shoptypes.FixedQuantityPlayerShopkeeper;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;
import com.nisovin.shopkeepers.shoptypes.WrittenBookPlayerShopkeeper;


public class ShopkeepersPlugin extends JavaPlugin implements Listener {

	static ShopkeepersPlugin plugin;

	private boolean debug = false;
	
	private Map<String, List<Shopkeeper>> allShopkeepersByChunk = new HashMap<String, List<Shopkeeper>>();
	private Map<Integer, Shopkeeper> activeShopkeepers = new HashMap<Integer, Shopkeeper>();
	private Map<String, Integer> editing = new HashMap<String, Integer>();
	private Map<String, Integer> purchasing = new HashMap<String, Integer>();
	private Map<String, Integer> selectedShopType = new HashMap<String, Integer>();
	private Map<String, Block> selectedChest = new HashMap<String, Block>();
	
	private boolean dirty = false;
	
	private boolean disableOtherVillagers = true;
	private boolean saveInstantly = true;
	
	private boolean createPlayerShopWithCommand = true;
	private boolean createPlayerShopWithEgg = true;
	private boolean deletingPlayerShopReturnsEgg = false;
	private boolean allowCustomQuantities = true;
	private boolean allowPlayerBookShop = true;
	private boolean protectChests = true;
	private int maxShopsPerPlayer = 0;
	private int maxChestDistance = 15;
			
	private String msgSelectedNormalShop = "&aNormal shopkeeper selected (sells items to players).";
	private String msgSelectedBookShop = "&aBook shopkeeper selected (sell books).";
	private String msgSelectedBuyShop = "&aBuying shopkeeper selected (buys items from players).";
	private String msgSelectedChest = "&aChest selected! Right click a block to place your shopkeeper.";
	private String msgMustSelectChest = "&aYou must right-click a chest before placing your shopkeeper.";
	private String msgChestTooFar = "&aThe shopkeeper's chest is too far away!";
	
	private String msgPlayerShopCreated = "&aShopkeeper created!\n&aAdd items you want to sell to your chest, then\n&aright-click the villager while sneaking to modify costs.";
	private String msgBookShopCreated = "&aShopkeeper created!\n&aAdd written books and blank books to your chest, then\n&aright-click the villager while sneaking to modify costs.";
	private String msgBuyShopCreated = "&aShopkeeper created!\n&aAdd one of each item you want to sell to your chest, then\n&aright-click the villager while sneaking to modify costs.";
	private String msgAdminShopCreated = "&aShopkeeper created!\n&aRight-click the villager while sneaking to modify trades.";
	private String msgShopCreateFail = "&aYou cannot create a shopkeeper there.";
	private String msgTooManyShops = "&aYou have too many shops.";
	private String msgShopInUse = "&aSomeone else is already purchasing from this shopkeeper.";
	
	BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
	
	@Override
	public void onEnable() {
		plugin = this;
		
		// get config
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			saveDefaultConfig();
		}
		reloadConfig();
		Configuration config = getConfig();

		debug = config.getBoolean("debug", debug);
		
		disableOtherVillagers = config.getBoolean("disable-other-villagers", disableOtherVillagers);
		saveInstantly = config.getBoolean("save-instantly", saveInstantly);
		
		createPlayerShopWithCommand = config.getBoolean("create-player-shop-with-command", createPlayerShopWithCommand);
		createPlayerShopWithEgg = config.getBoolean("create-player-shop-with-egg", createPlayerShopWithEgg);
		deletingPlayerShopReturnsEgg = config.getBoolean("deleting-player-shop-returns-egg", deletingPlayerShopReturnsEgg);
		allowCustomQuantities = config.getBoolean("allow-custom-quantities", allowCustomQuantities);
		allowPlayerBookShop = config.getBoolean("allow-player-book-shop", allowPlayerBookShop);
		protectChests = config.getBoolean("protect-chests", protectChests);
		maxShopsPerPlayer = config.getInt("max-shops-per-player", maxShopsPerPlayer);
		maxChestDistance = config.getInt("max-chest-distance", maxChestDistance);
		
		Settings.editorTitle = config.getString("editor-title", Settings.editorTitle);
		Settings.saveItem = config.getInt("save-item", Settings.saveItem);
		Settings.deleteItem = config.getInt("delete-item", Settings.deleteItem);
		
		Settings.currencyItem = config.getInt("currency-item", Settings.currencyItem);
		Settings.currencyData = (short)config.getInt("currency-item-data", Settings.currencyData);
		Settings.zeroItem = config.getInt("zero-item", Settings.zeroItem);
		
		Settings.highCurrencyItem = config.getInt("high-currency-item", Settings.highCurrencyItem);
		Settings.highCurrencyData = (short)config.getInt("high-currency-item-data", Settings.highCurrencyData);
		Settings.highCurrencyValue = config.getInt("high-currency-value", Settings.highCurrencyValue);
		Settings.highCurrencyMinCost = config.getInt("high-currency-min-cost", Settings.highCurrencyMinCost);
		Settings.highZeroItem = config.getInt("high-zero-item", Settings.highZeroItem);
		if (Settings.highCurrencyValue <= 0) Settings.highCurrencyItem = 0;
		
		msgSelectedNormalShop = config.getString("msg-selected-normal-shop", msgSelectedNormalShop);
		msgSelectedBookShop = config.getString("msg-selected-book-shop", msgSelectedBookShop);
		msgSelectedBuyShop = config.getString("msg-selected-buy-shop", msgSelectedBuyShop);
		msgSelectedChest = config.getString("msg-selected-chest", msgSelectedChest);
		msgMustSelectChest = config.getString("msg-must-select-chest", msgMustSelectChest);
		msgChestTooFar = config.getString("msg-chest-too-far", msgChestTooFar);
		
		msgPlayerShopCreated = config.getString("msg-player-shop-created", msgPlayerShopCreated);
		msgBookShopCreated = config.getString("msg-book-shop-created", msgBookShopCreated);
		msgBuyShopCreated = config.getString("msg-buy-shop-created", msgBuyShopCreated);
		msgAdminShopCreated = config.getString("msg-admin-shop-created", msgAdminShopCreated);
		msgShopCreateFail = config.getString("msg-shop-create-fail", msgShopCreateFail);
		msgTooManyShops = config.getString("msg-too-many-shops", msgTooManyShops);
		msgShopInUse = config.getString("msg-shop-in-use", msgShopInUse);
		
		Settings.recipeListVar = config.getString("recipe-list-var", Settings.recipeListVar);
				
		// load shopkeeper saved data
		load();
		
		// spawn villagers in loaded chunks
		for (World world : Bukkit.getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks()) {
				loadShopkeepersInChunk(chunk);
			}
		}
		
		// register events
		getServer().getPluginManager().registerEvents(this, this);
		if (protectChests) {
			getServer().getPluginManager().registerEvents(new ChestProtectListener(this), this);
		}
		
		// start teleporter
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
					shopkeeper.teleport();
				}
			}
		}, 200, 200);
		
		// start saver
		if (!saveInstantly) {
			Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				public void run() {
					if (dirty) {
						saveReal();
						dirty = false;
					}
				}
			}, 6000, 6000);
		}
	}
	
	@Override
	public void onDisable() {
		if (dirty) {
			saveReal();
			dirty = false;
		}
		
		for (String playerName : editing.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
				player.closeInventory();
			}
		}
		editing.clear();
		
		for (String playerName : purchasing.keySet()) {
			Player player = Bukkit.getPlayerExact(playerName);
			if (player != null) {
				player.closeInventory();
			}
		}
		purchasing.clear();
		
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
			shopkeeper.remove();
		}
		activeShopkeepers.clear();
		allShopkeepersByChunk.clear();
		
		selectedShopType.clear();
		selectedChest.clear();
		
		HandlerList.unregisterAll((Plugin)this);		
		Bukkit.getScheduler().cancelTasks(this);
	}
	
	/**
	 * Reloads the plugin.
	 */
	public void reload() {
		onDisable();
		onEnable();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("shopkeeper.reload")) {
			// reload command
			reload();
			sender.sendMessage(ChatColor.GREEN + "Shopkeepers plugin reloaded!");
			return true;
		} else if (sender instanceof Player) {
			Player player = (Player)sender;
			if (!player.hasPermission("shopkeeper.admin") && !player.hasPermission("shopkeeper.player")) return true;
			
			// get the profession, default to farmer if an invalid one is specified
			int prof = 0;
			if (args.length > 0) {
				if (args[0].matches("[0-9]+")) {
					prof = Integer.parseInt(args[0]);
					if (prof > 5) {
						prof = 0;
					}
				} else {
					Profession p = Profession.valueOf(args[0].toUpperCase());
					if (p != null) {
						prof = p.getId();
					}
				}
			}
			
			// get the spawn location for the shopkeeper
			Block block = player.getTargetBlock(null, 10);
			if (block != null && block.getType() != Material.AIR) {
				if (createPlayerShopWithCommand && block.getType() == Material.CHEST && player.hasPermission("shopkeeper.player")) {
					// check if already a chest
					if (isChestProtected(null, block)) {
						return true;
					}
					// check for permission
					PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), block, BlockFace.UP);
					Bukkit.getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						return true;
					}
					// create the player shopkeeper
					createNewPlayerShopkeeper(player, block, block.getLocation().add(0, 1.5, 0), prof, 0);
					sendMessage(player, msgPlayerShopCreated);
				} else if (player.hasPermission("shopkeeper.admin")) {
					// create the admin shopkeeper
					createNewAdminShopkeeper(block.getLocation().add(0, 1.5, 0), prof);
					sendMessage(player, msgAdminShopCreated);
				}
			} else {
				sendMessage(player, msgShopCreateFail);
			}
			
			return true;
		} else {
			sender.sendMessage("You must be a player to create a shopkeeper.");
			sender.sendMessage("Use 'shopkeeper reload' to reload the plugin.");
			return true;
		}
	}
	
	/**
	 * Creates a new admin shopkeeper and spawns it into the world.
	 * @param location the block location the shopkeeper should spawn
	 * @param profession the shopkeeper's profession, a number from 0 to 5
	 * @return the shopkeeper created
	 */
	public Shopkeeper createNewAdminShopkeeper(Location location, int profession) {
		// make sure profession is valid
		if (profession < 0 || profession > 5) {
			profession = 0;
		}
		// create the shopkeeper (and spawn it)
		Shopkeeper shopkeeper = new AdminShopkeeper(location, profession);
		shopkeeper.spawn();
		activeShopkeepers.put(shopkeeper.getEntityId(), shopkeeper);
		addShopkeeper(shopkeeper);
		
		return shopkeeper;
	}

	/**
	 * Creates a new player-based shopkeeper and spawns it into the world.
	 * @param player the player who created the shopkeeper
	 * @param chest the backing chest for the shop
	 * @param location the block location the shopkeeper should spawn
	 * @param profession the shopkeeper's profession, a number from 0 to 5
	 * @param type the player shop type (0=normal, 1=book, 2=buy)
	 * @return the shopkeeper created
	 */
	public Shopkeeper createNewPlayerShopkeeper(Player player, Block chest, Location location, int profession, int type) {
		// make sure profession is valid
		if (profession < 0 || profession > 5) {
			profession = 0;
		}
		
		ShopkeeperType shopType = null;
		if (type == 0) {
			shopType = ShopkeeperType.PLAYER_NORMAL;
		} else if (type == 1) {
			shopType = ShopkeeperType.PLAYER_BOOK;
		} else if (type == 2) {
			shopType = ShopkeeperType.PLAYER_BUY;
		}
		if (shopType == null) {
			return null;
		}
		
		int maxShops = maxShopsPerPlayer;
		
		// call event
		CreatePlayerShopkeeperEvent event = new CreatePlayerShopkeeperEvent(player, chest, location, profession, shopType, maxShops);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return null;
		} else {
			location = event.getSpawnLocation();
			profession = event.getProfessionId();
			shopType = event.getType();
			maxShops = event.getMaxShopsForPlayer();
		}
		
		// count owned shops
		if (maxShops > 0) {
			int count = 0;
			for (List<Shopkeeper> list : allShopkeepersByChunk.values()) {
				for (Shopkeeper shopkeeper : list) {
					if (shopkeeper instanceof PlayerShopkeeper && ((PlayerShopkeeper)shopkeeper).getOwner().equalsIgnoreCase(player.getName())) {
						count++;
					}
				}
			}
			if (count >= maxShops) {
				sendMessage(player, msgTooManyShops);
				return null;
			}
		}
		
		// create the shopkeeper
		Shopkeeper shopkeeper = null;
		if (shopType == ShopkeeperType.PLAYER_NORMAL) {
			if (allowCustomQuantities) {
				shopkeeper = new CustomQuantityPlayerShopkeeper(player, chest, location, profession);
			} else {
				shopkeeper = new FixedQuantityPlayerShopkeeper(player, chest, location, profession);
			}
		} else if (shopType == ShopkeeperType.PLAYER_BOOK) {
			shopkeeper = new WrittenBookPlayerShopkeeper(player, chest, location, profession);
		} else if (shopType == ShopkeeperType.PLAYER_BUY) {
			shopkeeper = new BuyingPlayerShopkeeper(player, chest, location, profession);
		}

		// spawn and save the shopkeeper
		if (shopkeeper != null) {
			shopkeeper.spawn();
			activeShopkeepers.put(shopkeeper.getEntityId(), shopkeeper);
			addShopkeeper(shopkeeper);
		}
		
		return shopkeeper;
	}
	
	void addShopkeeper(Shopkeeper shopkeeper) {
		// add to chunk list
		List<Shopkeeper> list = allShopkeepersByChunk.get(shopkeeper.getChunk());
		if (list == null) {
			list = new ArrayList<Shopkeeper>();
			allShopkeepersByChunk.put(shopkeeper.getChunk(), list);
		}
		list.add(shopkeeper);
		// save all data
		save();
	}
	
	/**
	 * Gets the shopkeeper by the villager's entity id.
	 * @param entityId the entity id of the villager
	 * @return the Shopkeeper, or null if the enitity with the given id is not a shopkeeper
	 */
	public Shopkeeper getShopkeeperByEntityId(int entityId) {
		return activeShopkeepers.get(entityId);
	}
	
	/**
	 * Gets all shopkeepers from a given chunk. Returns null if there are no shopkeepers in that chunk.
	 * @param world the world
	 * @param x chunk x-coordinate
	 * @param z chunk z-coordinate
	 * @return a list of shopkeepers, or null if there are none
	 */
	public List<Shopkeeper> getShopkeepersInChunk(String world, int x, int z) {
		return allShopkeepersByChunk.get(world + "," + x + "," + z);
	}
	
	@EventHandler
	void onEntityInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Villager) {
			debug("Player " + event.getPlayer().getName() + " is interacting with villager at " + event.getRightClicked().getLocation());
			Shopkeeper shopkeeper = activeShopkeepers.get(event.getRightClicked().getEntityId());
			if (event.isCancelled()) {
				debug("  Cancelled by another plugin");
			} else if (shopkeeper != null && event.getPlayer().isSneaking()) {
				// modifying a shopkeeper
				debug("  Opening editor window...");
				boolean isEditing = shopkeeper.onEdit(event.getPlayer());
				if (isEditing) {
					debug("  Editor window opened");
					event.setCancelled(true);
					editing.put(event.getPlayer().getName(), event.getRightClicked().getEntityId());
				} else {
					debug("  Editor window NOT opened");
				}
			} else if (shopkeeper != null) {
				// only allow one person per shopkeeper
				debug("  Opening trade window...");
				OpenTradeEvent evt = new OpenTradeEvent(event.getPlayer(), shopkeeper);
				Bukkit.getPluginManager().callEvent(evt);
				if (evt.isCancelled()) {
					debug("  Trade cancelled by another plugin");
					event.setCancelled(true);
					return;
				}
				if (purchasing.containsValue(event.getRightClicked().getEntityId())) {
					debug("  Villager already in use!");
					sendMessage(event.getPlayer(), msgShopInUse);
					event.setCancelled(true);
					return;
				}
				// set the trade recipe list (also prevent shopkeepers adding their own recipes by refreshing them with our list)
				shopkeeper.updateRecipes();
				purchasing.put(event.getPlayer().getName(), event.getRightClicked().getEntityId());
				debug("  Trade window opened");
			} else if (disableOtherVillagers && shopkeeper == null) {
				// don't allow trading with other villagers
				debug("  Non-shopkeeper, trade prevented");
				event.setCancelled(true);
			} else if (shopkeeper == null) {
				debug("  Non-shopkeeper");
			}
		}
	}
	
	@EventHandler
	void onInventoryClose(InventoryCloseEvent event) {
		String name = event.getPlayer().getName();
		if (editing.containsKey(name)) {
			debug("Player " + name + " closed editor window");
			int entityId = editing.remove(name);
			Shopkeeper shopkeeper = activeShopkeepers.get(entityId);
			if (shopkeeper != null) {
				if (event.getInventory().getTitle().equals(Settings.editorTitle)) {
					shopkeeper.onEditorClose(event);
					closeTradingForShopkeeper(entityId);
				}
			}
		}
		if (purchasing.containsKey(name)) {
			debug("Player " + name + " closed trade window");
			purchasing.remove(name);
		}
	}
	
	@EventHandler
	void onEntityDamage(EntityDamageEvent event) {
		// don't allow damaging shopkeepers!
		if (activeShopkeepers.containsKey(event.getEntity().getEntityId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	void onInventoryClick(InventoryClickEvent event) {
		// shopkeeper editor click
		if (event.getInventory().getTitle().equals(Settings.editorTitle)) {
			if (editing.containsKey(event.getWhoClicked().getName())) {
				// get the shopkeeper being edited
				int entityId = editing.get(event.getWhoClicked().getName());
				Shopkeeper shopkeeper = activeShopkeepers.get(entityId);
				if (shopkeeper != null) {
					// editor click
					EditorClickResult result = shopkeeper.onEditorClick(event);
					if (result == EditorClickResult.DELETE_SHOPKEEPER) {
						// close inventories
						event.getWhoClicked().closeInventory();
						editing.remove(event.getWhoClicked().getName());
						closeTradingForShopkeeper(entityId);
						
						// return egg
						if (deletingPlayerShopReturnsEgg && shopkeeper instanceof PlayerShopkeeper) {
							event.getWhoClicked().getInventory().addItem(new ItemStack(Material.MONSTER_EGG, 1, (short)120));
						}
						
						// remove shopkeeper
						activeShopkeepers.remove(entityId);
						allShopkeepersByChunk.get(shopkeeper.getChunk()).remove(shopkeeper);
						save();
					} else if (result == EditorClickResult.DONE_EDITING) {
						// end the editing session
						event.getWhoClicked().closeInventory();
						editing.remove(event.getWhoClicked().getName());
						closeTradingForShopkeeper(entityId);
						save();
					} else if (result == EditorClickResult.SAVE_AND_CONTINUE) {
						save();
					}
				} else {
					event.setCancelled(true);
					event.getWhoClicked().closeInventory();
				}
			} else {
				event.setCancelled(true);
				event.getWhoClicked().closeInventory();
			}
		}
		// purchase click
		if (event.getInventory().getName().equals("mob.villager") && event.getRawSlot() == 2 && purchasing.containsKey(event.getWhoClicked().getName())) {
			int entityId = purchasing.get(event.getWhoClicked().getName());
			Shopkeeper shopkeeper = activeShopkeepers.get(entityId);
			if (shopkeeper != null) {
				shopkeeper.onPurchaseClick(event);
			}
		}
	}
	
	@EventHandler
	void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		
		// prevent opening shop chests
		if (event.hasBlock() && event.getClickedBlock().getType() == Material.CHEST) {
			Block block = event.getClickedBlock();
			
			// check for protected chest
			if (!event.getPlayer().hasPermission("shopkeeper.bypass")) {
				if (isChestProtected(player, block)) {
					event.setCancelled(true);
					return;
				}
				for (BlockFace face : faces) {
					if (block.getRelative(face).getType() == Material.CHEST) {
						if (isChestProtected(player, block.getRelative(face))) {
							event.setCancelled(true);
							return;
						}				
					}
				}
			}
		}
		
		// check for player shop spawn
		if (createPlayerShopWithEgg && player.hasPermission("shopkeeper.player") && player.getGameMode() != GameMode.CREATIVE) {
			String playerName = player.getName();
			ItemStack inHand = player.getItemInHand();
			if (inHand != null && inHand.getType() == Material.MONSTER_EGG && inHand.getDurability() == 120) {
				event.setCancelled(true);
				if (event.getAction() == Action.RIGHT_CLICK_AIR) {
					// cycle shop options
					int option = 0;
					if (selectedShopType.containsKey(playerName)) {
						option = selectedShopType.get(playerName) + 1;
						if (option > 2) {
							option = 0;
						}
					}
					if (option == 1 && !allowPlayerBookShop) {
						option = 2;
					}
					selectedShopType.put(playerName, option);
					if (option == 0) {
						sendMessage(player, msgSelectedNormalShop);
					} else if (option == 1) {
						sendMessage(player, msgSelectedBookShop);
					} else if (option == 2) {
						sendMessage(player, msgSelectedBuyShop);
					}
				} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
					Block block = event.getClickedBlock();
					if (block.getType() == Material.CHEST && (!selectedChest.containsKey(playerName) || !selectedChest.get(playerName).equals(block))) {
						// select chest
						selectedChest.put(playerName, event.getClickedBlock());
						sendMessage(player, msgSelectedChest);
					} else {
						Block chest = selectedChest.get(playerName);
						if (chest == null) {
							sendMessage(player, msgMustSelectChest);
						} else if ((int)chest.getLocation().distance(block.getLocation()) > maxChestDistance) {
							sendMessage(player, msgChestTooFar);
						} else {
							// create player shopkeeper
							int option = 0;
							if (selectedShopType.containsKey(playerName)) {
								option = selectedShopType.get(playerName);
							}
							Shopkeeper shopkeeper = createNewPlayerShopkeeper(player, chest, block.getLocation().add(0, 1.5, 0), 0, option);
							if (shopkeeper != null) {
								// send message
								if (option == 0) {
									sendMessage(player, msgPlayerShopCreated);
								} else if (option == 1) {
									sendMessage(player, msgBookShopCreated);
								} else if (option == 2) {
									sendMessage(player, msgBuyShopCreated);
								} else {
									return;
								}
								// remove egg
								inHand.setAmount(inHand.getAmount() - 1);
								if (inHand.getAmount() > 0) {
									player.setItemInHand(inHand);
								} else {
									player.setItemInHand(null);
								}
							}
							// clear selection vars
							selectedShopType.remove(playerName);
							selectedChest.remove(playerName);
						}
					}
				}
				
			}
		}
	}
		
	@EventHandler(priority=EventPriority.LOWEST)
	void onChunkLoad(ChunkLoadEvent event) {
		loadShopkeepersInChunk(event.getChunk());
	}

	@EventHandler
	void onChunkUnload(ChunkUnloadEvent event) {
		List<Shopkeeper> shopkeepers = allShopkeepersByChunk.get(event.getWorld().getName() + "," + event.getChunk().getX() + "," + event.getChunk().getZ());
		if (shopkeepers != null) {
			debug("Unloading " + shopkeepers.size() + " shopkeepers in chunk " + event.getChunk().getX() + "," + event.getChunk().getZ());
			for (Shopkeeper shopkeeper : shopkeepers) {
				if (shopkeeper.isActive()) {
					activeShopkeepers.remove(shopkeeper.getEntityId());
					shopkeeper.remove();
				}
			}
		}
	}
	
	@EventHandler
	void onWorldLoad(WorldLoadEvent event) {
		for (Chunk chunk : event.getWorld().getLoadedChunks()) {
			loadShopkeepersInChunk(chunk);
		}
	}
	
	@EventHandler
	void onWorldUnload(WorldUnloadEvent event) {
		String worldName = event.getWorld().getName();
		Iterator<Shopkeeper> iter = activeShopkeepers.values().iterator();
		int count = 0;
		while (iter.hasNext()) {
			Shopkeeper shopkeeper = iter.next();
			if (shopkeeper.getWorldName().equals(worldName)) {
				shopkeeper.remove();
				iter.remove();
				count++;
			}
		}
		debug("Unloaded " + count + " shopkeepers in unloaded world " + worldName);
	}
	
	@EventHandler
	void onPlayerQuit(PlayerQuitEvent event) {
		String name = event.getPlayer().getName();
		editing.remove(name);
		purchasing.remove(name);
		selectedShopType.remove(name);
		selectedChest.remove(name);
	}

	private void closeTradingForShopkeeper(int entityId) {
		Iterator<String> editors = editing.keySet().iterator();
		while (editors.hasNext()) {
			String name = editors.next();
			if (editing.get(name).equals(entityId)) {
				Player player = Bukkit.getPlayerExact(name);
				if (player != null) {
					player.closeInventory();
				}
				editors.remove();
			}
		}
		Iterator<String> purchasers = purchasing.keySet().iterator();
		while (purchasers.hasNext()) {
			String name = purchasers.next();
			if (purchasing.get(name).equals(entityId)) {
				Player player = Bukkit.getPlayerExact(name);
				if (player != null) {
					player.closeInventory();
				}
				purchasers.remove();
			}
		}
	}
	
	public boolean isChestProtected(Player player, Block block) {
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
			if (shopkeeper instanceof PlayerShopkeeper) {
				PlayerShopkeeper pshop = (PlayerShopkeeper)shopkeeper;
				if ((player == null || !pshop.getOwner().equalsIgnoreCase(player.getName())) && pshop.usesChest(block)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void sendMessage(Player player, String message) {
		message = ChatColor.translateAlternateColorCodes('&', message);
		String[] msgs = message.split("\n");
		for (String msg : msgs) {
			player.sendMessage(msg);
		}
	}
	
	private void loadShopkeepersInChunk(Chunk chunk) {
		List<Shopkeeper> shopkeepers = allShopkeepersByChunk.get(chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ());
		if (shopkeepers != null) {
			debug("Loading " + shopkeepers.size() + " shopkeepers in chunk " + chunk.getX() + "," + chunk.getZ());
			for (Shopkeeper shopkeeper : shopkeepers) {
				if (!shopkeeper.isActive()) {
					shopkeeper.spawn();
					activeShopkeepers.put(shopkeeper.getEntityId(), shopkeeper);
				}
			}
			save();
		}
	}
	
	private void load() {
		File file = new File(getDataFolder(), "save.yml");
		if (!file.exists()) return;
		
		YamlConfiguration config = new YamlConfiguration();
		try {
			config.load(file);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		Set<String> keys = config.getKeys(false);
		for (String key : keys) {
			ConfigurationSection section = config.getConfigurationSection(key);
			Shopkeeper shopkeeper = null;
			String type = section.getString("type", "");
			if (type.equals("book")) {
				if (allowPlayerBookShop) {
					shopkeeper = new WrittenBookPlayerShopkeeper(section);
				}
			} else if (type.equals("buy")) {
				shopkeeper = new BuyingPlayerShopkeeper(section);
			} else if (type.equals("player") || section.contains("owner")) {
				if (allowCustomQuantities) {
					shopkeeper = new CustomQuantityPlayerShopkeeper(section);
				} else {
					shopkeeper = new FixedQuantityPlayerShopkeeper(section);
				}
			} else {
				shopkeeper = new AdminShopkeeper(section);
			}
			if (shopkeeper != null) {
				List<Shopkeeper> list = allShopkeepersByChunk.get(shopkeeper.getChunk());
				if (list == null) {
					list = new ArrayList<Shopkeeper>();
					allShopkeepersByChunk.put(shopkeeper.getChunk(), list);
				}
				list.add(shopkeeper);
			}
		}
	}
	
	private void save() {
		if (saveInstantly) {
			saveReal();
		} else {
			dirty = true;
		}
	}
	
	private void saveReal() {
		YamlConfiguration config = new YamlConfiguration();
		int counter = 0;
		for (List<Shopkeeper> shopkeepers : allShopkeepersByChunk.values()) {
			for (Shopkeeper shopkeeper : shopkeepers) {
				ConfigurationSection section = config.createSection(counter + "");
				shopkeeper.save(section);
				counter++;
			}
		}
		
		File file = new File(getDataFolder(), "save.yml");
		if (file.exists()) {
			file.delete();
		}
		try {
			config.save(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void debug(String message) {
		if (plugin.debug) {
			plugin.getLogger().info(message);
		}
	}

}
