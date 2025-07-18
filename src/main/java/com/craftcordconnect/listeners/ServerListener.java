package com.craftcordconnect.listeners;

import com.craftcordconnect.CraftCordConnect;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.Material;
import org.bukkit.map.MapView;
import com.craftcordconnect.managers.DiscordManager;
import java.util.List;

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
        Player player = event.getPlayer();
        DiscordManager discordManager = plugin.getDiscordManager();
        if (discordManager != null) {
            List<MapView> temp = discordManager.tempMaps.remove(player.getUniqueId());
            if (temp != null) {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == Material.FILLED_MAP && item.hasItemMeta()) {
                        MapMeta meta = (MapMeta) item.getItemMeta();
                        MapView view = meta.getMapView();
                        if (view != null && temp.contains(view)) {
                            player.getInventory().remove(item);
                        }
                    }
                }
                // Restore the original item if it was overwritten by the map
                discordManager.restorePreviousMainHandItem(player);
            }
        }
    }
} 