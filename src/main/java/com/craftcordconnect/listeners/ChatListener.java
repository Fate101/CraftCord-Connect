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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.Material;
import org.bukkit.map.MapView;
import com.craftcordconnect.managers.DiscordManager;
import java.util.List;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;

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
        // Filter out root advancements and all recipe unlocks
        if (advancementKey.contains("root") || advancementKey.startsWith("recipes/")) return;
        // Try to get a nice display name if possible
        String title = null;
        if (event.getAdvancement().getDisplay() != null && event.getAdvancement().getDisplay().getTitle() != null) {
            title = event.getAdvancement().getDisplay().getTitle().toString();
        }
        String message = title != null ? "**" + player.getName() + "** has made the advancement **" + title + "**" : "**" + player.getName() + "** has made an advancement!";
        plugin.getDiscordManager().sendMessageToDiscord(player, "🏆 " + message);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.FILLED_MAP && item.hasItemMeta()) {
            MapMeta meta = (MapMeta) item.getItemMeta();
            MapView view = meta.getMapView();
            if (view != null) {
                DiscordManager discordManager = plugin.getDiscordManager();
                List<MapView> temp = discordManager != null ? discordManager.tempMaps.get(event.getPlayer().getUniqueId()) : null;
                if (temp != null && temp.contains(view)) {
                    event.getPlayer().sendMessage("§eThis is a temporary map from Discord. It will be removed now.");
                    event.getPlayer().getInventory().remove(item);
                    temp.remove(view);
                    discordManager.restorePreviousMainHandItem(event.getPlayer());
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getPreviousSlot());
        if (item != null && item.getType() == Material.FILLED_MAP && item.hasItemMeta()) {
            MapMeta meta = (MapMeta) item.getItemMeta();
            MapView view = meta.getMapView();
            DiscordManager discordManager = plugin.getDiscordManager();
            List<MapView> temp = discordManager != null ? discordManager.tempMaps.get(player.getUniqueId()) : null;
            if (temp != null && view != null && temp.contains(view)) {
                discordManager.restorePreviousMainHandItem(player);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.FILLED_MAP || !item.hasItemMeta()) return;

        MapMeta meta = (MapMeta) item.getItemMeta();
        MapView view = meta.getMapView();
        DiscordManager discordManager = plugin.getDiscordManager();
        List<MapView> temp = discordManager != null ? discordManager.tempMaps.get(player.getUniqueId()) : null;
        if (temp != null && view != null && temp.contains(view)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot move temporary Discord maps!");
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getOldCursor();
        if (item == null || item.getType() != Material.FILLED_MAP || !item.hasItemMeta()) return;

        MapMeta meta = (MapMeta) item.getItemMeta();
        MapView view = meta.getMapView();
        DiscordManager discordManager = plugin.getDiscordManager();
        List<MapView> temp = discordManager != null ? discordManager.tempMaps.get(player.getUniqueId()) : null;
        if (temp != null && view != null && temp.contains(view)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot move temporary Discord maps!");
        }
    }
} 