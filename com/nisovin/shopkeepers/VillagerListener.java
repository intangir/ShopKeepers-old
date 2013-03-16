package com.nisovin.shopkeepers;


import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import com.nisovin.shopkeepers.events.OpenTradeEvent;

public class VillagerListener implements Listener {

	final ShopkeepersPlugin plugin;
	
	public VillagerListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	void onEntityInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Villager) {
			Villager villager = (Villager)event.getRightClicked();
			ShopkeepersPlugin.debug("Player " + event.getPlayer().getName() + " is interacting with villager at " + villager.getLocation());
			Shopkeeper shopkeeper = plugin.activeShopkeepers.get("entity" + villager.getEntityId());
			if (event.isCancelled()) {
				ShopkeepersPlugin.debug("  Cancelled by another plugin");
			} else if (shopkeeper != null && event.getPlayer().isSneaking()) {
				// modifying a shopkeeper
				ShopkeepersPlugin.debug("  Opening editor window...");
				event.setCancelled(true);
				boolean isEditing = shopkeeper.onEdit(event.getPlayer());
				if (isEditing) {
					ShopkeepersPlugin.debug("  Editor window opened");
					plugin.editing.put(event.getPlayer().getName(), shopkeeper.getId());
				} else {
					ShopkeepersPlugin.debug("  Editor window NOT opened");
				}
			} else if (shopkeeper != null) {
				// trading with shopkeeper
				ShopkeepersPlugin.debug("  Opening trade window...");
				OpenTradeEvent evt = new OpenTradeEvent(event.getPlayer(), shopkeeper);
				Bukkit.getPluginManager().callEvent(evt);
				if (evt.isCancelled()) {
					ShopkeepersPlugin.debug("  Trade cancelled by another plugin");
					event.setCancelled(true);
					return;
				}
				// open trade window
				event.setCancelled(true);
				plugin.openTradeWindow(shopkeeper, event.getPlayer());
				plugin.purchasing.put(event.getPlayer().getName(), shopkeeper.getId());
				ShopkeepersPlugin.debug("  Trade window opened");
			} else if (Settings.disableOtherVillagers && shopkeeper == null) {
				// don't allow trading with other villagers
				ShopkeepersPlugin.debug("  Non-shopkeeper, trade prevented");
				event.setCancelled(true);
			} else if (shopkeeper == null) {
				ShopkeepersPlugin.debug("  Non-shopkeeper");
			}
		}
	}

	@EventHandler
	void onEntityDamage(EntityDamageEvent event) {
		// don't allow damaging shopkeepers!
		if (plugin.activeShopkeepers.containsKey("entity" + event.getEntity().getEntityId())) {
			event.setCancelled(true);
			if (event instanceof EntityDamageByEntityEvent) {
				EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent)event;
				if (evt.getDamager() instanceof Monster) {
					evt.getDamager().remove();
				}
			}
		}
	}
	
	@EventHandler
	void onTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		if (target != null && target.getType() == EntityType.VILLAGER && plugin.activeShopkeepers.containsKey("entity" + target.getEntityId())) {
			event.setCancelled(true);
		}
	}
	
}
