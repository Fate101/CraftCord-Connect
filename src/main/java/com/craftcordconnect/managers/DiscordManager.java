package com.craftcordconnect.managers;

import com.craftcordconnect.CraftCordConnect;
import com.craftcordconnect.utils.ConfigManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.EmbedBuilder;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.concurrent.*;
import java.awt.Color;
import java.net.URI;

public class DiscordManager {
    
    private final CraftCordConnect plugin;
    private final ConfigManager configManager;
    private JDA jda;
    
    // Cache for player dominant colors
    private final ConcurrentHashMap<String, Color> avatarColorCache = new ConcurrentHashMap<>();
    private final ExecutorService colorExecutor = Executors.newCachedThreadPool();
    
    public DiscordManager(CraftCordConnect plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }
    
    public void setJda(JDA jda) {
        this.jda = jda;
    }
    
    public void sendMessageToDiscord(Player player, String message) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Attempting to send message to Discord from " + player.getName() + ": " + message);
        }

        if (jda == null) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().warning("JDA is null, cannot send message");
            }
            return;
        }

        if (!configManager.isRelayEnabled()) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Relay is disabled, not sending message");
            }
            return;
        }

        // Check if message should be ignored
        if (shouldIgnoreMessage(message)) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Message ignored due to filter: " + message);
            }
            return;
        }

        // Check if player can send messages
        if (!plugin.getUserManager().canSendDiscordMessages(player)) {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Player " + player.getName() + " has Discord sending disabled");
            }
            return;
        }

        // Truncate if too long
        if (message.length() > configManager.getMaxMessageLength()) {
            message = message.substring(0, configManager.getMaxMessageLength() - 3) + "...";
        }

        sendMessageAsEmbed(player, message);
    }

    private void sendMessageAsEmbed(Player player, String message) {
        if (configManager.isDynamicEmbedColorEnabled()) {
            // Async dominant color fetch
            colorExecutor.submit(() -> {
                Color color = getDominantColorForPlayer(player);
                sendEmbedWithColor(player, message, color);
            });
        } else {
            sendEmbedWithColor(player, message, configManager.getEmbedColor());
        }
    }

    private void sendEmbedWithColor(Player player, String message, Color color) {
        try {
            TextChannel channel = jda.getTextChannelById(configManager.getChannelId());
            if (channel == null) {
                plugin.getLogger().warning("Could not find Discord channel with ID: " + configManager.getChannelId());
                return;
            }
            EmbedBuilder embed = new EmbedBuilder();
            embed.setAuthor(player.getName(), null, "https://mc-heads.net/avatar/" + player.getUniqueId() + "/128.png");
            embed.setDescription(message);
            embed.setColor(color);
            channel.sendMessageEmbeds(embed.build())
                .queue(
                    success -> {
                        if (configManager.isDebugEnabled()) {
                            plugin.getLogger().info("Embed message sent successfully to Discord");
                        }
                    },
                    error -> plugin.getLogger().warning("Failed to send embed message to Discord: " + error.getMessage())
                );
        } catch (Exception e) {
            plugin.getLogger().severe("Exception while sending embed message to Discord: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Color getDominantColorForPlayer(Player player) {
        String uuid = player.getUniqueId().toString();
        if (avatarColorCache.containsKey(uuid)) {
            return avatarColorCache.get(uuid);
        }
        try {
            String url = "https://mc-heads.net/avatar/" + uuid + "/128.png";
            BufferedImage image = ImageIO.read(URI.create(url).toURL());
            if (image == null) return configManager.getEmbedColor();
            Color dominant = getDominantColor(image);
            avatarColorCache.put(uuid, dominant);
            return dominant;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to fetch or process avatar for color: " + e.getMessage());
            return configManager.getEmbedColor();
        }
    }

    // Simple average color algorithm
    private Color getDominantColor(BufferedImage image) {
        long sumR = 0, sumG = 0, sumB = 0, count = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, true);
                if (color.getAlpha() < 128) continue; // skip transparent
                sumR += color.getRed();
                sumG += color.getGreen();
                sumB += color.getBlue();
                count++;
            }
        }
        if (count == 0) return configManager.getEmbedColor();
        return new Color((int)(sumR/count), (int)(sumG/count), (int)(sumB/count));
    }
    
    public void sendMessageToMinecraft(String author, String message) {
        if (!configManager.isRelayEnabled()) {
            return;
        }
        
        // Format message
        String formattedMessage = configManager.getMinecraftFormat()
                .replace("{author}", author)
                .replace("{message}", message);
        
        // Apply color codes
        formattedMessage = ChatColor.translateAlternateColorCodes('&', formattedMessage);
        
        // Send to all online players who can receive Discord messages
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getUserManager().canReceiveDiscordMessages(player)) {
                player.sendMessage(formattedMessage);
            }
        }
    }
    
    public void sendServerStatus(String status) {
        if (jda == null || !configManager.isServerStatusEnabled()) {
            return;
        }
        
        sendServerStatusViaBot(status);
    }
    
    public void sendServerStatusSync(String status) {
        if (jda == null || !configManager.isServerStatusEnabled()) {
            return;
        }
        
        sendServerStatusViaBotSync(status);
    }
    
    private void sendServerStatusViaBot(String status) {
        try {
            TextChannel channel = jda.getTextChannelById(configManager.getChannelId());
            if (channel != null) {
                channel.sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .setContent("**Server Status**: " + status)
                    .build())
                    .queue();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send server status to Discord: " + e.getMessage());
        }
    }
    
    private void sendServerStatusViaBotSync(String status) {
        try {
            TextChannel channel = jda.getTextChannelById(configManager.getChannelId());
            if (channel != null) {
                channel.sendMessage(new net.dv8tion.jda.api.utils.messages.MessageCreateBuilder()
                    .setContent("**Server Status**: " + status)
                    .build())
                    .complete(); // Use complete() instead of queue() for synchronous execution
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send server status to Discord: " + e.getMessage());
        }
    }
    
    private boolean shouldIgnoreMessage(String message) {
        List<String> ignoredPatterns = configManager.getIgnoredMessages();
        
        for (String pattern : ignoredPatterns) {
            if (Pattern.matches(pattern, message)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isConnected() {
        return jda != null && jda.getStatus() == net.dv8tion.jda.api.JDA.Status.CONNECTED;
    }

    // Removed isWebhookReady and getWebhook as webhooks are no longer used
} 