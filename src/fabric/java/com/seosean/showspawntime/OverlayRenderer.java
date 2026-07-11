package com.seosean.showspawntime;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OverlayRenderer {
    private static final Map<Integer, List<Integer>> OLD_ONE_WAVES = new HashMap<>();
    private static final Map<Integer, List<Integer>> GIANT_WAVES = new HashMap<>();
    private static final Map<Integer, List<Integer>> BOTH_WAVES = new HashMap<>();

    static {
        OLD_ONE_WAVES.put(40, List.of(5)); OLD_ONE_WAVES.put(45, List.of(3,4)); OLD_ONE_WAVES.put(46, List.of(4));
        OLD_ONE_WAVES.put(48, List.of(4)); OLD_ONE_WAVES.put(54, List.of(5)); OLD_ONE_WAVES.put(55, List.of(6));
        OLD_ONE_WAVES.put(58, List.of(5)); OLD_ONE_WAVES.put(59, List.of(1,2,3,4,5,6)); OLD_ONE_WAVES.put(60, List.of(3,4));
        OLD_ONE_WAVES.put(64, List.of(5,6)); OLD_ONE_WAVES.put(67, List.of(6)); OLD_ONE_WAVES.put(68, List.of(5,6));
        OLD_ONE_WAVES.put(69, List.of(5,6)); OLD_ONE_WAVES.put(70, List.of(2,3)); OLD_ONE_WAVES.put(74, List.of(4,5,6));
        OLD_ONE_WAVES.put(77, List.of(6)); OLD_ONE_WAVES.put(78, List.of(5,6)); OLD_ONE_WAVES.put(79, List.of(5,6));
        OLD_ONE_WAVES.put(80, List.of(2,3)); OLD_ONE_WAVES.put(84, List.of(4,5,6)); OLD_ONE_WAVES.put(87, List.of(6));
        OLD_ONE_WAVES.put(88, List.of(5,6)); OLD_ONE_WAVES.put(89, List.of(5,6)); OLD_ONE_WAVES.put(90, List.of(2,3));
        OLD_ONE_WAVES.put(94, List.of(4,5,6)); OLD_ONE_WAVES.put(97, List.of(6)); OLD_ONE_WAVES.put(98, List.of(5,6));
        OLD_ONE_WAVES.put(99, List.of(5,6)); OLD_ONE_WAVES.put(100, List.of(2,3));

        GIANT_WAVES.put(15, List.of(6)); GIANT_WAVES.put(20, List.of(3,5)); GIANT_WAVES.put(22, List.of(4,6));
        GIANT_WAVES.put(24, List.of(2,4,6)); GIANT_WAVES.put(30, List.of(1,2,3)); GIANT_WAVES.put(36, List.of(2,3));
        GIANT_WAVES.put(37, List.of(2,3)); GIANT_WAVES.put(38, List.of(2,3)); GIANT_WAVES.put(39, List.of(2,3));
        GIANT_WAVES.put(40, List.of(2,3)); GIANT_WAVES.put(41, List.of(2,3)); GIANT_WAVES.put(42, List.of(1,2,3));
        GIANT_WAVES.put(43, List.of(2,4,6)); GIANT_WAVES.put(44, List.of(1,2,3)); GIANT_WAVES.put(45, List.of(2));
        GIANT_WAVES.put(47, List.of(3)); GIANT_WAVES.put(50, List.of(2,4)); GIANT_WAVES.put(51, List.of(2,4));
        GIANT_WAVES.put(52, List.of(2,4)); GIANT_WAVES.put(53, List.of(2,4)); GIANT_WAVES.put(54, List.of(4));
        GIANT_WAVES.put(55, List.of(1,2,3,4)); GIANT_WAVES.put(58, List.of(4)); GIANT_WAVES.put(65, List.of(4,5,6));
        GIANT_WAVES.put(75, List.of(4,5,6)); GIANT_WAVES.put(85, List.of(4,5,6)); GIANT_WAVES.put(95, List.of(4,5,6));

        BOTH_WAVES.put(54, List.of(2)); BOTH_WAVES.put(55, List.of(5)); BOTH_WAVES.put(58, List.of(2));
        BOTH_WAVES.put(70, List.of(4,5,6)); BOTH_WAVES.put(80, List.of(4,5,6)); BOTH_WAVES.put(90, List.of(4,5,6));
        BOTH_WAVES.put(100, List.of(4,5,6));
    }

    public void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        renderAutoSplits(context, client);
        if (!ShowSpawnTimeClient.STATE.isInZombies()) {
            return;
        }
        renderSpawnTimes(context, client);
        renderPowerups(context, client);
        renderDps(context, client);
        renderLightningRod(context, client);
    }

    public MutableText enhanceSidebarLine(ScoreboardEntry entry, Text name) {
        GameState state = ShowSpawnTimeClient.STATE;
        if (!state.isZombiesTitle()) {
            return name.copy();
        }

        String plain = ScoreboardSnapshot.trim(name);
        MutableText base = name.copy();
        if (ShowSpawnTimeClient.CONFIG.wave3LeftNotice && ShowSpawnTimeClient.LANG.isZombiesLeft(plain)
                && (state.map() == ZombiesMap.DEAD_END || state.map() == ZombiesMap.BAD_BLOOD)) {
            int left = state.waveThreeLeft();
            int actual = numberAfterColon(plain);
            if (left > 0 && actual >= 0) {
                return base.append(Text.literal(" | ").formatted(Formatting.WHITE))
                        .append(Text.literal(Integer.toString(left)).formatted(actual <= left ? Formatting.GREEN : Formatting.RED));
            }
        }

        String colon = plain.contains("：") ? "：" : plain.contains(":") ? ":" : "";
        if (colon.isEmpty()) {
            return base;
        }
        String displayedName = plain.split(PatternQuote.quote(colon), 2)[0].trim();
        AbstractClientPlayerEntity player = findPlayer(entry.owner(), displayedName);
        Map<String, Long> cooldowns = state.fastReviveCooldowns();
        String cooldownName = findCooldownName(entry.owner(), displayedName, cooldowns);
        if (player == null && cooldownName == null) {
            return base;
        }
        String playerName = player != null ? player.getName().getString() : cooldownName;
        Formatting healthColor = null;
        if (ShowSpawnTimeClient.CONFIG.playerHealthNotice && player != null && !player.isInvisible()) {
            healthColor = player.getHealth() > player.getMaxHealth() / 2 ? Formatting.GREEN
                    : player.getHealth() > player.getMaxHealth() / 4 ? Formatting.YELLOW : Formatting.RED;
        }
        Long deadline = cooldowns.get(playerName);
        if (deadline != null && deadline <= System.nanoTime()) {
            deadline = null;
        }
        String raw = base.getString();
        int colonIndex = raw.indexOf('：');
        if (colonIndex < 0) colonIndex = raw.indexOf(':');
        if (colonIndex < 0) {
            return base;
        }
        MutableText healthText = healthColor == null ? Text.empty() : Text.literal("(").formatted(Formatting.WHITE)
                .append(Text.literal(Integer.toString((int) player.getHealth())).formatted(healthColor))
                .append(Text.literal(") ").formatted(Formatting.WHITE));
        MutableText cooldownText = deadline == null || ShowSpawnTimeClient.CONFIG.fastRevivePosition == SstConfig.FastRevivePosition.OFF
                ? Text.empty() : Text.literal("(").formatted(Formatting.WHITE)
                .append(SstI18n.text("hud.showspawntime.seconds", String.format("%.1f", (deadline - System.nanoTime()) / 1_000_000_000.0))
                        .formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal(") ").formatted(Formatting.WHITE));
        MutableText nameText = styledSlice(base, 0, colonIndex);
        MutableText colonAndValue = styledSlice(base, colonIndex, raw.length());
        return switch (ShowSpawnTimeClient.CONFIG.fastRevivePosition) {
            case FRONT -> Text.empty().append(cooldownText).append(healthText).append(nameText).append(colonAndValue);
            case MID -> Text.empty().append(healthText).append(cooldownText).append(nameText).append(colonAndValue);
            case BEHIND -> Text.empty().append(healthText).append(nameText).append(cooldownText).append(colonAndValue);
            case OFF -> Text.empty().append(healthText).append(nameText).append(colonAndValue);
        };
    }

    private void renderSpawnTimes(DrawContext context, MinecraftClient client) {
        int[] times = ShowSpawnTimeClient.STATE.roundTimes();
        int round = ShowSpawnTimeClient.STATE.currentRound();
        if (round == 0 || times.length == 0) {
            return;
        }
        TextRenderer renderer = client.textRenderer;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int x = ShowSpawnTimeClient.CONFIG.xSpawnTime < 0
                ? screenWidth - renderer.getWidth("➤ W2 00:00")
                : (int) (ShowSpawnTimeClient.CONFIG.xSpawnTime * screenWidth);
        int y = ShowSpawnTimeClient.CONFIG.ySpawnTime < 0
                ? screenHeight - renderer.fontHeight * 7
                : (int) (ShowSpawnTimeClient.CONFIG.ySpawnTime * screenHeight);
        int nextWave = nextWave(times, ShowSpawnTimeClient.STATE.gameMilliseconds());
        int arrowY = y + renderer.fontHeight * (5 - times.length + nextWave);
        context.drawTextWithShadow(renderer, "➤ ", x, arrowY, 0xFFCC00CC);
        int textX = x + renderer.getWidth("➤ ");
        for (int index = 0; index < times.length; index++) {
            int wave = index + 1;
            int lineY = y + renderer.fontHeight * (5 - times.length + wave);
            context.drawTextWithShadow(renderer, "W" + wave + " " + formatTime(times[index]),
                    textX, lineY,
                    0xFF000000 | waveColor(round, wave, nextWave));
        }
    }

    private void renderPowerups(DrawContext context, MinecraftClient client) {
        if (!ShowSpawnTimeClient.CONFIG.powerupAlert) {
            return;
        }
        TextRenderer renderer = client.textRenderer;
        int x = ShowSpawnTimeClient.CONFIG.xPowerup < 0 ? 0
                : (int) (ShowSpawnTimeClient.CONFIG.xPowerup * context.getScaledWindowWidth());
        int y = ShowSpawnTimeClient.CONFIG.yPowerup < 0
                ? context.getScaledWindowHeight() / 2 - renderer.fontHeight * 4
                : (int) (ShowSpawnTimeClient.CONFIG.yPowerup * context.getScaledWindowHeight());
        int queue = 0;
        for (PowerupManager.PowerupType type : ShowSpawnTimeClient.POWERUPS.incoming()) {
            MutableText text = SstI18n.text("hud.showspawntime.powerup.incoming", type.text().formatted(type.color))
                    .formatted(Formatting.RED);
            context.drawTextWithShadow(renderer, text, x, y + renderer.fontHeight * queue++, 0xFFFFFFFF);
        }
        for (PowerupManager.ActivePowerup powerup : ShowSpawnTimeClient.POWERUPS.active()) {
            Formatting color = powerup.secondsRemaining() <= 10 && powerup.secondsRemaining() % 2 == 0
                    ? Formatting.WHITE : powerup.type().color;
            MutableText text = SstI18n.text("hud.showspawntime.powerup.active",
                    powerup.type().text().formatted(color),
                    "00:" + String.format("%02d", powerup.secondsRemaining())).formatted(Formatting.AQUA);
            context.drawTextWithShadow(renderer, text, x, y + renderer.fontHeight * queue++, 0xFFFFFFFF);
        }
    }

    private void renderDps(DrawContext context, MinecraftClient client) {
        if (!ShowSpawnTimeClient.CONFIG.dpsCounter) {
            return;
        }
        TextRenderer renderer = client.textRenderer;
        Text dummy = SstI18n.text("hud.showspawntime.dps", SstI18n.text("powerup.showspawntime.insta_kill"));
        int x = ShowSpawnTimeClient.CONFIG.xDpsCounter < 0
                ? (int) (context.getScaledWindowWidth() * 0.75) - renderer.getWidth(dummy)
                : (int) (ShowSpawnTimeClient.CONFIG.xDpsCounter * context.getScaledWindowWidth());
        int y = ShowSpawnTimeClient.CONFIG.yDpsCounter < 0
                ? context.getScaledWindowHeight() - renderer.fontHeight * 3
                : (int) (ShowSpawnTimeClient.CONFIG.yDpsCounter * context.getScaledWindowHeight());
        MutableText text = SstI18n.text("hud.showspawntime.dps",
                ShowSpawnTimeClient.DPS.instaKillActive()
                        ? SstI18n.text("powerup.showspawntime.insta_kill").formatted(Formatting.RED)
                        : Text.literal(trimDouble(ShowSpawnTimeClient.DPS.dps())).formatted(Formatting.AQUA));
        context.drawTextWithShadow(renderer, text, x, y, 0xFFFFFFFF);
    }

    private void renderAutoSplits(DrawContext context, MinecraftClient client) {
        if (!ShowSpawnTimeClient.AUTO_SPLITS.running()) {
            return;
        }
        String time = ShowSpawnTimeClient.AUTO_SPLITS.formattedTime();
        TextRenderer renderer = client.textRenderer;
        int x = ShowSpawnTimeClient.CONFIG.xAutoSplits < 0
                ? context.getScaledWindowWidth() - renderer.getWidth(time)
                : (int) (ShowSpawnTimeClient.CONFIG.xAutoSplits * context.getScaledWindowWidth());
        int y = ShowSpawnTimeClient.CONFIG.yAutoSplits < 0
                ? context.getScaledWindowHeight() - renderer.fontHeight
                : (int) (ShowSpawnTimeClient.CONFIG.yAutoSplits * context.getScaledWindowHeight());
        context.drawText(renderer, time, x, y, 0xFFFFFFFF, false);
    }

    private void renderLightningRod(DrawContext context, MinecraftClient client) {
        if (!ShowSpawnTimeClient.CONFIG.lightningRodQueue || ShowSpawnTimeClient.STATE.lightningDisplayTicks() <= 0) {
            return;
        }
        TextRenderer renderer = client.textRenderer;
        int centerX = context.getScaledWindowWidth() / 2;
        int y = (int) (context.getScaledWindowHeight() / 1.2F);
        String[] lights = {"❶", "❷", "❸", "❹"};
        String display = "❶ ▬ ❷ ▬ ❸ ▬ ❹";
        int start = centerX - renderer.getWidth(display) / 2;
        int x = start;
        int alpha = Math.min(255, ShowSpawnTimeClient.STATE.lightningDisplayTicks() * 255 / 20);
        for (int index = 0; index < lights.length; index++) {
            int rgb = index < ShowSpawnTimeClient.STATE.lightningUses() ? 0xFFFF00 : 0x808080;
            int color = alpha << 24 | rgb;
            context.drawTextWithShadow(renderer, lights[index], x, y, color);
            x += renderer.getWidth(lights[index]);
            if (index + 1 < lights.length) {
                context.drawTextWithShadow(renderer, " ▬ ", x, y, alpha << 24 | 0x00FF00);
                x += renderer.getWidth(" ▬ ");
            }
        }
    }

    private static String findCooldownName(String owner, String displayedName, Map<String, Long> cooldowns) {
        String ownerName = ScoreboardSnapshot.trim(owner);
        for (String name : cooldowns.keySet()) {
            if (name.equals(ownerName) || name.equals(displayedName)) {
                return name;
            }
        }
        return null;
    }

    private static MutableText styledSlice(Text text, int start, int end) {
        MutableText result = Text.empty();
        int[] offset = {0};
        text.visit((style, string) -> {
            int segmentStart = offset[0];
            int segmentEnd = segmentStart + string.length();
            int from = Math.max(start, segmentStart);
            int to = Math.min(end, segmentEnd);
            if (from < to) {
                result.append(Text.literal(string.substring(from - segmentStart, to - segmentStart)).setStyle(style));
            }
            offset[0] = segmentEnd;
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    private static AbstractClientPlayerEntity findPlayer(String owner, String displayedName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;
        String ownerName = ScoreboardSnapshot.trim(owner);
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            String profileName = player.getName().getString();
            if (profileName.equals(ownerName)
                    || displayedName.equals(ScoreboardSnapshot.trim(player.getDisplayName()))) {
                return player;
            }
        }
        return null;
    }

    private int waveColor(int round, int wave, int nextWave) {
        boolean next = wave == nextWave;
        boolean passed = wave < nextWave;
        if (ShowSpawnTimeClient.CONFIG.colorAlert && ShowSpawnTimeClient.STATE.map() == ZombiesMap.ALIEN_ARCADIUM) {
            if (BOTH_WAVES.getOrDefault(round, List.of()).contains(wave)) return next ? 0xFF0000 : passed ? 0x5A5A5A : 0x783300;
            if (GIANT_WAVES.getOrDefault(round, List.of()).contains(wave)) return next ? 0x0099FF : passed ? 0x5A5A5A : 0x663399;
            if (OLD_ONE_WAVES.getOrDefault(round, List.of()).contains(wave)) return next ? 0x00FF00 : passed ? 0x5A5A5A : 0x006666;
        }
        return next ? 0xFFFF00 : passed ? 0x5A5A5A : 0x808080;
    }

    private static int nextWave(int[] times, int milliseconds) {
        for (int index = 0; index < times.length; index++) {
            if (milliseconds <= times[index] * 1_000) return index + 1;
        }
        return times.length;
    }

    private static String formatTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private static String trimDouble(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : String.format("%.2f", value);
    }

    private static int numberAfterColon(String text) {
        String[] split = text.split("[:：]", 2);
        if (split.length < 2) return -1;
        String digits = split[1].replaceAll("[^0-9]", "");
        return digits.isEmpty() ? -1 : Integer.parseInt(digits);
    }

    private static final class PatternQuote {
        private static String quote(String value) {
            return java.util.regex.Pattern.quote(value);
        }
    }
}
