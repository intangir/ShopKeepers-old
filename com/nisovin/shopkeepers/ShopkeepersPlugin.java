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
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopkeepersPlugin extends JavaPlugin implements Listener {

	static ShopkeepersPlugin plugin;
	
	private Map<String, List<Shopkeeper>> allShopkeepersByChunk = new HashMap<String, List<Shopkeeper>>();
	private Map<Integer, Shopkeeper> activeShopkeepers = new HashMap<Integer, Shopkeeper>();
	private Map<String, Integer> editing = new HashMap<String, Integer>();
	private Map<String, Integer> purchasing = new HashMap<String, Integer>();
	
	private boolean disableOtherVillagers = true;
	private boolean createPlayerShopWithCommand = true;
	private boolean createPlayerShopWithEgg = true;
	
	static String editorTitle = "Shopkeeper Editor";
	static int saveItem = Material.EMERALD_BLOCK.getId();
	static int deleteItem = Material.FIRE.getId();
	
	static int currencyItem = Material.EMERALD.getId();
	static short currencyData = 0;
	static int zeroItem = Material.SLIME_BALL.getId();
	
	static int highCurrencyItem = Material.EMERALD_BLOCK.getId();
	static short highCurrencyData = 0;
	static int highCurrencyValue = 9;
	static int highCurrencyMinCost = 20;
	static int highZeroItem = Material.SLIME_BALL.getId();
		
	private String msgPlayerShopCreated = "&aShopkeeper created!\n&aAdd items you want to sell to your chest, then\n&aright-click the villager while sneaking to modify costs.";
	private String msgAdminShopCreated = "&aShopkeeper created!\n&aRight-click the villager while sneaking to modify trades.";
	private String msgShopCreateFail = "&aYou cannot create a shopkeeper there.";
	private String msgShopInUse = "&aSomeone else is already purchasing from this shopkeeper.";
	
	static String recipeListVar = "i";
	
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
		
		disableOtherVillagers = config.getBoolean("disable-other-villagers", disableOtherVillagers);
		createPlayerShopWithCommand = config.getBoolean("create-player-shop-with-command", createPlayerShopWithCommand);
		createPlayerShopWithEgg = config.getBoolean("create-player-shop-with-egg", createPlayerShopWithEgg);
		
		editorTitle = config.getString("editor-title", editorTitle);
		saveItem = config.getInt("save-item", saveItem);
		deleteItem = config.getInt("delete-item", deleteItem);
		
		currencyItem = config.getInt("currency-item", currencyItem);
		currencyData = (short)config.getInt("currency-item-data", currencyData);
		zeroItem = config.getInt("zero-item", zeroItem);
		
		highCurrencyItem = config.getInt("high-currency-item", highCurrencyItem);
		highCurrencyData = (short)config.getInt("high-currency-item-data", highCurrencyData);
		highCurrencyValue = config.getInt("high-currency-value", highCurrencyValue);
		highCurrencyMinCost = config.getInt("high-currency-min-cost", highCurrencyMinCost);
		highZeroItem = config.getInt("high-zero-item", highZeroItem);
		if (highCurrencyValue <= 0) highCurrencyItem = 0;
		
		msgPlayerShopCreated = config.getString("msg-player-shop-created", msgPlayerShopCreated);
		msgAdminShopCreated = config.getString("msg-admin-shop-created", msgAdminShopCreated);
		msgShopCreateFail = config.getString("msg-shop-create-fail", msgShopCreateFail);
		msgShopInUse = config.getString("msg-shop-in-use", msgShopInUse);
		
		recipeListVar = config.getString("recipe-list-var", recipeListVar);
				
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
		
		// start teleporter
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
					shopkeeper.teleport();
				}
			}
		}, 200, 200);
	}
	
	@Override
	public void onDisable() {
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
		HandlerList.unregisterAll((Plugin)this);		
		Bukkit.getScheduler().cancelTasks(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("reload") && sender.hasPermission("shopkeeper.reload")) {
			// reload command
			onDisable();
			onEnable();
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
					createNewPlayerShopkeeper(player, block, block.getLocation().add(0, 1.5, 0), prof);
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
	 * Creates a shopkeeper and spawns it into the world.
	 * @param location the location the shopkeeper should spawn
	 * @param profession the shopkeeper's profession, a number from 0 to 5
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
	
	public Shopkeeper createNewPlayerShopkeeper(Player player, Block chest, Location location, int profession) {
		// make sure profession is valid
		if (profession < 0 || profession > 5) {
			profession = 0;
		}
		// create the shopkeeper (and spawn it)
		Shopkeeper shopkeeper = new PlayerShopkeeper(player, chest, location, profession);
		shopkeeper.spawn();
		activeShopkeepers.put(shopkeeper.getEntityId(), shopkeeper);
		addShopkeeper(shopkeeper);
		
		return shopkeeper;
	}
	
	public void addShopkeeper(Shopkeeper shopkeeper) {
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
	
	@EventHandler
	public void onEntityInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Villager) {
			Shopkeeper shopkeeper = activeShopkeepers.get(event.getRightClicked().getEntityId());
			if (shopkeeper != null && event.getPlayer().isSneaking()) {
				// modifying a shopkeeper
				boolean isEditing = shopkeeper.onEdit(event.getPlayer());
				if (isEditing) {
					event.setCancelled(true);
					editing.put(event.getPlayer().getName(), event.getRightClicked().getEntityId());
				}
			} else if (shopkeeper != null) {
				// only allow one person per shopkeeper
				if (purchasing.containsValue(event.getRightClicked().getEntityId())) {
					sendMessage(event.getPlayer(), msgShopInUse);
					event.setCancelled(true);
					return;
				}
				// set the trade recipe list (also prevent shopkeepers adding their own recipes by refreshing them with our list)
				shopkeeper.updateRecipes();
				purchasing.put(event.getPlayer().getName(), event.getRightClicked().getEntityId());
			} else if (disableOtherVillagers && shopkeeper == null) {
				// don't allow trading with other villagers
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		editing.remove(event.getPlayer().getName());
		purchasing.remove(event.getPlayer().getName());
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		// don't allow damaging shopkeepers!
		if (activeShopkeepers.containsKey(event.getEntity().getEntityId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		// shopkeeper editor click
		if (editing.containsKey(event.getWhoClicked().getName()) && event.getInventory().getTitle().equals(editorTitle)) {
			// get the shopkeeper being edited
			int entityId = editing.get(event.getWhoClicked().getName());
			Shopkeeper shopkeeper = activeShopkeepers.get(entityId);
			if (shopkeeper != null) {
				// editor click
				EditorClickResult result = shopkeeper.onEditorClick(event);
				if (result == EditorClickResult.DELETE_SHOPKEEPER) {
					// delete the shopkeeper
					event.getWhoClicked().closeInventory();
					activeShopkeepers.remove(entityId);
					allShopkeepersByChunk.get(shopkeeper.getChunk()).remove(shopkeeper);
					save();
				}
				if (result == EditorClickResult.DONE_EDITING || result == EditorClickResult.DELETE_SHOPKEEPER) {
					// end the editing session
					event.getWhoClicked().closeInventory();
					editing.remove(event.getWhoClicked().getName());
					save();
				}
				if (result == EditorClickResult.SAVE_AND_CONTINUE) {
					save();
				}
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
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.hasBlock() && event.getClickedBlock().getType() == Material.CHEST) {
			Player player = event.getPlayer();
			Block block = event.getClickedBlock();
			
			// check for protected chest
			if (!event.getPlayer().hasPermission("shopkeeper.bypass")) {
				if (isChestProtected(player, block)) {
					event.setCancelled(true);
					return;
				}
				BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
				for (BlockFace face : faces) {
					if (block.getRelative(face).getType() == Material.CHEST) {
						if (isChestProtected(player, block.getRelative(face))) {
							event.setCancelled(true);
							return;
						}				
					}
				}
			}
			
			// check for player shop spawn
			if (createPlayerShopWithEgg && player.hasPermission("shopkeeper.player")) {
				ItemStack inHand = player.getItemInHand();
				if (inHand != null && inHand.getType() == Material.MONSTER_EGG && inHand.getDurability() == 120) {
					// create player shopkeeper
					createNewPlayerShopkeeper(player, block, block.getLocation().add(0, 1.5, 0), 0);
					sendMessage(player, msgPlayerShopCreated);
					// remove egg
					event.setCancelled(true);
					inHand.setAmount(inHand.getAmount() - 1);
					if (inHand.getAmount() > 0) {
						player.setItemInHand(inHand);
					} else {
						player.setItemInHand(null);
					}
				}
			}
		}
		
	}	

	private boolean isChestProtected(Player player, Block block) {
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
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		loadShopkeepersInChunk(event.getChunk());
	}

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		List<Shopkeeper> shopkeepers = allShopkeepersByChunk.get(event.getWorld().getName() + "," + event.getChunk().getX() + "," + event.getChunk().getZ());
		if (shopkeepers != null) {
			for (Shopkeeper shopkeeper : shopkeepers) {
				if (shopkeeper.isActive()) {
					activeShopkeepers.remove(shopkeeper.getEntityId());
					shopkeeper.remove();
				}
			}
		}
	}
	
	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		for (Chunk chunk : event.getWorld().getLoadedChunks()) {
			loadShopkeepersInChunk(chunk);
		}
	}
	
	@EventHandler
	public void onWorldUnload(WorldUnloadEvent event) {
		String worldName = event.getWorld().getName();
		Iterator<Shopkeeper> iter = activeShopkeepers.values().iterator();
		while (iter.hasNext()) {
			Shopkeeper shopkeeper = iter.next();
			if (shopkeeper.getWorldName().equals(worldName)) {
				shopkeeper.remove();
				iter.remove();
			}
		}
	}
	
	private void loadShopkeepersInChunk(Chunk chunk) {
		List<Shopkeeper> shopkeepers = allShopkeepersByChunk.get(chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ());
		if (shopkeepers != null) {
			for (Shopkeeper shopkeeper : shopkeepers) {
				if (!shopkeeper.isActive()) {
					shopkeeper.spawn();
					activeShopkeepers.put(shopkeeper.getEntityId(), shopkeeper);
				}
			}
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
			if (section.contains("owner")) {
				shopkeeper = new PlayerShopkeeper(section);
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

}
