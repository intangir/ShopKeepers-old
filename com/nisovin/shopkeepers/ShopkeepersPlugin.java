package com.nisovin.shopkeepers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopkeepersPlugin extends JavaPlugin implements Listener {

	private Map<String, List<Shopkeeper>> allShopkeepersByChunk = new HashMap<String, List<Shopkeeper>>();
	private Map<Integer, Shopkeeper> activeShopkeepers = new HashMap<Integer, Shopkeeper>();
	private Map<String, Integer> editing = new HashMap<String, Integer>();
	
	private boolean disableOtherVillagers = true;
	static String recipeListVar = "i";
	
	@Override
	public void onEnable() {
		// get config
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			saveDefaultConfig();
		}
		Configuration config = getConfig();
		disableOtherVillagers = config.getBoolean("disable-other-villagers", disableOtherVillagers);
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
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
			shopkeeper.remove();
		}
		activeShopkeepers.clear();		
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
				// create the shopkeeper
				createNewShopkeeper(block.getLocation().add(0, 1.5, 0), prof);
				sender.sendMessage(ChatColor.GREEN + "Shopkeeper created!");
				sender.sendMessage(ChatColor.GREEN + "Right-click the villager while sneaking to modify trades.");
			} else {
				sender.sendMessage(ChatColor.GREEN + "Please look at a block to create a shopkeeper.");
			}
			
			return true;
		} else {
			sender.sendMessage(ChatColor.GREEN + "You must be a player to create a shopkeeper.");
			sender.sendMessage(ChatColor.GREEN + "Use 'shopkeeper reload' to reload the plugin.");
			return true;
		}
	}
	
	/**
	 * Creates a shopkeeper and spawns it into the world.
	 * @param location the location the shopkeeper should spawn
	 * @param profession the shopkeeper's profession, a number from 0 to 5
	 */
	public void createNewShopkeeper(Location location, int profession) {
		// make sure profession is valid
		if (profession < 0 || profession > 5) {
			profession = 0;
		}
		// create the shopkeeper (and spawn it)
		Shopkeeper shopkeeper = new Shopkeeper(location, profession);
		activeShopkeepers.put(shopkeeper.getEntityId(), shopkeeper);
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
		boolean isShopkeeper = activeShopkeepers.containsKey(event.getRightClicked().getEntityId());
		if (event.getPlayer().hasPermission("shopkeeper.modify") && event.getPlayer().isSneaking() && isShopkeeper) {
			// modifying a shopkeeper
			event.setCancelled(true);
			// get the shopkeeper's trade options
			Shopkeeper shopkeeper = activeShopkeepers.get(event.getRightClicked().getEntityId());
			Inventory inv = Bukkit.createInventory(event.getPlayer(), 27, "Shopkeeper Editor");
			List<ItemStack[]> recipes = shopkeeper.getRecipes();
			for (int i = 0; i < recipes.size() && i < 8; i++) {
				ItemStack[] recipe = recipes.get(i);
				inv.setItem(i, recipe[0]);
				inv.setItem(i + 9, recipe[1]);
				inv.setItem(i + 18, recipe[2]);
			}
			// add the special buttons
			inv.setItem(8, new ItemStack(Material.EMERALD_BLOCK));
			inv.setItem(26, new ItemStack(Material.FIRE));
			// show editing inventory
			event.getPlayer().openInventory(inv);
			editing.put(event.getPlayer().getName(), event.getRightClicked().getEntityId());
		} else if (isShopkeeper) {
			// prevent shopkeepers adding their own recipes by refreshing them with our list
			Shopkeeper shopkeeper = activeShopkeepers.get(event.getRightClicked().getEntityId());
			shopkeeper.updateRecipes();
		} else if (disableOtherVillagers && !isShopkeeper) {
			// don't allow trading with other villagers
			event.setCancelled(true);
		}
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
		if (editing.containsKey(event.getWhoClicked().getName()) && event.getInventory().getTitle().equals("Shopkeeper Editor")) {
			// get the shopkeeper being edited
			int entityId = editing.get(event.getWhoClicked().getName());
			Shopkeeper shopkeeper = activeShopkeepers.get(entityId);
			if (shopkeeper != null) {
				// check for special buttons
				if (event.getRawSlot() == 8) {
					// it's the save button - get the trades and save them to the shopkeeper
					Inventory inv = event.getInventory();
					List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
					for (int i = 0; i < 8; i++) {
						if (inv.getItem(i) != null && inv.getItem(i + 18) != null) {
							ItemStack[] recipe = new ItemStack[3];
							recipe[0] = inv.getItem(i);
							recipe[1] = inv.getItem(i + 9);
							recipe[2] = inv.getItem(i + 18);
							recipes.add(recipe);
						}
					}
					shopkeeper.setRecipes(recipes);
					save();
					// close editing window
					event.setCancelled(true);
					event.getWhoClicked().closeInventory();
					editing.remove(event.getWhoClicked().getName());
				} else if (event.getRawSlot() == 26) {
					// it's the delete button - remove the shopkeeper
					shopkeeper.remove();
					activeShopkeepers.remove(entityId);
					allShopkeepersByChunk.get(shopkeeper.getChunk()).remove(shopkeeper);
					save();
					// close the editing window
					event.setCancelled(true);
					event.getWhoClicked().closeInventory();
					editing.remove(event.getWhoClicked().getName());
				}
			}
		}
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		loadShopkeepersInChunk(event.getChunk());
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
			Shopkeeper shopkeeper = new Shopkeeper(section);
			List<Shopkeeper> list = allShopkeepersByChunk.get(shopkeeper.getChunk());
			if (list == null) {
				list = new ArrayList<Shopkeeper>();
				allShopkeepersByChunk.put(shopkeeper.getChunk(), list);
			}
			list.add(shopkeeper);
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
