package com.ubivismedia.dungeonlobby.dungeon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DungeonGenerator {
    private final World dungeonWorld;
    private final Random random = new Random();
    private final List<Room> rooms = new ArrayList<>();
    private static final int DUNGEON_SIZE = 64;
    private static final int MIN_ROOM_SIZE = 6;
    private static final int MAX_ROOM_SIZE = 16;
    private Room entranceRoom;
    private Room exitRoom;

    public DungeonGenerator(World world) {
        this.dungeonWorld = world;
    }

    public void generateDungeon() {
        rooms.clear();
        splitSpace(0, 0, DUNGEON_SIZE, DUNGEON_SIZE);
        carveRooms();
        connectRooms();
        placeEntranceAndExit();
    }

    private void splitSpace(int x, int z, int width, int height) {
        if (width <= MAX_ROOM_SIZE && height <= MAX_ROOM_SIZE) {
            int roomWidth = random.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;
            int roomHeight = random.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1) + MIN_ROOM_SIZE;
            int roomX = x + (width - roomWidth) / 2;
            int roomZ = z + (height - roomHeight) / 2;
            rooms.add(new Room(roomX, roomZ, roomWidth, roomHeight));
            return;
        }

        boolean splitVertically = random.nextBoolean();
        if (width > height) {
            splitVertically = true;
        } else if (height > width) {
            splitVertically = false;
        }

        if (splitVertically) {
            int split = random.nextInt(width - MIN_ROOM_SIZE * 2) + MIN_ROOM_SIZE;
            splitSpace(x, z, split, height);
            splitSpace(x + split, z, width - split, height);
        } else {
            int split = random.nextInt(height - MIN_ROOM_SIZE * 2) + MIN_ROOM_SIZE;
            splitSpace(x, z, width, split);
            splitSpace(x, z + split, width, height - split);
        }
    }

    private void carveRooms() {
        for (Room room : rooms) {
            for (int x = room.x; x < room.x + room.width; x++) {
                for (int z = room.z; z < room.z + room.height; z++) {
                    Location loc = new Location(dungeonWorld, x, 64, z);
                    loc.getBlock().setType(Material.AIR);
                }
            }
        }
    }

    private void connectRooms() {
        Collections.sort(rooms, Comparator.comparingInt(r -> r.x));
        for (int i = 0; i < rooms.size() - 1; i++) {
            Room a = rooms.get(i);
            Room b = rooms.get(i + 1);
            carveCorridor(a.centerX(), a.centerZ(), b.centerX(), b.centerZ());
        }
    }

    private void carveCorridor(int x1, int z1, int x2, int z2) {
        while (x1 != x2) {
            Location loc = new Location(dungeonWorld, x1, 64, z1);
            loc.getBlock().setType(Material.AIR);
            x1 += Integer.compare(x2, x1);
        }
        while (z1 != z2) {
            Location loc = new Location(dungeonWorld, x1, 64, z1);
            loc.getBlock().setType(Material.AIR);
            z1 += Integer.compare(z2, z1);
        }
    }

    private void placeEntranceAndExit() {
        if (rooms.isEmpty()) return;

        entranceRoom = rooms.get(0); // Erste Raum als Eingang
        exitRoom = rooms.get(rooms.size() - 1); // Letzte Raum als Ausgang

        Location exit = new Location(dungeonWorld, exitRoom.centerX(), 64, exitRoom.centerZ());

        // Erstelle ein End-Portal als Ausgang
        exit.getBlock().setType(Material.END_PORTAL_FRAME);

        // Partikeleffekt für das Portal
        new BukkitRunnable() {
            @Override
            public void run() {
                dungeonWorld.spawnParticle(Particle.PORTAL, exit.clone().add(0.5, 1, 0.5), 20, 0.5, 1, 0.5, 0.1);
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("DungeonLobby"), 0L, 20L);
    }

    private static class Room {
        int x, z, width, height;

        Room(int x, int z, int width, int height) {
            this.x = x;
            this.z = z;
            this.width = width;
            this.height = height;
        }

        int centerX() {
            return x + width / 2;
        }

        int centerZ() {
            return z + height / 2;
        }
    }
}