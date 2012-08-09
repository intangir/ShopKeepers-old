package com.nisovin.shopkeepers.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.Shopkeeper;

public class OpenTradeEvent extends Event implements Cancellable {
	
	private Player player;
	private Shopkeeper shopkeeper;
	private boolean cancelled;
	
	public OpenTradeEvent(Player player, Shopkeeper shopkeeper) {
		this.player = player;
		this.shopkeeper = shopkeeper;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public Shopkeeper getShopkeeper() {
		return shopkeeper;
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
