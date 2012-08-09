package com.nisovin.shopkeepers.events;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.ShopkeeperType;

public class CreatePlayerShopkeeperEvent extends Event implements Cancellable {

	private Player player;
	private Block chest;
	private Location location;
	private int profession;
	private ShopkeeperType type;
	private int maxShops;
	
	private boolean cancelled;

	public CreatePlayerShopkeeperEvent(Player player, Block chest, Location location, int profession, ShopkeeperType type, int maxShops) {
		this.player = player;
		this.chest = chest;
		this.location = location;
		this.profession = profession;
		this.type = type;
		this.maxShops = maxShops;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public Block getChest() {
		return chest;
	}
	
	public Location getSpawnLocation() {
		return location;
	}
	
	public int getProfessionId() {
		return profession;
	}
	
	public ShopkeeperType getType() {
		return type;
	}
	
	public int getMaxShopsForPlayer() {
		return maxShops;
	}
	
	public void setSpawnLocation(Location location) {
		this.location = location;
	}
	
	public void setProfessionId(int profession) {
		if (profession >= 0 && profession <= 5) {
			this.profession = profession;
		}
	}
	
	public void setType(ShopkeeperType type) {
		if (type != ShopkeeperType.ADMIN) {
			this.type = type;
		}
	}
	
	public void setMaxShopsForPlayer(int maxShops) {
		this.maxShops = maxShops;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
    private static final HandlerList handlers = new HandlerList();
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
