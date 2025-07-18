package com.craftcordconnect;

import com.craftcordconnect.commands.CraftCordCommand;
import com.craftcordconnect.commands.CraftCordToggleCommand;
import com.craftcordconnect.listeners.ChatListener;
import com.craftcordconnect.listeners.DiscordListener;
import com.craftcordconnect.listeners.ServerListener;
import com.craftcordconnect.managers.DiscordManager;
import com.craftcordconnect.managers.UserManager;
import com.craftcordconnect.utils.ConfigManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.plugin.java.JavaPlugin;

public class CraftCordConnect extends JavaPlugin {
    
    private static CraftCordConnect instance;
    private DiscordManager discordManager;
    private UserManager userManager;
    private ConfigManager configManager;
    private JDA jda;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        configManager = new ConfigManager(this);
        userManager = new UserManager(this);
        discordManager = new DiscordManager(this);
        
        // Load configuration
        configManager.loadConfig();
        
        // Register commands
        getCommand("craftcord").setExecutor(new CraftCordCommand(this));
        getCommand("craftcordtoggle").setExecutor(new CraftCordToggleCommand(this));
        // getCommand("viewmap").setExecutor(new CraftCordCommand(this)); // Removed from plugin.yml for hidden command

        // Removed dynamic registration of /viewmap, now handled as a subcommand of /craftcord
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerListener(this), this);
        
        // Initialize Discord bot
        initializeDiscordBot();
        
        getLogger().info("CraftCord-Connect has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Send shutdown notification synchronously to avoid zip file errors
        if (discordManager != null && configManager != null && 
            configManager.isServerStatusEnabled() && 
            configManager.isSystemEventEnabled("server-stop")) {
            try {
                discordManager.sendServerStatusSync("🔴 Server is shutting down!");
            } catch (Exception e) {
                getLogger().warning("Failed to send shutdown notification to Discord: " + e.getMessage());
            }
        }
        
        // Shutdown JDA gracefully
        if (jda != null) {
            try {
                jda.shutdown();
                jda.awaitShutdown(java.time.Duration.ofSeconds(5)); // Wait up to 5 seconds for graceful shutdown
            } catch (Exception e) {
                getLogger().warning("Error during JDA shutdown: " + e.getMessage());
            }
        }
        
        // Save user preferences
        if (userManager != null) {
            try {
                userManager.saveAllUsers();
            } catch (Exception e) {
                getLogger().warning("Failed to save user preferences: " + e.getMessage());
            }
        }
        
        getLogger().info("CraftCord-Connect has been disabled!");
    }
    
    private void initializeDiscordBot() {
        String botToken = configManager.getBotToken();
        String channelId = configManager.getChannelId();
        
        if (botToken.equals("YOUR_BOT_TOKEN_HERE") || channelId.equals("YOUR_CHANNEL_ID_HERE")) {
            getLogger().warning("Please configure your Discord bot token and channel ID in config.yml!");
            return;
        }
        
        try {
            jda = JDABuilder.createDefault(botToken)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .setActivity(Activity.playing(configManager.getStatusMessage()))
                    .addEventListeners(new DiscordListener(this))
                    .build();
            
            jda.awaitReady();
            discordManager.setJda(jda);
            
            getLogger().info("Discord bot connected successfully!");
            
        } catch (Exception e) {
            getLogger().severe("Failed to connect to Discord: " + e.getMessage());
        }
    }
    
    public static CraftCordConnect getInstance() {
        return instance;
    }
    
    public DiscordManager getDiscordManager() {
        return discordManager;
    }
    
    public UserManager getUserManager() {
        return userManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public JDA getJda() {
        return jda;
    }
} 