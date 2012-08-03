package com.nisovin.shopkeepers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopKeepersPlugin extends JavaPlugin implements Listener {

	private Map<String, List<Shopkeeper>> allShopkeepersByChunk = new HashMap<String, List<Shopkeeper>>();
	private Map<Integer, Shopkeeper> activeShopkeepers = new HashMap<Integer, Shopkeeper>();
	private Map<String, Integer> editing = new HashMap<String, Integer>();
	
	@Override
	public void onEnable() {
		load();
		
		for (World world : Bukkit.getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks()) {
				loadShopkeepersInChunk(chunk);
			}
		}
		
		getServer().getPluginManager().registerEvents(this, this);
		
	}
	
	@Override
	public void onDisable() {
		for (Shopkeeper shopkeeper : activeShopkeepers.values()) {
			shopkeeper.remove();
		}
		activeShopkeepers.clear();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = (Player)sender;
		
		int prof = 0;
		if (args.length > 0) {
			if (args[0].matches("[0-9]+")) {
				prof = Integer.parseInt(args[0]);
				if (prof > 4) {
					prof = 0;
				}
			} else {
				Profession p = Profession.valueOf(args[0].toUpperCase());
				if (p != null) {
					prof = p.getId();
				}
			}
		}
		
		Block block = player.getTargetBlock(null, 10);
		if (block != null && block.getType() != Material.AIR) {
			Shopkeeper shopkeeper = new Shopkeeper(block.getLocation().add(0, 1.5, 0), prof);
			activeShopkeepers.put(shopkeeper.getEntityId(), shopkeeper);
			List<Shopkeeper> list = allShopkeepersByChunk.get(shopkeeper.getChunk());
			if (list == null) {
				list = new ArrayList<Shopkeeper>();
				allShopkeepersByChunk.put(shopkeeper.getChunk(), list);
			}
			list.add(shopkeeper);
			save();
		}
		
		return true;
	}
	
	@EventHandler
	public void onEntityInteract(PlayerInteractEntityEvent event) {
		if (event.getPlayer().hasPermission("shopkeeper.modify") && event.getPlayer().isSneaking() && activeShopkeepers.containsKey(event.getRightClicked().getEntityId())) {
			event.setCancelled(true);
			Shopkeeper shopkeeper = activeShopkeepers.get(event.getRightClicked().getEntityId());
			Inventory inv = Bukkit.createInventory(event.getPlayer(), 27, "Shopkeeper Editor");
			List<ItemStack[]> recipes = shopkeeper.getRecipes();
			for (int i = 0; i < recipes.size() && i < 8; i++) {
				ItemStack[] recipe = recipes.get(i);
				inv.setItem(i, recipe[0]);
				inv.setItem(i + 9, recipe[1]);
				inv.setItem(i + 18, recipe[2]);
			}
			inv.setItem(8, new ItemStack(Material.EMERALD_BLOCK));
			inv.setItem(26, new ItemStack(Material.FIRE));
			event.getPlayer().openInventory(inv);
			editing.put(event.getPlayer().getName(), event.getRightClicked().getEntityId());
		} else if (!activeShopkeepers.containsKey(event.getRightClicked().getEntityId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (activeShopkeepers.containsKey(event.getEntity().getEntityId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (editing.containsKey(event.getWhoClicked().getName()) && event.getInventory().getTitle().equals("Shopkeeper Editor")) {
			int entityId = editing.get(event.getWhoClicked().getName());
			Shopkeeper shopkeeper = activeShopkeepers.get(entityId);
			if (shopkeeper != null) {
				if (event.getSlot() == 8) {
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
					event.setCancelled(true);
					event.getWhoClicked().closeInventory();
					editing.remove(event.getWhoClicked().getName());
				} else if (event.getSlot() == 26) {
					shopkeeper.remove();
					activeShopkeepers.remove(entityId);
					allShopkeepersByChunk.get(shopkeeper.getChunk()).remove(shopkeeper);
					save();
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
