package com.craftcordconnect.listeners;

import com.craftcordconnect.CraftCordConnect;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {
    
    private final CraftCordConnect plugin;
    
    public ChatListener(CraftCordConnect plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Relay chat message to Discord
        plugin.getDiscordManager().sendMessageToDiscord(event.getPlayer(), event.getMessage());
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.getConfigManager().isServerStatusEnabled() && 
            plugin.getConfigManager().isSystemEventEnabled("player-join")) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getDiscordManager().sendServerStatus(
                    "🟢 " + event.getPlayer().getName() + " joined the server"
                );
            }, 20L); // 1 second delay
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getConfigManager().isServerStatusEnabled() && 
            plugin.getConfigManager().isSystemEventEnabled("player-leave")) {
            plugin.getDiscordManager().sendServerStatus(
                "🔴 " + event.getPlayer().getName() + " left the server"
            );
        }
    }
} 