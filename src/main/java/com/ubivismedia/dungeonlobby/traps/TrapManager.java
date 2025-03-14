package com.ubivismedia.dungeonlobby.traps;

import com.ubivismedia.dungeonlobby.localization.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Random;

public class TrapManager implements Listener {
    private final Random random = new Random();
    private final LanguageManager languageManager;

    public TrapManager(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    public void placeRandomTrap(World world, int x, int y, int z) {
        int trapType = random.nextInt(3); // 3 verschiedene Fallen
        switch (trapType) {
            case 0:
                placeSpeerTrap(world, x, y, z);
                break;
            case 1:
                placeFireTrap(world, x, y, z);
                break;
            case 2:
                placePoisonTrap(world, x, y, z);
                break;
        }
    }

    private void placeSpeerTrap(World world, int x, int y, int z) {
        Location trapLocation = new Location(world, x, y, z);
        trapLocation.getBlock().setType(Material.STONE_PRESSURE_PLATE);
    }

    private void placeFireTrap(World world, int x, int y, int z) {
        Location trapLocation = new Location(world, x, y, z);
        trapLocation.getBlock().setType(Material.NETHERRACK);
        world.getBlockAt(x, y + 1, z).setType(Material.FIRE);
    }

    private void placePoisonTrap(World world, int x, int y, int z) {
        Location trapLocation = new Location(world, x, y, z);
        trapLocation.getBlock().setType(Material.MOSSY_COBBLESTONE);
    }

    public void placeHiddenTrap(World world, int x, int y, int z) {
        int hiddenType = random.nextInt(3); // 3 verschiedene versteckte Fallen
        switch (hiddenType) {
            case 0:
                placeTrapUnderCarpet(world, x, y, z);
                break;
            case 1:
                placeTrapInWall(world, x, y, z);
                break;
            case 2:
                placeTrapInCeiling(world, x, y, z);
                break;
        }
    }

    private void placeTrapUnderCarpet(World world, int x, int y, int z) {
        world.getBlockAt(x, y, z).setType(Material.STONE_PRESSURE_PLATE);
        world.getBlockAt(x, y + 1, z).setType(Material.WHITE_CARPET);
    }

    private void placeTrapInWall(World world, int x, int y, int z) {
        world.getBlockAt(x, y, z).setType(Material.DISPENSER);
        world.getBlockAt(x, y + 1, z).setType(Material.STONE_BUTTON);
    }

    private void placeTrapInCeiling(World world, int x, int y, int z) {
        world.getBlockAt(x, y, z).setType(Material.DISPENSER);
        world.getBlockAt(x, y - 1, z).setType(Material.STONE_BUTTON);
    }

    @EventHandler
    public void onPlayerTriggerTrap(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        Material blockType = loc.getBlock().getType();

        switch (blockType) {
            case STONE_PRESSURE_PLATE:
                player.damage(5.0);
                languageManager.sendMessage(player, "trap.spear");
                break;
            case FIRE:
                player.setFireTicks(100);
                languageManager.sendMessage(player, "trap.fire");
                break;
            case MOSSY_COBBLESTONE:
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                languageManager.sendMessage(player, "trap.poison");
                break;
        }
    }
}