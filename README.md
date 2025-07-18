# CraftCord-Connect

A Minecraft 1.21.7 plugin that enables bidirectional communication between Minecraft chat and Discord channels.

> **AI Disclosure**: This plugin was created with assistance from AI tools. The code structure, features, and implementation were developed collaboratively with AI assistance to ensure best practices and comprehensive functionality.

## Features

- **Bidirectional Chat**: Messages from Minecraft are sent to Discord and vice versa
- **Self-Contained**: No separate software required - everything runs within the plugin
- **User Control**: Players can disable receiving or sending Discord messages individually
- **No Extra Ports**: Uses Discord's API, no additional ports need to be opened
- **Configurable**: Customizable message formats and settings
- **Server Status**: Automatic notifications when players join/leave
- **Player Avatars in Embeds**: Minecraft player avatars are shown in Discord embeds
- **System Events**: Server start/stop notifications
- **Debug Mode**: Comprehensive logging for troubleshooting

## Requirements

- Minecraft Server 1.21.7 (Spigot/Paper)
- Java 17 or higher
- Discord Bot Token

## Setup Instructions

### 1. Create a Discord Bot

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Click "New Application" and give it a name
3. Go to the "Bot" section and click "Add Bot"
4. Copy the bot token (you'll need this later)
5. Enable the following bot permissions:
   - Send Messages
   - Read Message History
   - Use Slash Commands
6. Go to "OAuth2" → "URL Generator"
7. Select "bot" scope and the permissions above
8. Use the generated URL to invite the bot to your server

### 2. Get Channel ID

1. Enable Developer Mode in Discord (User Settings → Advanced → Developer Mode)
2. Right-click on the channel where you want messages to be relayed
3. Click "Copy ID" - this is your channel ID

### 3. Install the Plugin

1. Download the compiled JAR file
2. Place it in your server's `plugins` folder
3. Start/restart your server
4. The plugin will create a `config.yml` file

### 4. Configure the Plugin

Edit `plugins/CraftCordConnect/config.yml`:

```yaml
discord:
  bot-token: "YOUR_BOT_TOKEN_HERE"
  channel-id: "YOUR_CHANNEL_ID_HERE"
  status-message: "Minecraft Server Online"

relay:
  enabled: true
  # Enable dynamic embed color based on the player's avatar (may impact performance, see below)
  dynamic-embed-color: true
  # Embed color to use when dynamic-embed-color is false (hex code, e.g. "#00BFFF"). If invalid, defaults to #00BFFF
  embed-color: "#00BFFF"
  # Use Discord display name/nickname instead of username when relaying messages to Minecraft
  use-discord-nickname: true
  discord-format: "**{player}**: {message}"
  minecraft-format: "&b[Discord] &f{author}: {message}"
  max-message-length: 1900
  server-status: true
  debug: false                # Enable for troubleshooting
  system-events:
    server-start: true
    server-stop: true
    player-join: true
    player-leave: true
```

Replace `YOUR_BOT_TOKEN_HERE` and `YOUR_CHANNEL_ID_HERE` with your actual values.

### 5. Restart the Server

After configuring, restart your server. The plugin should connect to Discord automatically.

## Commands

### Admin Commands

- `/craftcord reload` - Reload the configuration
- `/craftcord status` - Show connection status
- `/craftcord toggle <on|off>` - Enable/disable the relay

### User Commands

- `/craftcordtoggle` - Show your current relay settings
- `/craftcordtoggle receive` - Toggle receiving Discord messages
- `/craftcordtoggle send` - Toggle sending messages to Discord
- `/craftcordtoggle all` - Toggle both receive and send

## Permissions

- `craftcord.admin` - Access to admin commands (default: op)
- `craftcord.player` - Grants all standard player permissions (toggle, send, receive) (default: true)
  - `craftcord.toggle` - Allow users to toggle relay settings (default: true)
  - `craftcord.send` - Allow sending messages to Discord (default: true)
  - `craftcord.receive` - Allow receiving messages from Discord (default: true)

## Configuration Options

### Discord Settings
- `bot-token`: Your Discord bot token
- `channel-id`: Discord channel ID for relay
- `status-message`: Bot's status message

### Relay Settings
- `enabled`: Enable/disable the relay
- `dynamic-embed-color`: If true, the embed color on Discord will match the dominant color of the player's Minecraft avatar. This is done asynchronously and cached per player, but may add a small delay for the first message from each player. Set to false to use a default color for all embeds.
- `embed-color`: The hex code (e.g. `#00BFFF`) for the embed color when `dynamic-embed-color` is false, or as a fallback if color extraction fails. If the value is invalid or missing, the plugin will use `#00BFFF` as a safe default.
- `use-discord-nickname`: If true (default), the plugin will use the Discord display name/nickname when relaying messages from Discord to Minecraft. If false, it will use the Discord username instead.
- `discord-format`: Format for messages sent to Discord
- `minecraft-format`: Format for messages received from Discord
- `max-message-length`: Maximum message length (Discord limit: 2000)
- `server-status`: Enable server status notifications
- `debug`: Enable detailed logging for troubleshooting
- `system-events`: Configure which system events to notify about
- `ignored-messages`: Regex patterns for messages to ignore

### User Settings
- `default-receive`: Default setting for receiving Discord messages
- `default-send`: Default setting for sending to Discord
- `save-preferences`: Save user preferences to file

## Building from Source

1. Clone the repository
2. Install Maven
3. Run `mvn clean package`
4. The JAR file will be in the `target` folder

## Troubleshooting

### Bot Not Connecting
- Double-check your Discord bot token in `config.yml`.
- Ensure your bot has the following permissions in your Discord server:
  - Send Messages
  - Read Message History
  - Use Slash Commands
- Make sure the bot is invited to the correct server and channel.
- Check the server console/logs for any error messages on startup.

### Messages Not Relaying
- Verify the channel ID in `config.yml` matches your Discord channel.
- Ensure the relay is enabled in the config (`relay.enabled: true`).
- Check that users have the correct permissions (`craftcord.player` or its children).
- Enable debug mode (`debug: true`) for more detailed logs.

### Avatar/Embed Color Issues
- If you experience delays, try disabling dynamic embed color (`dynamic-embed-color: false`).
- Some avatar services may be rate-limited or temporarily unavailable.
- Enable debug mode to see detailed logs about avatar fetching and embed color.

### Plugin Not Loading
- Ensure you are running Java 17 or higher.
- Check for errors in the server console/logs.
- Make sure the plugin JAR is in the correct `plugins` folder.
- Verify the plugin is compatible with your Minecraft server version.

### General Tips
- Use `/craftcord reload` to reload the configuration after making changes.
- If you encounter persistent issues, enable debug mode and review the logs for more information.

## Support

If you encounter issues:
1. Check the server console for error messages
2. Enable debug mode (`debug: true`) for detailed logging
3. Verify your Discord bot configuration
4. Ensure all permissions are set correctly

## AI Development Disclosure

This plugin was built with a lot of help from AI tools (and a bit of human creativity). The code, features, and documentation were all shaped by a mix of AI suggestions and hands-on tinkering. If you spot something clever, it might have been the AI—or maybe just a lucky guess!

Feel free to fork, improve, or just enjoy the plugin. If you have ideas or want to contribute, jump in!

## License

This project is open source. Feel free to modify and distribute according to your needs. 