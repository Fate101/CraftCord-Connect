package com.craftcordconnect.listeners;

import com.craftcordconnect.CraftCordConnect;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import javax.annotation.Nonnull;
import java.util.stream.Collectors;

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
        boolean relayMedia = plugin.getConfigManager().getConfig().getBoolean("relay.relay-links-and-media", true);
        StringBuilder extra = new StringBuilder();
        if (relayMedia) {
            // Attachments (images/files)
            var attachments = event.getMessage().getAttachments();
            for (var att : attachments) {
                if (att.isImage()) {
                    extra.append(" [Image] ").append(att.getUrl());
                } else {
                    extra.append(" [File] ").append(att.getUrl());
                }
            }
            // Stickers
            var stickers = event.getMessage().getStickers();
            for (var sticker : stickers) {
                String url = sticker.getIconUrl();
                if (url != null) {
                    extra.append(" [Sticker] ").append(url);
                } else {
                    extra.append(" [Sticker] ").append(sticker.getName());
                }
            }
        }
        // Ignore empty messages unless there is media
        if (message.trim().isEmpty() && extra.length() == 0) {
            return;
        }
        
        // Relay message to Minecraft
        String author;
        String authorColorHex = null;
        var member = event.getMember();
        if (plugin.getConfigManager().isUseDiscordNickname() && member != null) {
            author = member.getEffectiveName();
        } else {
            author = event.getAuthor().getName();
        }
        if (plugin.getConfigManager().isUseDiscordRoleColor() && member != null && member.getColor() != null) {
            java.awt.Color color = member.getColor();
            authorColorHex = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        }
        // Run on main thread since we're modifying the world
        String relayMsg = message + extra;
        String finalAuthor = author;
        String finalAuthorColorHex = authorColorHex;
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getDiscordManager().sendMessageToMinecraft(finalAuthor, relayMsg.trim(), finalAuthorColorHex);
        });
    }
} 