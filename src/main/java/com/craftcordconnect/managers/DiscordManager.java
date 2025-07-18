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
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.Material;
import org.bukkit.map.MapView;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import java.net.URL;

public class DiscordManager {
    
    private final CraftCordConnect plugin;
    private final ConfigManager configManager;
    private JDA jda;
    
    // Cache for player dominant colors
    private final ConcurrentHashMap<String, Color> avatarColorCache = new ConcurrentHashMap<>();
    private final ExecutorService colorExecutor = Executors.newCachedThreadPool();
    
    // Track temporary maps for cleanup
    public final ConcurrentHashMap<UUID, List<MapView>> tempMaps = new ConcurrentHashMap<>();

    // Server-side mapping for viewmap codes
    private final Map<String, String> urlMap = new ConcurrentHashMap<>();

    // Track previous main hand items and their slots for map restore
    private final Map<UUID, ItemStack> previousMainHandItems = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> previousMainHandSlots = new ConcurrentHashMap<>();

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
    
    public void sendMessageToMinecraft(String author, String message, String authorColorHex) {
        if (!configManager.isRelayEnabled()) {
            return;
        }
        String coloredAuthor = author;
        if (authorColorHex != null && !authorColorHex.isEmpty()) {
            coloredAuthor = toMinecraftHexColor(authorColorHex) + author + ChatColor.RESET;
        }
        // Format message
        String formattedMessage = configManager.getMinecraftFormat()
                .replace("{author}", coloredAuthor)
                .replace("{message}", message);
        // Apply color codes
        formattedMessage = ChatColor.translateAlternateColorCodes('&', formattedMessage);
        // Patterns
        java.util.regex.Pattern imageOrFileOrStickerPattern = java.util.regex.Pattern.compile("\\[(Image|File|Sticker)] (https?://\\S+)");
        java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile("(?<!\\[Image] |\\[File] |\\[Sticker] )(https?://\\S+)");
        for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
            if (plugin.getUserManager().canReceiveDiscordMessages(player)) {
                java.util.regex.Matcher matcher1 = imageOrFileOrStickerPattern.matcher(formattedMessage);
                java.util.regex.Matcher matcher2 = urlPattern.matcher(formattedMessage);
                if (!matcher1.find() && !matcher2.find()) {
                    player.sendMessage(formattedMessage);
                    continue;
                }
                matcher1.reset();
                matcher2.reset();
                net.md_5.bungee.api.chat.TextComponent fullMsg = new net.md_5.bungee.api.chat.TextComponent("");
                int lastEnd = 0;
                java.util.List<java.util.regex.MatchResult> matches = new java.util.ArrayList<>();
                while (matcher1.find()) matches.add(matcher1.toMatchResult());
                while (matcher2.find()) matches.add(matcher2.toMatchResult());
                matches.sort(java.util.Comparator.comparingInt(java.util.regex.MatchResult::start));
                for (java.util.regex.MatchResult match : matches) {
                    String before = formattedMessage.substring(lastEnd, match.start());
                    if (!before.isEmpty()) {
                        fullMsg.addExtra(new net.md_5.bungee.api.chat.TextComponent(before));
                    }
                    String matchText = match.group();
                    if (matchText.startsWith("[Image] ") || matchText.startsWith("[Sticker] ") || matchText.startsWith("[File] ")) {
                        String type = matchText.startsWith("[Image] ") ? "Image" : (matchText.startsWith("[Sticker] ") ? "Sticker" : "File");
                        String url = match.group(2);
                        if (type.equals("Image")) {
                            String code = generateShortCode();
                            urlMap.put(code, url);
                            net.md_5.bungee.api.chat.TextComponent viewButton = new net.md_5.bungee.api.chat.TextComponent("[View Image]");
                            viewButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                            viewButton.setUnderlined(true);
                            viewButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/craftcord viewmap image " + code));
                            net.md_5.bungee.api.chat.TextComponent linkButton = new net.md_5.bungee.api.chat.TextComponent("[Image Link]");
                            linkButton.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                            linkButton.setUnderlined(true);
                            linkButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                                net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, url));
                            fullMsg.addExtra(viewButton);
                            fullMsg.addExtra(new net.md_5.bungee.api.chat.TextComponent(" "));
                            fullMsg.addExtra(linkButton);
                        } else if (type.equals("Sticker")) {
                            String code = generateShortCode();
                            urlMap.put(code, url);
                            net.md_5.bungee.api.chat.TextComponent viewButton = new net.md_5.bungee.api.chat.TextComponent("[View Sticker]");
                            viewButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                            viewButton.setUnderlined(true);
                            viewButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/craftcord viewmap sticker " + code));
                            net.md_5.bungee.api.chat.TextComponent linkButton = new net.md_5.bungee.api.chat.TextComponent("[Sticker Link]");
                            linkButton.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                            linkButton.setUnderlined(true);
                            linkButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                                net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, url));
                            fullMsg.addExtra(viewButton);
                            fullMsg.addExtra(new net.md_5.bungee.api.chat.TextComponent(" "));
                            fullMsg.addExtra(linkButton);
                        } else if (type.equals("File")) {
                            net.md_5.bungee.api.chat.TextComponent linkButton = new net.md_5.bungee.api.chat.TextComponent("[File Link]");
                            linkButton.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                            linkButton.setUnderlined(true);
                            linkButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                                net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, url));
                            fullMsg.addExtra(linkButton);
                        }
                    } else {
                        String url = match.group(1) != null ? match.group(1) : matchText;
                        net.md_5.bungee.api.chat.TextComponent button = new net.md_5.bungee.api.chat.TextComponent("[Link]");
                        button.setColor(net.md_5.bungee.api.ChatColor.AQUA);
                        button.setUnderlined(true);
                        button.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                            net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, url));
                        fullMsg.addExtra(button);
                    }
                    lastEnd = match.end();
                }
                if (lastEnd < formattedMessage.length()) {
                    fullMsg.addExtra(new net.md_5.bungee.api.chat.TextComponent(formattedMessage.substring(lastEnd)));
                }
                player.spigot().sendMessage(fullMsg);
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
                EmbedBuilder embed = new EmbedBuilder();
                embed.setAuthor(configManager.getServerEmbedName(), null, "https://mc-heads.net/avatar/00000000-0000-0000-0000-000000000000/128.png");
                embed.setDescription(status);
                if (status.contains("🟢")) {
                    embed.setColor(Color.GREEN);
                } else if (status.contains("🔴")) {
                    embed.setColor(Color.RED);
                } else {
                    embed.setColor(configManager.getEmbedColor());
                }
                channel.sendMessageEmbeds(embed.build()).queue();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send server status to Discord: " + e.getMessage());
        }
    }
    
    private void sendServerStatusViaBotSync(String status) {
        try {
            TextChannel channel = jda.getTextChannelById(configManager.getChannelId());
            if (channel != null) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setAuthor(configManager.getServerEmbedName(), null, "https://mc-heads.net/avatar/00000000-0000-0000-0000-000000000000/128.png");
                embed.setDescription(status);
                if (status.contains("🟢")) {
                    embed.setColor(Color.GREEN);
                } else if (status.contains("🔴")) {
                    embed.setColor(Color.RED);
                } else {
                    embed.setColor(configManager.getEmbedColor());
                }
                channel.sendMessageEmbeds(embed.build()).complete();
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

    // Call this when a player clicks an [Image Link] or [Sticker Link] button
    public void handleImageMapRequest(Player player, String url, String type) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                BufferedImage image = ImageIO.read(new URL(url));
                if (image == null) {
                    player.sendMessage("§cFailed to load image for map.");
                    return;
                }
                // Scale and center image to fit 128x128, preserving aspect ratio
                int targetSize = 128;
                int imgW = image.getWidth();
                int imgH = image.getHeight();
                double scale = Math.min((double)targetSize / imgW, (double)targetSize / imgH);
                int newW = (int)(imgW * scale);
                int newH = (int)(imgH * scale);
                BufferedImage resized = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g = resized.createGraphics();
                g.setColor(java.awt.Color.BLACK);
                g.fillRect(0, 0, targetSize, targetSize);
                int x = (targetSize - newW) / 2;
                int y = (targetSize - newH) / 2;
                g.drawImage(image, x, y, newW, newH, null);
                g.dispose();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    MapView mapView = Bukkit.createMap(player.getWorld());
                    mapView.getRenderers().clear();
                    mapView.addRenderer(new MapRenderer() {
                        private boolean rendered = false;
                        @Override
                        public void render(MapView view, MapCanvas canvas, Player p) {
                            if (!rendered) {
                                canvas.drawImage(0, 0, resized);
                                rendered = true;
                            }
                        }
                    });
                    ItemStack mapItem = new ItemStack(Material.FILLED_MAP, 1);
                    MapMeta meta = (MapMeta) mapItem.getItemMeta();
                    meta.setMapView(mapView);
                    mapItem.setItemMeta(meta);
                    // Store previous main hand item and its slot, then give map in hand
                    UUID uuid = player.getUniqueId();
                    ItemStack prev = player.getInventory().getItemInMainHand();
                    int currentSlot = player.getInventory().getHeldItemSlot();
                    
                    // Only store the item if it's not null and not air
                    if (prev != null && prev.getType() != Material.AIR) {
                        previousMainHandItems.put(uuid, prev.clone());
                        previousMainHandSlots.put(uuid, currentSlot);
                    } else {
                        // If no item to restore, still track the slot but don't store null item
                        previousMainHandSlots.put(uuid, currentSlot);
                    }
                    
                    player.getInventory().setItemInMainHand(mapItem);
                    // Track for cleanup
                    tempMaps.computeIfAbsent(uuid, k -> Collections.synchronizedList(new ArrayList<>())).add(mapView);
                    // Schedule removal after 30 seconds
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        restorePreviousMainHandItem(player);
                        // Optionally, remove mapView from server (advanced: MapView API cleanup)
                    }, 20 * 30);
                    player.sendMessage("§aYou received a temporary map for this " + type.toLowerCase() + ". It will be removed soon!");
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage("§cFailed to load image for map: " + e.getMessage()));
            }
        });
    }

    private String generateShortCode() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    public String getUrlForCode(String code) {
        return urlMap.get(code); // Do not remove, so it persists until server restarts
    }

    // Removed isWebhookReady and getWebhook as webhooks are no longer used

    // Restore previous main hand item for a player to its original slot
    public void restorePreviousMainHandItem(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack prev = previousMainHandItems.remove(uuid);
        Integer originalSlot = previousMainHandSlots.remove(uuid);
        
        if (prev != null && prev.getType() != Material.AIR && originalSlot != null) {
            // Check if the original slot is empty or contains the map
            ItemStack currentInSlot = player.getInventory().getItem(originalSlot);
            if (currentInSlot == null || currentInSlot.getType() == Material.AIR || 
                (currentInSlot.getType() == Material.FILLED_MAP && currentInSlot.hasItemMeta())) {
                // Slot is empty or contains our map, safe to restore
                player.getInventory().setItem(originalSlot, prev);
            } else {
                // Slot is occupied by something else, try to add to inventory
                player.getInventory().addItem(prev);
            }
        } else if (originalSlot != null) {
            // No item to restore, but we should still clear the map from the slot
            ItemStack currentInSlot = player.getInventory().getItem(originalSlot);
            if (currentInSlot != null && currentInSlot.getType() == Material.FILLED_MAP && currentInSlot.hasItemMeta()) {
                player.getInventory().setItem(originalSlot, null);
            }
        }
    }

    // Utility to convert #RRGGBB to §x§R§R§G§G§B§B
    public static String toMinecraftHexColor(String hex) {
        hex = hex.replace("#", "");
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }
} 