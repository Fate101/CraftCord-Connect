package com.craftcordconnect.managers;

import com.craftcordconnect.CraftCordConnect;
import com.craftcordconnect.utils.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserManager {
    
    private final CraftCordConnect plugin;
    private final ConfigManager configManager;
    private final Map<UUID, UserPreferences> userPreferences;
    private final File userDataFile;
    private final FileConfiguration userData;
    
    public UserManager(CraftCordConnect plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.userPreferences = new HashMap<>();
        this.userDataFile = new File(plugin.getDataFolder(), "users.yml");
        this.userData = YamlConfiguration.loadConfiguration(userDataFile);
        loadUserData();
    }
    
    public boolean canReceiveDiscordMessages(Player player) {
        UserPreferences prefs = getUserPreferences(player.getUniqueId());
        return prefs.canReceive;
    }
    
    public boolean canSendDiscordMessages(Player player) {
        UserPreferences prefs = getUserPreferences(player.getUniqueId());
        return prefs.canSend;
    }
    
    public void setCanReceiveDiscordMessages(Player player, boolean canReceive) {
        UserPreferences prefs = getUserPreferences(player.getUniqueId());
        prefs.canReceive = canReceive;
        saveUserPreferences(player.getUniqueId());
    }
    
    public void setCanSendDiscordMessages(Player player, boolean canSend) {
        UserPreferences prefs = getUserPreferences(player.getUniqueId());
        prefs.canSend = canSend;
        saveUserPreferences(player.getUniqueId());
    }
    
    private UserPreferences getUserPreferences(UUID playerId) {
        return userPreferences.computeIfAbsent(playerId, uuid -> {
            UserPreferences prefs = new UserPreferences();
            prefs.canReceive = userData.getBoolean("users." + uuid + ".receive", configManager.getDefaultReceive());
            prefs.canSend = userData.getBoolean("users." + uuid + ".send", configManager.getDefaultSend());
            return prefs;
        });
    }
    
    private void saveUserPreferences(UUID playerId) {
        UserPreferences prefs = userPreferences.get(playerId);
        if (prefs != null) {
            userData.set("users." + playerId + ".receive", prefs.canReceive);
            userData.set("users." + playerId + ".send", prefs.canSend);
            try {
                userData.save(userDataFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save user preferences: " + e.getMessage());
            }
        }
    }
    
    private void loadUserData() {
        if (userDataFile.exists()) {
            try {
                userData.load(userDataFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load user data: " + e.getMessage());
            }
        }
    }
    
    public void saveAllUsers() {
        for (UUID playerId : userPreferences.keySet()) {
            saveUserPreferences(playerId);
        }
    }
    
    private static class UserPreferences {
        boolean canReceive = true;
        boolean canSend = true;
    }
} 