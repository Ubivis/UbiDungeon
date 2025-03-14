package com.ubivismedia.dungeonlobby.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import com.ubivismedia.dungeonlobby.DungeonLobby;

public class DatabaseManager {
    private final DungeonLobby plugin;
    private Connection connection;
    private final String databaseType;
    private final String databaseURL;
    private final Logger logger;

    public DatabaseManager(DungeonLobby plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        FileConfiguration config = plugin.getConfig();
        this.databaseType = config.getString("database.type", "sqlite");

        if (databaseType.equalsIgnoreCase("mysql")) {
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String dbName = config.getString("database.mysql.database", "dungeonlobby");
            String user = config.getString("database.mysql.user", "root");
            String password = config.getString("database.mysql.password", "");
            this.databaseURL = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false";
        } else {
            this.databaseURL = "jdbc:sqlite:plugins/DungeonLobby/dungeon_stats.db";
        }
    }

    public void connect() {
        try {
            connection = DriverManager.getConnection(databaseURL);
            createTables();
            logger.info("Connected to " + databaseType.toUpperCase() + " database successfully.");
        } catch (SQLException e) {
            logger.severe("Failed to connect to database: " + e.getMessage());
        }
    }

    private void createTables() {
        String createDungeonStatsTable = "CREATE TABLE IF NOT EXISTS dungeon_stats ("
                + "player_uuid TEXT PRIMARY KEY, "
                + "dungeons_completed INTEGER DEFAULT 0, "
                + "dungeons_failed INTEGER DEFAULT 0"
                + ");";

        String createPlayerDataTable = "CREATE TABLE IF NOT EXISTS player_data ("
                + "player_uuid TEXT PRIMARY KEY, "
                + "coins INTEGER DEFAULT 0, "
                + "last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");";

        String createDungeonHistoryTable = "CREATE TABLE IF NOT EXISTS dungeon_history ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "player_uuid TEXT, "
                + "dungeon_name TEXT, "
                + "difficulty TEXT, "
                + "completed BOOLEAN, "
                + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ");";

        String createChestsTable = "CREATE TABLE IF NOT EXISTS chests ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "player_uuid TEXT, "
                + "chest_location TEXT, "
                + "contents TEXT"
                + ");";

        try {
            executeStatement(createDungeonStatsTable);
            executeStatement(createPlayerDataTable);
            executeStatement(createDungeonHistoryTable);
            executeStatement(createChestsTable);
        } catch (SQLException e) {
            logger.severe("Failed to create tables: " + e.getMessage());
        }
    }

    private void executeStatement(String sql) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }

    public void updateDungeonStats(String playerUUID, boolean completed) {
        String updateSQL = "INSERT INTO dungeon_stats (player_uuid, dungeons_completed, dungeons_failed) "
                + "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE "
                + "dungeons_completed = dungeons_completed + ?, "
                + "dungeons_failed = dungeons_failed + ?";

        try (PreparedStatement stmt = connection.prepareStatement(updateSQL)) {
            stmt.setString(1, playerUUID);
            stmt.setInt(2, completed ? 1 : 0);
            stmt.setInt(3, completed ? 0 : 1);
            stmt.setInt(4, completed ? 1 : 0);
            stmt.setInt(5, completed ? 0 : 1);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to update dungeon stats: " + e.getMessage());
        }
    }

    public void logDungeonRun(String playerUUID, String dungeonName, String difficulty, boolean completed) {
        String insertSQL = "INSERT INTO dungeon_history (player_uuid, dungeon_name, difficulty, completed) "
                + "VALUES (?, ?, ?, ?);";

        try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
            stmt.setString(1, playerUUID);
            stmt.setString(2, dungeonName);
            stmt.setString(3, difficulty);
            stmt.setBoolean(4, completed);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to log dungeon run: " + e.getMessage());
        }
    }

    public int getCompletedDungeons(String playerUUID) {
        return getDungeonStat(playerUUID, "dungeons_completed");
    }

    public int getFailedDungeons(String playerUUID) {
        return getDungeonStat(playerUUID, "dungeons_failed");
    }

    private int getDungeonStat(String playerUUID, String column) {
        String query = "SELECT " + column + " FROM dungeon_stats WHERE player_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUUID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(column);
            }
        } catch (SQLException e) {
            logger.severe("Failed to fetch dungeon stats: " + e.getMessage());
        }
        return 0;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed.");
            }
        } catch (SQLException e) {
            logger.severe("Failed to close database connection: " + e.getMessage());
        }
    }

    public void saveSecureChest(Location location, UUID playerUUID) {
        String insertSQL = "INSERT INTO chests (player_uuid, chest_location, contents) VALUES (?, ?, '')";
        try (PreparedStatement stmt = connection.prepareStatement(insertSQL)) {
            stmt.setString(1, playerUUID.toString());
            stmt.setString(2, locationToString(location));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to save secure chest: " + e.getMessage());
        }
    }

    public Map<Location, UUID> loadSecureChests() {
        Map<Location, UUID> chests = new HashMap<>();
        String query = "SELECT player_uuid, chest_location FROM chests";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                Location location = stringToLocation(rs.getString("chest_location"));
                chests.put(location, playerUUID);
            }
        } catch (SQLException e) {
            logger.severe("Failed to load secure chests: " + e.getMessage());
        }
        return chests;
    }

    public void saveChestContents(Location location, Inventory inventory) {
        String contents = serializeInventory(inventory);
        String updateSQL = "UPDATE chests SET contents = ? WHERE chest_location = ?";
        try (PreparedStatement stmt = connection.prepareStatement(updateSQL)) {
            stmt.setString(1, contents);
            stmt.setString(2, locationToString(location));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to save chest contents: " + e.getMessage());
        }
    }

    public ItemStack[] loadChestContents(Location location) {
        String query = "SELECT contents FROM chests WHERE chest_location = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, locationToString(location));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return deserializeInventory(rs.getString("contents"));
            }
        } catch (SQLException e) {
            logger.severe("Failed to load chest contents: " + e.getMessage());
        }
        return new ItemStack[0];
    }

    private String locationToString(Location location) {
        return location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private Location stringToLocation(String str) {
        String[] parts = str.split(",");
        return new Location(Bukkit.getWorld(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }

    private String serializeInventory(Inventory inventory) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(inventory.getSize());
            for (ItemStack item : inventory.getContents()) {
                dataOutput.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            logger.severe("Failed to serialize inventory: " + e.getMessage());
        }
        return "";
    }

    private ItemStack[] deserializeInventory(String data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            return items;
        } catch (IOException | ClassNotFoundException e) {
            logger.severe("Failed to deserialize inventory: " + e.getMessage());
        }
        return new ItemStack[0];
    }
}
