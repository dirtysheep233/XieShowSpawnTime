package com.seosean.showspawntime;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DpsCounter {
    private static final List<WeaponInfo> WEAPONS = List.of(
            new WeaponInfo("Pistol", List.of("mob.irongolem.hit", "entity.iron_golem.damage", "entity.iron_golem.hurt"), 2.5F, 10, 15, Items.WOODEN_HOE, 6, 6),
            new WeaponInfo("Shotgun", List.of("random.explode", "entity.generic.explode"), 2.5F, 8, 12, Items.IRON_HOE, 4.5, 4.5),
            new WeaponInfo("Sniper", List.of("fireworks.blast_far", "entity.firework_rocket.blast_far"), 0.5F, 30, 45, Items.WOODEN_SHOVEL, 30, 40),
            new WeaponInfo("Rifle", List.of("fireworks.largeblast", "entity.firework_rocket.large_blast"), 2.5F, 7, 10, Items.STONE_HOE, 6, 8),
            new WeaponInfo("Zombie Zapper", List.of("fire.ignite", "item.flintandsteel.use"), 0.5F, 15, 20, Items.DIAMOND_PICKAXE, 12, 18),
            new WeaponInfo("Elder Gun", List.of("ambient.weather.thunder", "entity.lightning_bolt.thunder"), 2.0F, 20, 30, Items.SHEARS, 15, 20),
            new WeaponInfo("Flame Thrower", List.of("fire.fire", "block.fire.ambient"), 2.0F, 4, 6, Items.GOLDEN_HOE, 2, 2),
            new WeaponInfo("Blow Dart", List.of("random.bow", "entity.arrow.shoot"), 0.5F, 20, 30, Items.IRON_SHOVEL, 10, 10),
            new WeaponInfo("Zombie Soaker", List.of("mob.slime.attack", "entity.slime.attack"), 2.0F, 5, 10, Items.DIAMOND_HOE, 5, 8),
            new WeaponInfo("Rainbow Rifle", List.of("fireworks.largeblast", "entity.firework_rocket.large_blast"), 2.5F, 5, 7, Items.GOLDEN_SHOVEL, 5, 6, 6.5, 7),
            new WeaponInfo("Double Barrel Shotgun", List.of("fireworks.largeblast", "entity.firework_rocket.large_blast"), 0.8F, 8, 12, Items.FLINT_AND_STEEL, 7, 7, 8, 8),
            new WeaponInfo("Gold Digger", List.of("dig.stone", "block.stone.break"), 2.0F, 10, 15, Items.GOLDEN_PICKAXE, 6, 8, 10, 12, 15, 20)
    );

    private List<WeaponInfo> probableWeapons = new ArrayList<>();
    private WeaponInfo readyWeapon;
    private WeaponInfo cachedWeapon;
    private double damage;
    private double dps;
    private int instaKillSeconds;
    private int doubleGoldSeconds;
    private int tick;

    public void reset() {
        probableWeapons = new ArrayList<>();
        readyWeapon = null;
        cachedWeapon = null;
        damage = 0;
        dps = 0;
        instaKillSeconds = 0;
        doubleGoldSeconds = 0;
        tick = 0;
    }

    public void tick() {
        if (++tick < 20) {
            return;
        }
        tick = 0;
        if (instaKillSeconds > 0) instaKillSeconds--;
        if (doubleGoldSeconds > 0) doubleGoldSeconds--;
        dps = damage;
        damage = 0;
    }

    public void onPowerup(PowerupManager.PowerupType type) {
        if (type == PowerupManager.PowerupType.INSTA_KILL) {
            instaKillSeconds = 10;
        } else if (type == PowerupManager.PowerupType.DOUBLE_GOLD) {
            doubleGoldSeconds = 30;
        }
    }

    public void onSound(String sound, float pitch) {
        if (!ShowSpawnTimeClient.CONFIG.dpsCounter) {
            return;
        }
        String normalized = normalizeSound(sound);
        if (isSuccessfulHit(normalized, pitch)) {
            if (readyWeapon == null && cachedWeapon != null) {
                readyWeapon = cachedWeapon;
            }
            if (readyWeapon != null) {
                damage += readyWeapon.damage();
                cachedWeapon = readyWeapon;
            }
            readyWeapon = null;
            return;
        }

        List<WeaponInfo> matched = new ArrayList<>();
        for (WeaponInfo weapon : WEAPONS) {
            if (weapon.matchesSound(normalized, pitch)) {
                matched.add(weapon);
            }
        }
        if (!matched.isEmpty()) {
            probableWeapons = matched;
        }
    }

    public void onChat(String message, boolean inZombies) {
        if (!ShowSpawnTimeClient.CONFIG.dpsCounter || !inZombies || message.contains(":")) {
            return;
        }
        if (!message.contains("+") || !ShowSpawnTimeClient.LANG.contains(message, "zombies.game.gold")) {
            return;
        }

        Matcher matcher = Pattern.compile("\\d+").matcher(message);
        if (!matcher.find()) {
            return;
        }
        int gold = Integer.parseInt(matcher.group());
        if (doubleGoldSeconds > 0) {
            gold /= 2;
        }
        boolean critical = ShowSpawnTimeClient.LANG.contains(message, "zombies.game.criticalhit");
        for (WeaponInfo candidate : WEAPONS) {
            if ((critical ? candidate.criticalGold : candidate.gold) != gold) {
                continue;
            }
            for (WeaponInfo bySound : probableWeapons) {
                if (candidate.item == bySound.item) {
                    readyWeapon = bySound;
                    probableWeapons = new ArrayList<>();
                    return;
                }
            }
        }
        probableWeapons = new ArrayList<>();
    }

    public double dps() {
        return dps;
    }

    public boolean instaKillActive() {
        return instaKillSeconds > 0;
    }

    private static boolean isSuccessfulHit(String sound, float pitch) {
        boolean id = sound.endsWith("random.successful_hit")
                || sound.endsWith("entity.arrow.hit_player");
        return id && (Math.abs(1.5F - pitch) < 0.1 || Math.abs(2.0F - pitch) < 0.1);
    }

    private static String normalizeSound(String sound) {
        String normalized = sound.toLowerCase(Locale.ROOT);
        return normalized.startsWith("minecraft:") ? normalized.substring("minecraft:".length()) : normalized;
    }

    private record WeaponInfo(String name, List<String> sounds, float pitch, int gold, int criticalGold,
                              Item item, double baseDamage, double... ultimateDamage) {
        private boolean matchesSound(String sound, float actualPitch) {
            return sounds.stream().anyMatch(sound::endsWith) && Math.abs(pitch - actualPitch) < 0.1;
        }

        private double damage() {
            int level = ultimateLevel();
            if (level > 0 && level <= ultimateDamage.length) {
                return ultimateDamage[level - 1];
            }
            return baseDamage;
        }

        private int ultimateLevel() {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) {
                return 0;
            }
            for (int index = 0; index < client.player.getInventory().size(); index++) {
                ItemStack stack = client.player.getInventory().getStack(index);
                if (!stack.isOf(item)) {
                    continue;
                }
                String displayName = stack.getName().getString();
                if (!ShowSpawnTimeClient.LANG.contains(displayName, "zombies.game.ultimate")) {
                    continue;
                }
                if (displayName.contains("IV") || displayName.contains("4")) return 4;
                if (displayName.contains("V") || displayName.contains("5")) return 5;
                if (displayName.contains("III") || displayName.contains("3")) return 3;
                if (displayName.contains("II") || displayName.contains("2")) return 2;
                if (displayName.contains("I") || displayName.contains("1")) return 1;
            }
            return 0;
        }
    }
}
