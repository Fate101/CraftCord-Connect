package com.craftcordconnect.commands;

import com.craftcordconnect.CraftCordConnect;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CraftCordCommand implements CommandExecutor, TabCompleter {
    
    private final CraftCordConnect plugin;
    
    public CraftCordCommand(CraftCordConnect plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("discordrelay.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.getConfigManager().loadConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
                break;
                
            case "status":
                sendStatusMessage(sender);
                break;
                
            case "toggle":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /craftcord toggle <on|off>");
                    return true;
                }
                
                boolean enabled = args[1].equalsIgnoreCase("on");
                plugin.getConfigManager().getConfig().set("relay.enabled", enabled);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "CraftCord relay " + (enabled ? "enabled" : "disabled") + "!");
                break;
                
            default:
                sendHelpMessage(sender);
                break;
        }
        
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== CraftCord Connect Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/craftcord reload " + ChatColor.WHITE + "- Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/craftcord status " + ChatColor.WHITE + "- Show connection status");
        sender.sendMessage(ChatColor.YELLOW + "/craftcord toggle <on|off> " + ChatColor.WHITE + "- Enable/disable relay");
    }
    
    private void sendStatusMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== CraftCord Connect Status ===");
        sender.sendMessage(ChatColor.YELLOW + "Discord Connection: " + 
            (plugin.getDiscordManager().isConnected() ? ChatColor.GREEN + "Connected" : ChatColor.RED + "Disconnected"));
        sender.sendMessage(ChatColor.YELLOW + "Relay Enabled: " + 
            (plugin.getConfigManager().isRelayEnabled() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        sender.sendMessage(ChatColor.YELLOW + "Channel ID: " + ChatColor.WHITE + plugin.getConfigManager().getChannelId());
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "status", "toggle");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            return Arrays.asList("on", "off");
        }
        return new ArrayList<>();
    }
} 