package com.craftcordconnect.commands;

import com.craftcordconnect.CraftCordConnect;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CraftCordToggleCommand implements CommandExecutor, TabCompleter {
    private final CraftCordConnect plugin;

    public CraftCordToggleCommand(CraftCordConnect plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("discordrelay.toggle")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        if (args.length == 0) {
            sendStatus(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "receive":
                plugin.getUserManager().setCanReceiveDiscordMessages(player, !plugin.getUserManager().canReceiveDiscordMessages(player));
                player.sendMessage(ChatColor.GREEN + "Toggled receiving Discord messages.");
                break;
            case "send":
                plugin.getUserManager().setCanSendDiscordMessages(player, !plugin.getUserManager().canSendDiscordMessages(player));
                player.sendMessage(ChatColor.GREEN + "Toggled sending messages to Discord.");
                break;
            case "all":
                plugin.getUserManager().setCanReceiveDiscordMessages(player, !plugin.getUserManager().canReceiveDiscordMessages(player));
                plugin.getUserManager().setCanSendDiscordMessages(player, !plugin.getUserManager().canSendDiscordMessages(player));
                player.sendMessage(ChatColor.GREEN + "Toggled both sending and receiving Discord messages.");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Usage: /craftcordtoggle <receive|send|all>");
                break;
        }
        return true;
    }

    private void sendStatus(Player player) {
        boolean canReceive = plugin.getUserManager().canReceiveDiscordMessages(player);
        boolean canSend = plugin.getUserManager().canSendDiscordMessages(player);
        player.sendMessage(ChatColor.GOLD + "=== CraftCord Toggle Settings ===");
        player.sendMessage(ChatColor.YELLOW + "Receive Discord messages: " + (canReceive ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        player.sendMessage(ChatColor.YELLOW + "Send messages to Discord: " + (canSend ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        player.sendMessage(ChatColor.GRAY + "Use /craftcordtoggle <receive|send|all> to change settings");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("receive", "send", "all");
        }
        return new ArrayList<>();
    }
} 