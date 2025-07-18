package com.craftcordconnect.listeners;

import com.craftcordconnect.CraftCordConnect;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;

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
                    "✨ " + event.getPlayer().getName() + " joined the server"
                );
            }, 20L); // 1 second delay
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getConfigManager().isServerStatusEnabled() && 
            plugin.getConfigManager().isSystemEventEnabled("player-leave")) {
            plugin.getDiscordManager().sendServerStatus(
                "💨 " + event.getPlayer().getName() + " left the server"
            );
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().getConfig().getBoolean("relay.death-messages", true)) return;
        Player player = event.getEntity();
        String deathMessage = event.getDeathMessage();
        if (deathMessage != null && !deathMessage.isEmpty()) {
            plugin.getDiscordManager().sendMessageToDiscord(player, "💀 " + deathMessage);
        }
    }

    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        if (!plugin.getConfigManager().getConfig().getBoolean("relay.advancement-messages", true)) return;
        Player player = event.getPlayer();
        if (!player.getWorld().getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS)) return;
        String advancementKey = event.getAdvancement().getKey().getKey();
        // Filter out root advancements (like "recipes/root")
        if (advancementKey.contains("root")) return;
        // Try to get a nice display name if possible
        String title = null;
        if (event.getAdvancement().getDisplay() != null && event.getAdvancement().getDisplay().getTitle() != null) {
            title = event.getAdvancement().getDisplay().getTitle().toString();
        }
        String message = title != null ? "**" + player.getName() + "** has made the advancement **" + title + "**" : "**" + player.getName() + "** has made an advancement!";
        plugin.getDiscordManager().sendMessageToDiscord(player, "🏆 " + message);
    }
} 