package com.craftcordconnect.listeners;

import com.craftcordconnect.CraftCordConnect;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ServerListener implements Listener {
    
    private final CraftCordConnect plugin;
    
    public ServerListener(CraftCordConnect plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (plugin.getConfigManager().isServerStatusEnabled() && 
            plugin.getConfigManager().isSystemEventEnabled("server-start")) {
            plugin.getDiscordManager().sendServerStatus("🟢 Server has started successfully!");
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // This event fires when a player joins the server
        // Could be used for additional status tracking if needed
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // This event fires when a player quits the server
        // Could be used for additional status tracking if needed
    }
} 