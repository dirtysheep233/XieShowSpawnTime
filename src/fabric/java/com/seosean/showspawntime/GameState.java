package com.seosean.showspawntime;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GameState {
    private static final Integer[] DE_NORMAL_LEFT = {4,5,3,4,7,6,5,6,8,9,7,7,8,8,8,11,12,11,14,16,19,21,18,22,23,26,25,27,30,33};
    private static final Integer[] DE_HARD_LEFT = {4,6,4,5,8,10,6,7,10,9,8,8,10,13,8,12,14,13,19,19,21,22,22,24,23,29,25,29,31,33};
    private static final Integer[] DE_RIP_LEFT = {5,7,5,6,10,12,8,9,12,9,10,10,12,16,11,15,17,15,25,22,24,23,26,26,27,33,31,34,36,36};
    private static final Integer[] BB_NORMAL_LEFT = {5,6,7,6,4,5,6,5,7,9,9,10,10,11,11,10,11,10,12,12,13,13,14,14,16,12,14,18,21,23};
    private static final Integer[] BB_HARD_LEFT = {6,6,8,6,6,7,7,7,10,11,12,12,12,12,12,13,14,12,14,12,15,14,15,15,17,13,16,21,24,25};
    private static final Integer[] BB_RIP_LEFT = {6,8,8,8,7,7,10,9,11,15,12,12,12,13,13,13,16,12,14,14,18,16,16,18,25,15,20,25,25,27};

    private final ScoreboardSnapshot scoreboard = new ScoreboardSnapshot();
    private final Map<String, Long> fastReviveCooldowns = new ConcurrentHashMap<>();
    private ClientWorld lastWorld;
    private ZombiesMap map = ZombiesMap.NULL;
    private Difficulty difficulty = Difficulty.NORMAL;
    private int clientTicks;
    private int currentRound;
    private int[] roundTimes = new int[0];
    private boolean gameStarted;
    private long roundStartedNanos;
    private int gameMilliseconds;
    private int previousGameMilliseconds;
    private int predictionTick = -1;
    private int lightningUses;
    private int lightningResetTicks;
    private int lightningDisplayTicks;
    private boolean aaRoundTenGuardianSound;
    private String lastMessage = "";
    private long lastMessageNanos;
    private String lastTitle = "";
    private long lastTitleNanos;
    private PendingRound pendingRound;

    public void tick(MinecraftClient client) {
        clientTicks++;
        if (client.world != lastWorld) {
            onWorldChanged(client.world);
        }
        if (client.world == null || client.player == null) {
            return;
        }

        if (clientTicks % 5 == 0) {
            scoreboard.update(client);
            ShowSpawnTimeClient.LANG.detect(scoreboard);
        }
        resolvePendingRound(client);
        if (clientTicks % 20 == 0 || map == ZombiesMap.NULL) {
            ZombiesMap detected = ZombiesMap.detect(client, scoreboard);
            if (detected != ZombiesMap.NULL) {
                map = detected;
                roundTimes = map.roundTimes(currentRound);
            }
        }

        if (gameStarted) {
            previousGameMilliseconds = gameMilliseconds;
            gameMilliseconds = (int) ((System.nanoTime() - roundStartedNanos) / 1_000_000L);
            playWaveNotices();
        }

        fastReviveCooldowns.entrySet().removeIf(entry -> entry.getValue() <= System.nanoTime());
        if (lightningResetTicks > 0 && --lightningResetTicks == 0) {
            lightningUses = 0;
        }
        if (lightningDisplayTicks > 0) {
            lightningDisplayTicks--;
        }
        if (predictionTick >= 0 && --predictionTick == 0) {
            sendPowerupPrediction();
            predictionTick = -1;
        }

        ShowSpawnTimeClient.POWERUPS.tick(client, this);
        ShowSpawnTimeClient.DPS.tick();
    }

    public void onTitle(Text title) {
        String message = ScoreboardSnapshot.trim(title);
        long now = System.nanoTime();
        if (message.equals(lastTitle) && now - lastTitleNanos < 100_000_000L) {
            return;
        }
        lastTitle = message;
        lastTitleNanos = now;

        boolean finished = ShowSpawnTimeClient.LANG.contains(message, "zombies.game.youwin")
                || ShowSpawnTimeClient.LANG.contains(message, "zombies.game.gameover");
        if (!ShowSpawnTimeClient.LANG.isRoundTitle(message) && !finished) {
            return;
        }

        recordPreviousRound();

        MinecraftClient client = MinecraftClient.getInstance();
        scoreboard.update(client);
        ShowSpawnTimeClient.LANG.detect(scoreboard);

        if (finished) {
            pendingRound = null;
            gameStarted = false;
            previousGameMilliseconds = 0;
            gameMilliseconds = 0;
            currentRound = 0;
            roundTimes = new int[0];
            predictionTick = -1;
            map = ZombiesMap.NULL;
            difficulty = Difficulty.NORMAL;
            fastReviveCooldowns.clear();
            ShowSpawnTimeClient.POWERUPS.reset();
            ShowSpawnTimeClient.DPS.reset();
            return;
        }

        int round = ShowSpawnTimeClient.LANG.getRoundNumber(message);
        if (round <= 0) {
            return;
        }
        pendingRound = new PendingRound(round, now);
        resolvePendingRound(client);
    }

    private void resolvePendingRound(MinecraftClient client) {
        if (pendingRound == null) {
            return;
        }
        long now = System.nanoTime();
        if (now - pendingRound.receivedNanos() > 10_000_000_000L) {
            ShowSpawnTimeClient.LOGGER.warn("Discarding unresolved Zombies round {} after scoreboard timeout", pendingRound.round());
            pendingRound = null;
            return;
        }
        if (!scoreboard.isZombiesTitle()) {
            return;
        }
        ZombiesMap detectedMap = ZombiesMap.detect(client, scoreboard);
        if (detectedMap == ZombiesMap.NULL) {
            return;
        }

        int round = pendingRound.round();
        long receivedNanos = pendingRound.receivedNanos();
        pendingRound = null;
        currentRound = round;
        map = detectedMap;
        roundTimes = map.roundTimes(round);
        gameStarted = true;
        previousGameMilliseconds = 0;
        gameMilliseconds = (int) ((now - receivedNanos) / 1_000_000L);
        roundStartedNanos = receivedNanos;
        lightningUses = 0;
        aaRoundTenGuardianSound = false;
        ShowSpawnTimeClient.AUTO_SPLITS.onRoundStarted();
        ShowSpawnTimeClient.POWERUPS.onRoundStarted(round, map, gameMilliseconds);
        predictionTick = ShowSpawnTimeClient.CONFIG.powerupPredict ? 40 : -1;
    }

    public void onChat(Text text) {
        String message = ScoreboardSnapshot.trim(text);
        long now = System.nanoTime();
        if (message.equals(lastMessage) && now - lastMessageNanos < 100_000_000L) {
            return;
        }
        lastMessage = message;
        lastMessageNanos = now;
        if (message.isEmpty() || message.contains(":")) {
            return;
        }

        if (scoreboard.isZombiesTitle()) {
            if (ShowSpawnTimeClient.LANG.contains(message, "zombies.game.difficulty.hard")) {
                difficulty = Difficulty.HARD;
            } else if (ShowSpawnTimeClient.LANG.contains(message, "zombies.game.difficulty.rip")) {
                difficulty = Difficulty.RIP;
            }
        }

        if (isInZombies() && ShowSpawnTimeClient.LANG.contains(message, "zombies.game.revive")) {
            detectRevivedPlayer(message);
        }
        if (isInZombies() && (message.contains("!") || message.contains("！"))
                && ShowSpawnTimeClient.LANG.contains(message, "zombies.game.hasspawned")) {
            lightningUses = Math.max(0, lightningUses - 1);
        }

        PowerupManager.PowerupType type = ShowSpawnTimeClient.POWERUPS.onActivated(message, this);
        if (type != PowerupManager.PowerupType.NULL) {
            ShowSpawnTimeClient.DPS.onPowerup(type);
        }
        ShowSpawnTimeClient.DPS.onChat(message, isInZombies());
    }

    public void onSound(String identifier, float pitch) {
        String sound = identifier.toLowerCase();
        boolean startSound = sound.endsWith("entity.wither.spawn") || sound.endsWith("mob.wither.spawn");
        boolean guardianSound = sound.endsWith("entity.guardian.curse") || sound.endsWith("mob.guardian.curse");
        if (startSound || guardianSound && !aaRoundTenGuardianSound) {
            aaRoundTenGuardianSound = guardianSound;
            lightningUses = 0;
            gameStarted = true;
            if (lightningDisplayTicks > 100) {
                lightningDisplayTicks = 100;
            }
        } else if (sound.endsWith("entity.ender_dragon.death") || sound.endsWith("mob.enderdragon.end")) {
            aaRoundTenGuardianSound = false;
            lightningUses = 0;
            gameStarted = false;
            if (lightningDisplayTicks > 100) {
                lightningDisplayTicks = 100;
            }
        }

        if (ShowSpawnTimeClient.CONFIG.lightningRodQueue && isInZombies() && map == ZombiesMap.ALIEN_ARCADIUM
                && (sound.endsWith("entity.lightning_bolt.thunder") || sound.endsWith("ambient.weather.thunder"))
                && Math.abs(pitch - 2.0F) > 0.01F) {
            lightningUses = Math.min(4, lightningUses + 1);
            lightningResetTicks = 100;
            lightningDisplayTicks = 160;
        }
        if (isInZombies()) {
            ShowSpawnTimeClient.AUTO_SPLITS.onSound(identifier);
        }
        ShowSpawnTimeClient.DPS.onSound(identifier, pitch);
    }

    private void onWorldChanged(ClientWorld world) {
        lastWorld = world;
        scoreboard.clear();
        map = ZombiesMap.NULL;
        difficulty = Difficulty.NORMAL;
        currentRound = 0;
        roundTimes = new int[0];
        pendingRound = null;
        gameStarted = false;
        gameMilliseconds = 0;
        previousGameMilliseconds = 0;
        predictionTick = -1;
        lightningUses = 0;
        lightningResetTicks = 0;
        lightningDisplayTicks = 0;
        aaRoundTenGuardianSound = false;
        fastReviveCooldowns.clear();
        ShowSpawnTimeClient.POWERUPS.reset();
        ShowSpawnTimeClient.DPS.reset();
        ShowSpawnTimeClient.AUTO_SPLITS.reset();
    }

    private void playWaveNotices() {
        if (roundTimes.length == 0) {
            return;
        }
        int finalWave = roundTimes[roundTimes.length - 1];
        boolean waveSoundEnabled = map == ZombiesMap.ALIEN_ARCADIUM
                ? ShowSpawnTimeClient.CONFIG.playAaSound : ShowSpawnTimeClient.CONFIG.playDebbSound;
        if (waveSoundEnabled) {
            for (int time : roundTimes) {
                if (crossed(time * 1_000)) {
                    boolean last = time == finalWave;
                    ShowSpawnTimeClient.playConfiguredSound(
                            last ? ShowSpawnTimeClient.CONFIG.finalWaveSound : ShowSpawnTimeClient.CONFIG.precededWaveSound,
                            (float) (last ? ShowSpawnTimeClient.CONFIG.finalWavePitch : ShowSpawnTimeClient.CONFIG.precededWavePitch));
                    break;
                }
            }
        }
        if (ShowSpawnTimeClient.CONFIG.debbCountdown
                && (map == ZombiesMap.DEAD_END || map == ZombiesMap.BAD_BLOOD)) {
            for (int seconds = 3; seconds >= 1; seconds--) {
                if (crossed((finalWave - seconds) * 1_000)) {
                    ShowSpawnTimeClient.playConfiguredSound(ShowSpawnTimeClient.CONFIG.countdownSound,
                            (float) ShowSpawnTimeClient.CONFIG.countdownPitch);
                    break;
                }
            }
        }
    }

    private boolean crossed(int milliseconds) {
        return previousGameMilliseconds < milliseconds && gameMilliseconds >= milliseconds;
    }

    private void recordPreviousRound() {
        if (currentRound <= 0 || map == ZombiesMap.NULL) {
            return;
        }
        if (ShowSpawnTimeClient.CONFIG.cleanUpTime && roundTimes.length > 0) {
            double cleanup = gameMilliseconds / 1000.0 - roundTimes[roundTimes.length - 1];
            String value = cleanup < 0 ? "--" : new DecimalFormat("#.##").format(cleanup);
            MutableText copyText = SstI18n.text("message.showspawntime.cleanup_time", value);
            sendCopyable(SstI18n.text("message.showspawntime.cleanup_time",
                            Text.literal(value).formatted(Formatting.RED, Formatting.BOLD))
                            .formatted(Formatting.YELLOW), copyText.getString());
        }

        String mode = map == ZombiesMap.ALIEN_ARCADIUM
                ? ShowSpawnTimeClient.CONFIG.aaRoundsRecord : ShowSpawnTimeClient.CONFIG.debbRoundsRecord;
        if (!shouldRecord(mode, currentRound, map)) {
            return;
        }
        boolean chinese = SstI18n.ZH_CN.equals(SstI18n.language());
        RoundStats roundStats = extractRoundStats(chinese);
        if (roundStats == null) {
            return;
        }
        MutableText roundMessage;
        String copy;
        if (SstI18n.ZH_CN.equals(SstI18n.language())) {
            roundMessage = SstI18n.text("message.showspawntime.round_completed",
                    Text.literal(Integer.toString(currentRound)).formatted(Formatting.RED),
                    Text.literal(roundStats.time()).formatted(Formatting.GREEN),
                    Text.literal(roundStats.kills()).formatted(Formatting.GREEN)).formatted(Formatting.YELLOW);
            copy = SstI18n.string("message.showspawntime.round_completed",
                    currentRound, roundStats.time(), roundStats.kills()).stripLeading();
        } else {
            roundMessage = SstI18n.text("message.showspawntime.round_completed",
                    Text.literal(Integer.toString(currentRound)).formatted(Formatting.RED),
                    Text.literal(roundStats.time()).formatted(Formatting.GREEN)).formatted(Formatting.YELLOW);
            copy = SstI18n.string("message.showspawntime.round_completed",
                    currentRound, roundStats.time()).stripLeading();
        }
        MutableText bar = Text.literal("▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ ")
                .formatted(Formatting.GREEN, Formatting.BOLD);
        ShowSpawnTimeClient.sendLocalMessage(bar);
        sendCopyable(roundMessage, copy);
        ShowSpawnTimeClient.sendLocalMessage(bar);
    }

    private static boolean shouldRecord(String mode, int round, ZombiesMap map) {
        String normalized = mode == null ? "OFF" : mode.toUpperCase();
        if (normalized.contains("OFF")) return false;
        if (map == ZombiesMap.ALIEN_ARCADIUM && (round == 10 || round == 21 || round == 105)) return false;
        if (map != ZombiesMap.ALIEN_ARCADIUM && round % 10 == 0 && round <= 40) return false;
        if (normalized.contains("QUINTUPLE")) return round % 5 == 1;
        if (normalized.contains("TENFOLD")) return round % 10 == 1;
        return true;
    }

    private RoundStats extractRoundStats(boolean requireKills) {
        List<String> lines = scoreboard.lines();
        if (lines.size() < 3) {
            return null;
        }
        String statsLine = lines.get(lines.size() - 3).replace("§a", "").replace("👿", "").trim();
        String[] valueParts = statsLine.contains("：") ? statsLine.split("：", 2) : statsLine.split(" ", 2);
        if (valueParts.length < 2) {
            return null;
        }
        String fullValue = valueParts[1].trim();
        if (!requireKills) {
            return new RoundStats(fullValue, "");
        }
        Matcher timeMatcher = Pattern.compile("(?:\\d{1,2}:)?\\d{1,2}:\\d{2}(?:\\.\\d+)?").matcher(fullValue);
        if (!timeMatcher.find()) {
            return null;
        }
        String time = timeMatcher.group();
        String remaining = fullValue.substring(timeMatcher.end());
        Matcher killsMatcher = Pattern.compile("(?i)(?:kills?|击杀|擊殺)\\s*[：:]?\\s*(\\d+)").matcher(remaining);
        if (!killsMatcher.find()) {
            Matcher numberMatcher = Pattern.compile("\\d+").matcher(remaining);
            if (!numberMatcher.find()) {
                return null;
            }
            return new RoundStats(time, numberMatcher.group());
        }
        return new RoundStats(time, killsMatcher.group(1));
    }

    private static void sendCopyable(MutableText message, String copy) {
        message.setStyle(message.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(copy))
                .withHoverEvent(new HoverEvent.ShowText(SstI18n.text("tooltip.showspawntime.copy").formatted(Formatting.GREEN))));
        ShowSpawnTimeClient.sendLocalMessage(message);
    }

    private void sendPowerupPrediction() {
        List<Text> predictions = ShowSpawnTimeClient.POWERUPS.predictions(currentRound);
        if (predictions.isEmpty()) {
            return;
        }
        MutableText text = Text.literal("[").formatted(Formatting.GOLD)
                .append(Text.literal("ShowSpawnTime").formatted(Formatting.WHITE))
                .append(Text.literal("] ").formatted(Formatting.GOLD));
        for (int index = 0; index < predictions.size(); index++) {
            text.append(predictions.get(index).copy());
            text.append(Text.literal(index + 1 == predictions.size() ? "." : ", ").formatted(Formatting.WHITE));
        }
        ShowSpawnTimeClient.sendLocalMessage(text);
    }

    private void detectRevivedPlayer(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        List<String> matches = new ArrayList<>();
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            String name = player.getName().getString();
            if (message.contains(name)) {
                matches.add(name);
            }
        }
        if (!matches.isEmpty() && ShowSpawnTimeClient.CONFIG.fastRevivePosition != SstConfig.FastRevivePosition.OFF) {
            matches.sort((left, right) -> Integer.compare(right.length(), left.length()));
            fastReviveCooldowns.put(matches.get(0), System.nanoTime() + 5_000_000_000L);
        }
    }

    public int waveThreeLeft() {
        if (currentRound < 1 || currentRound > 30) {
            return 0;
        }
        Integer[] values;
        if (map == ZombiesMap.DEAD_END) {
            values = switch (difficulty) {
                case NORMAL -> DE_NORMAL_LEFT;
                case HARD -> DE_HARD_LEFT;
                case RIP -> DE_RIP_LEFT;
            };
        } else if (map == ZombiesMap.BAD_BLOOD) {
            values = switch (difficulty) {
                case NORMAL -> BB_NORMAL_LEFT;
                case HARD -> BB_HARD_LEFT;
                case RIP -> BB_RIP_LEFT;
            };
        } else {
            return 0;
        }
        return values[currentRound - 1];
    }

    private record RoundStats(String time, String kills) {
    }

    private record PendingRound(int round, long receivedNanos) {
    }

    public ScoreboardSnapshot scoreboard() { return scoreboard; }
    public ZombiesMap map() { return map; }
    public Difficulty difficulty() { return difficulty; }
    public int clientTicks() { return clientTicks; }
    public int currentRound() { return currentRound; }
    public int[] roundTimes() { return roundTimes.clone(); }
    public int gameMilliseconds() { return gameMilliseconds; }
    public boolean isInZombies() { return scoreboard.isInZombiesGame(); }
    public boolean isZombiesTitle() { return scoreboard.isZombiesTitle(); }
    public int lightningUses() { return lightningUses; }
    public int lightningDisplayTicks() { return lightningDisplayTicks; }
    public Map<String, Long> fastReviveCooldowns() { return Map.copyOf(fastReviveCooldowns); }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public enum Difficulty {
        NORMAL,
        HARD,
        RIP
    }
}
