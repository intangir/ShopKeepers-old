package com.nisovin.shopkeepers.events;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is called whenever a player attempts to create a player shopkeeper.
 * It is called before the max shops check for the player. The location, profession
 * shopkeeper type, and player's max shops can be modified. If this event is cancelled,
 * the shop will not be created.
 *
 */
public class CreatePlayerShopkeeperEvent extends Event implements Cancellable {

	private Player player;
	private Block chest;
	private Location location;
	private int profession;
	private int maxShops;
	
	private boolean cancelled;

	public CreatePlayerShopkeeperEvent(Player player, Block chest, Location location, int maxShops) {
		this.player = player;
		this.chest = chest;
		this.location = location;
		this.profession = 0;
		this.maxShops = maxShops;
	}
	
	/**
	 * Gets the player trying to create the shop.
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}
	
	/**
	 * Gets the chest block that will be backing this shop.
	 * @return the chest block
	 */
	public Block getChest() {
		return chest;
	}
	
	/**
	 * Gets the block location the villager will spawn at.
	 * @return the spawn location
	 */
	public Location getSpawnLocation() {
		return location;
	}
	
	/**
	 * Gets the profession id of the villager shopkeeper.
	 * @return the profession id
	 */
	@Deprecated
	public int getProfessionId() {
		return profession;
	}
	
	/**
	 * Gets the maximum number of shops this player can have.
	 * @return player max shops
	 */
	public int getMaxShopsForPlayer() {
		return maxShops;
	}
	
	/**
	 * Sets the location the villager will spawn at.
	 * @param location the spawn location
	 */
	public void setSpawnLocation(Location location) {
		this.location = location;
	}
	
	/**
	 * Sets the profession id of the shopkeeper villager. This should be a number between 0 and 5.
	 * @param profession the profession id
	 */
	@Deprecated
	public void setProfessionId(int profession) {
	}
	
	/**
	 * Sets the maximum number of shops the creating player can have. If they have more than this number,
	 * the shop will not be created.
	 * @param maxShops the player's max shops
	 */
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
