package com.craftcordconnect.utils;

import com.craftcordconnect.CraftCordConnect;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    
    private final CraftCordConnect plugin;
    private FileConfiguration config;
    
    public ConfigManager(CraftCordConnect plugin) {
        this.plugin = plugin;
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    public String getBotToken() {
        return config.getString("discord.bot-token", "YOUR_BOT_TOKEN_HERE");
    }
    
    public String getChannelId() {
        return config.getString("discord.channel-id", "YOUR_CHANNEL_ID_HERE");
    }
    
    public String getStatusMessage() {
        return config.getString("discord.status-message", "Minecraft Server Online");
    }
    
    public boolean isRelayEnabled() {
        return config.getBoolean("relay.enabled", true);
    }
    
    public boolean useWebhooks() {
        return config.getBoolean("relay.use-webhooks", true);
    }
    
    public String getDiscordFormat() {
        return config.getString("relay.discord-format", "**{player}**: {message}");
    }
    
    public String getMinecraftFormat() {
        return config.getString("relay.minecraft-format", "&b[Discord] &f{author}: {message}");
    }
    
    public int getMaxMessageLength() {
        return config.getInt("relay.max-message-length", 1900);
    }
    
    public boolean isServerStatusEnabled() {
        return config.getBoolean("relay.server-status", true);
    }
    
    public boolean isSystemEventEnabled(String event) {
        return config.getBoolean("relay.system-events." + event, true);
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("relay.debug", false);
    }
    
    public boolean useAvatars() {
        return config.getBoolean("relay.use-avatars", true);
    }
    
    public boolean isDynamicEmbedColorEnabled() {
        return config.getBoolean("relay.dynamic-embed-color", true);
    }
    
    public java.awt.Color getEmbedColor() {
        String hex = config.getString("relay.embed-color", "#00BFFF");
        try {
            if (hex.startsWith("#")) {
                return java.awt.Color.decode(hex);
            } else {
                return java.awt.Color.decode("#" + hex);
            }
        } catch (Exception e) {
            return java.awt.Color.decode("#00BFFF");
        }
    }
    
    public List<String> getIgnoredMessages() {
        return config.getStringList("relay.ignored-messages");
    }
    
    public boolean getDefaultReceive() {
        return config.getBoolean("users.default-receive", true);
    }
    
    public boolean getDefaultSend() {
        return config.getBoolean("users.default-send", true);
    }
    
    public boolean shouldSavePreferences() {
        return config.getBoolean("users.save-preferences", true);
    }
    
    public boolean isUseDiscordNickname() {
        return config.getBoolean("relay.use-discord-nickname", true);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
} 