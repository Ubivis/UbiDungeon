# AI Dungeon Generator

![Version](https://img.shields.io/badge/version-0.1.1--Alpha-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-1.19.4-green)
![Spigot](https://img.shields.io/badge/Spigot-compatible-orange)

An advanced procedural dungeon generation plugin for Minecraft servers that uses AI-inspired algorithms to create unique, theme-based dungeons complete with quests, traps, and special mobs.

## 🏰 Features

- **Procedural Dungeon Generation**: Create unique dungeons using cellular automata, Markov chains, and genetic algorithms
- **Biome-Specific Themes**: Each biome generates dungeons with distinct aesthetics and challenges
- **Dynamic Quest System**: Assigns quests to players entering dungeons with progress tracking and rewards
- **Advanced Trap System**: Various traps including arrow traps, pit falls, teleporters, and the dreaded Warden summoning
- **Elite Mobs**: Challenging enhanced mobs with special abilities and better loot
- **Multilingual Support**: Currently supports English and German

## 📥 Installation

1. Download the plugin JAR from [Hangar](https://hangar.papermc.io/Ubivis/AI_Dungeon_Generator) [POLYMART](https://polymart.org/resource/aidungeongenerator.7457) or [modrinth](https://modrinth.com/plugin/ai-dungeon-generator)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. The plugin will generate the default configuration files
5. Customize settings in `config.yml` to match your server's needs

## ⚙️ Configuration

The plugin is highly configurable through the `config.yml` file. Key configuration sections include:

```yaml
# Customize dungeon generation parameters
generation:
  async:
    enabled: true  # Async generation to prevent lag
    max-concurrent-generations: 3
  algorithm:
    dungeon-size:
      small: 25    # Size of small dungeons
      medium: 40   # Size of medium dungeons
      large: 60    # Size of large dungeons

# Configure dungeon discovery
discovery:
  enable-compass: true
  exploration-threshold: 0.1  # 10% of biome must be explored

# Theme-specific settings
themes:
  PYRAMID:
    primary-blocks:
      - SANDSTONE
      - SMOOTH_SANDSTONE
    accent-blocks:
      - GOLD_BLOCK
      - CHISELED_SANDSTONE
    # ... more theme settings

# Configure traps
traps:
  types:
    ARROW:
      enabled: true
      damage: 4.0
    # ... more trap types

# Configure quest system
quests:
  enabled: true
  max_quests_per_player: 5
  # ... more quest settings

```

## 🎮 Commands
### Dungeon Management

- **/aidungeon generate** - Generate a dungeon at your location
- **/aidungeon info** - Show information about nearby dungeons
- **/aidungeon list** - List all generated dungeons
- **/aidungeon tp <id>** - Teleport to a dungeon by ID
- **/aidungeon reload** - Reload the plugin configuration

### Quest Management

- **/quests** - Show your active quests
- **/quests details <id>** - Show detailed quest information
- **/quests track <id>** - Get a compass that tracks this quest
- **/quests claim <id>** - Claim rewards for a completed quest
- **/quests abandon <id>** - Abandon a quest

## 🔒 Permissions
- **aidungeon.admin** - Access to administrative commands (default: op)
- **aidungeon.discover** - Allows players to discover dungeons (default: true)
- **aidungeon.quest** - Allows players to use quest commands (default: true)

## 🧠 How It Works
The plugin generates dungeons using three primary algorithms:

- **Cellular Automata:** Creates the basic dungeon layout with rooms and corridors
Markov Chain Model: Determines room type transitions for a more natural feel
Genetic Optimizer: Refines the dungeon for better playability and aesthetics

The generation process considers the biome type to create theme-appropriate dungeons, with different block types, mob spawns, and trap systems.

### 🌍 Localization
The plugin supports multiple languages through language files in the lang/ folder.
Currently supported languages:

- English
- German

To add a new language, copy the en.yml file, rename it to your language code (e.g., fr.yml), and translate the messages.

## 🔌 Integration

- **PlaceholderAPI:** Use placeholders like %aidungeon_total_dungeons% in other plugins
- **Developer API:** The plugin provides an API for other plugins to interact with

## 📝 Developer API
Example of using the API from another plugin:
```java
// Get the API instance
AIDungeonAPI api = ((AIDungeonGenerator) Bukkit.getPluginManager().getPlugin("AIDungeonGenerator")).getAPI();

// Get a compass to the nearest dungeon
ItemStack compass = api.getNearestDungeonCompass(player);

// Get information about the nearest dungeon
BiomeArea nearestDungeon = api.getNearestDungeon(player);
```

## 🤝 Contributing
Contributions are welcome! Feel free to submit pull requests for:

- **New dungeon themes**
- **Additional trap types**
- **Bug fixes and optimizations**
- **Translations for other languages**

## 📜 License
This project is licensed under the MIT License - see the LICENSE file for details.

## 🗺️ Roadmap

More biome-specific themes
Dungeon progression system
Advanced boss battles
Custom item generation
Enhanced dungeon decorations


**Created by UbivisMedia - Bringing advanced procedural generation to Minecraft!**
