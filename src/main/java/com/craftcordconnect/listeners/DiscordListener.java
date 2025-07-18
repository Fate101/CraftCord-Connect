package com.craftcordconnect.listeners;

import com.craftcordconnect.CraftCordConnect;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import javax.annotation.Nonnull;

public class DiscordListener extends ListenerAdapter {
    
    private final CraftCordConnect plugin;
    
    public DiscordListener(CraftCordConnect plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        // Ignore bot messages
        if (event.getAuthor().isBot()) {
            return;
        }
        
        // Check if message is from the configured channel
        if (!event.getChannel().getId().equals(plugin.getConfigManager().getChannelId())) {
            return;
        }
        
        // Get message content
        String message = event.getMessage().getContentDisplay();
        
        // Ignore empty messages
        if (message.trim().isEmpty()) {
            return;
        }
        
        // Relay message to Minecraft
        String author;
        var member = event.getMember();
        if (plugin.getConfigManager().isUseDiscordNickname() && member != null) {
            author = member.getEffectiveName();
        } else {
            author = event.getAuthor().getName();
        }
        
        // Run on main thread since we're modifying the world
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getDiscordManager().sendMessageToMinecraft(author, message);
        });
    }
} 