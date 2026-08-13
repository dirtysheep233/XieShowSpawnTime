package com.seosean.showspawntime;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OverlayRenderer {

    private static final Map<Integer, Set<Integer>> GIANT_WAVES = Map.ofEntries(
            Map.entry(15, Set.of(6)),
            Map.entry(25, Set.of(2, 4, 6)),
            Map.entry(30, Set.of(2, 5, 8)),
            Map.entry(35, Set.of(2, 4, 6)),
            Map.entry(42, Set.of(3, 6)),
            Map.entry(44, Set.of(3, 4, 6)),
            Map.entry(47, Set.of(2, 4, 6)),
            Map.entry(50, Set.of(2, 4, 6)),
            Map.entry(52, Set.of(1, 3, 5)),
            Map.entry(54, Set.of(1, 3, 5)),
            Map.entry(57, Set.of(1, 3, 5, 7)),
            Map.entry(62, Set.of(6)),
            Map.entry(63, Set.of(1, 3)),
            Map.entry(64, Set.of(2, 4, 6)),
            Map.entry(67, Set.of(6)),
            Map.entry(68, Set.of(2, 4, 6))
    );

    private static final Map<Integer, Set<Integer>> GHAST_WAVES = Map.ofEntries(
            Map.entry(24, Set.of(1, 4)),
            Map.entry(27, Set.of(4, 6)),
            Map.entry(30, Set.of(3, 6, 9)),
            Map.entry(33, Set.of(2, 6, 10)),
            Map.entry(36, Set.of(1, 3, 5, 7, 8)),
            Map.entry(39, Set.of(1, 2, 4, 5, 7, 8, 10, 11))
    );

    private static final Map<Integer, Set<Integer>> ELDER_WAVES = Map.ofEntries(
            Map.entry(45, Set.of(7)),
            Map.entry(47, Set.of(3)),
            Map.entry(49, Set.of(12)),
            Map.entry(50, Set.of(3, 5)),
            Map.entry(52, Set.of(4, 6)),
            Map.entry(54, Set.of(4, 6)),
            Map.entry(55, Set.of(3, 5, 7)),
            Map.entry(57, Set.of(4, 6, 8)),
            Map.entry(60, Set.of(6)),
            Map.entry(61, Set.of(3, 4, 5, 6)),
            Map.entry(64, Set.of(1, 3, 5)),
            Map.entry(65, Set.of(2, 4, 6)),
            Map.entry(66, Set.of(2, 4, 6)),
            Map.entry(68, Set.of(1, 3, 5)),
            Map.entry(69, Set.of(6))
    );

    private static final Map<Integer, Set<Integer>> NUCLEAR_WAVES = Map.ofEntries(
            Map.entry(43, Set.of(7)),
            Map.entry(48, Set.of(4, 6)),
            Map.entry(58, Set.of(2, 4, 6, 8)),
            Map.entry(62, Set.of(2, 4)),
            Map.entry(67, Set.of(1, 2, 3, 4, 5))
    );

    private static final Map<Integer, Set<Integer>> BIG_BOSS_WAVES = Map.ofEntries(
            Map.entry(60, Set.of(6)),
            Map.entry(63, Set.of(6)),
            Map.entry(66, Set.of(3, 5, 7)),
            Map.entry(69, Set.of(2, 4))
    );

    private static final Map<Integer, Set<Integer>> OMEGA_WAVES = Map.ofEntries(
            Map.entry(24, Set.of(7)),
            Map.entry(29, Set.of(8)),
            Map.entry(34, Set.of(6)),
            Map.entry(39, Set.of(13))
    );

    private static final Set<Integer> ALL_ELDER_ROUNDS = Set.of(59);

    private static final int GIANT_BRIGHT    = 0xFF55FFFF;
    private static final int GIANT_DIM       = 0xFF007777;
    private static final int GHAST_BRIGHT    = 0xFFFFA500;
    private static final int GHAST_DIM       = 0xFF884400;
    private static final int ELDER_BRIGHT    = 0xFFFF0000;
    private static final int ELDER_DIM       = 0xFF880000;
    private static final int NUCLEAR_BRIGHT  = 0xFF00FF00;
    private static final int NUCLEAR_DIM     = 0xFF008800;
    private static final int BIG_BOSS_BRIGHT = 0xFFFF00FF;
    private static final int BIG_BOSS_DIM    = 0xFF880088;
    private static final int OMEGA_BRIGHT    = 0xFFFF7F00;
    private static final int OMEGA_DIM       = 0xFF903000; 

    public void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!ShowSpawnTimeClient.STATE.isInZombies()) return;
        renderSpawnTimes(context, client);
    }

    public MutableText enhanceSidebarLine(ScoreboardEntry entry, MutableText decoratedName) {
        if (!ShowSpawnTimeClient.STATE.isZombiesTitle()) return decoratedName;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return decoratedName;

        String plainName = ScoreboardSnapshot.trim(decoratedName);
        AbstractClientPlayerEntity player = null;

        if (plainName.contains(":") || plainName.contains("：")) {
            String beforeColon = plainName.split("[:：]", 2)[0].trim();
            player = findPlayer(beforeColon);
        }

        if (player == null) {
            String ownerName = ScoreboardSnapshot.trim(entry.owner());
            player = findPlayer(ownerName);
        }

        if (player == null) return decoratedName;

        // ----- 三色血量显示 START -----
        int health = (int) player.getHealth();
        int maxHealth = (int) player.getMaxHealth();

        Formatting color;
        if (health < maxHealth * 0.25) {
            color = Formatting.RED;          // 低于 25% → 红色
        } else if (health < maxHealth * 0.5) {
            color = Formatting.YELLOW;       // 25% ~ 50% → 黄色
        } else {
            color = Formatting.GREEN;        // ≥ 50% → 绿色
        }

        // 构造 "(健康值) " 格式，括号白色，数字着色
        MutableText healthText = Text.literal("(").formatted(Formatting.WHITE)
                .append(Text.literal(String.valueOf(health)).formatted(color))
                .append(Text.literal(") ").formatted(Formatting.WHITE));
        // ----- END -----

        return healthText.append(decoratedName);
    }

    private static AbstractClientPlayerEntity findPlayer(String name) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || name.isEmpty()) return null;
        for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
            String profileName = p.getName().getString();
            String displayName = ScoreboardSnapshot.trim(p.getDisplayName());
            if (profileName.equals(name) || displayName.equals(name)
                    || profileName.equalsIgnoreCase(name) || displayName.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    private void renderSpawnTimes(DrawContext context, MinecraftClient client) {
        int[] times = ShowSpawnTimeClient.STATE.roundTimes();
        int round = ShowSpawnTimeClient.STATE.currentRound();
        if (round == 0 || times.length == 0) return;

        TextRenderer renderer = client.textRenderer;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int gameMs = ShowSpawnTimeClient.STATE.gameMilliseconds();
        ZombiesMap map = ShowSpawnTimeClient.STATE.map();

        int x = ShowSpawnTimeClient.CONFIG.xSpawnTime < 0
                ? screenWidth - renderer.getWidth("➤ W2 00.0")
                : (int) (ShowSpawnTimeClient.CONFIG.xSpawnTime * screenWidth);
        int y = ShowSpawnTimeClient.CONFIG.ySpawnTime < 0
                ? screenHeight - renderer.fontHeight * (times.length + 2)
                : (int) (ShowSpawnTimeClient.CONFIG.ySpawnTime * screenHeight);

        int nextWave = nextWave(times, gameMs);
        int arrowY = y + renderer.fontHeight * (nextWave - 1);
        context.drawTextWithShadow(renderer, "➤ ", x, arrowY, 0xFFCC00CC);
        int textX = x + renderer.getWidth("➤ ");

        for (int index = 0; index < times.length; index++) {
            int wave = index + 1;
            int lineY = y + renderer.fontHeight * index;
            int waveMs = times[index] * 1000;
            double remaining = (waveMs - gameMs) / 1000.0;
            String timeStr = remaining > 0
                    ? String.format("%.1f%s", remaining, SstI18n.string("hud.showspawntime.second"))
                    : SstI18n.string("hud.showspawntime.spawned");
            int color = getWaveColor(round, wave, nextWave, map);
            String label = getWaveLabel(round, wave, map);
            context.drawTextWithShadow(renderer, label + timeStr, textX, lineY, color);
        }
    }

    private static List<String> getSpecialTypes(int round, int wave) {
        List<String> types = new ArrayList<>();
        if (GIANT_WAVES.getOrDefault(round, Set.of()).contains(wave)) types.add("giant");
        if (GHAST_WAVES.getOrDefault(round, Set.of()).contains(wave)) types.add("ghast");
        if (ELDER_WAVES.getOrDefault(round, Set.of()).contains(wave) || ALL_ELDER_ROUNDS.contains(round)) types.add("terminal");
        if (NUCLEAR_WAVES.getOrDefault(round, Set.of()).contains(wave)) types.add("nuclear");
        if (BIG_BOSS_WAVES.getOrDefault(round, Set.of()).contains(wave)) types.add("dhp");
        if (OMEGA_WAVES.getOrDefault(round, Set.of()).contains(wave)) types.add("omegagma");
        return types;
    }

    private static int getWaveColor(int round, int wave, int nextWave, ZombiesMap map) {
        boolean isNext = wave == nextWave;
        boolean passed = wave < nextWave;

        if (map == ZombiesMap.PORTAL_LAB) {
            if (passed) return 0xFF5A5A5A;
            List<String> types = getSpecialTypes(round, wave);
            if (types.isEmpty()) {
                return isNext ? 0xFFFFFF00 : 0xFF808080;
            }
            String priority = types.get(types.size() - 1);
            return switch (priority) {
                case "giant" -> isNext ? GIANT_BRIGHT : GIANT_DIM;
                case "ghast" -> isNext ? GHAST_BRIGHT : GHAST_DIM;
                case "terminal" -> isNext ? ELDER_BRIGHT : ELDER_DIM;
                case "nuclear" -> isNext ? NUCLEAR_BRIGHT : NUCLEAR_DIM;
                case "dhp" -> isNext ? BIG_BOSS_BRIGHT : BIG_BOSS_DIM;
                case "omegagma" -> isNext ? OMEGA_BRIGHT : OMEGA_DIM;
                default -> isNext ? 0xFFFFFF00 : 0xFF808080;
            };
        }
        return isNext ? 0xFFFFFF00 : passed ? 0xFF5A5A5A : 0xFF808080;
    }

    private static String getWaveLabel(int round, int wave, ZombiesMap map) {
        if (map == ZombiesMap.PORTAL_LAB) {
            List<String> types = getSpecialTypes(round, wave);
            if (!types.isEmpty()) {
                List<String> localized = new ArrayList<>();
                for (String type : types) {
                    localized.add(SstI18n.string("hud.showspawntime." + type));
                }
                return "W" + wave + " " + String.join("+", localized) + " ";
            }
        }
        return "W" + wave + " ";
    }

    private static int nextWave(int[] times, int milliseconds) {
        for (int index = 0; index < times.length; index++) {
            if (milliseconds <= times[index] * 1000) return index + 1;
        }
        return times.length;
    }
}