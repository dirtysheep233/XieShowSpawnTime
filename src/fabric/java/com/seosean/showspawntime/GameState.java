package com.seosean.showspawntime;
 
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
 
import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
public final class GameState {
    private final ScoreboardSnapshot scoreboard = new ScoreboardSnapshot();
    private net.minecraft.client.world.ClientWorld lastWorld;
    private ZombiesMap map = ZombiesMap.NULL;
    private int clientTicks;
    private int currentRound;
    private int[] roundTimes = new int[0];
    private boolean gameStarted;
    private long roundStartedNanos;
    private int gameMilliseconds;
    private int previousGameMilliseconds;
    private String lastMessage = "";
    private long lastMessageNanos;
    private int debugTicks;
 
    public void tick(MinecraftClient client) {
        clientTicks++;
        if (client.world != lastWorld) onWorldChanged(client.world);
        if (client.world == null || client.player == null) return;
 
        if (clientTicks % 5 == 0) {
            scoreboard.update(client);
            ShowSpawnTimeClient.LANG.detect(scoreboard);
        }
 
        if (clientTicks % 20 == 0 || map == ZombiesMap.NULL) {
            ZombiesMap detected = ZombiesMap.detect(client, scoreboard);
            if (detected != ZombiesMap.NULL) map = detected;
        }
 
        if (scoreboard.isInZombiesGame() && map != ZombiesMap.NULL) {
            int roundFromScoreboard = getRoundFromScoreboard();
            if (!gameStarted) {
                gameStarted = true;
                currentRound = roundFromScoreboard;
                roundTimes = map.roundTimes(currentRound);
                roundStartedNanos = System.nanoTime();
                gameMilliseconds = 0;
                previousGameMilliseconds = 0;
            } else if (roundFromScoreboard != currentRound && roundFromScoreboard > 0) {
                recordPreviousRound();
                currentRound = roundFromScoreboard;
                roundTimes = map.roundTimes(currentRound);
                roundStartedNanos = System.nanoTime();
                previousGameMilliseconds = 0;
                gameMilliseconds = 0;
            }
        } else {
            if (gameStarted) recordPreviousRound();
            gameStarted = false;
            currentRound = 0;
            roundTimes = new int[0];
            map = ZombiesMap.NULL;
            gameMilliseconds = 0;
            previousGameMilliseconds = 0;
        }
 
        if (gameStarted) {
            previousGameMilliseconds = gameMilliseconds;
            gameMilliseconds = (int) ((System.nanoTime() - roundStartedNanos) / 1_000_000L);
            playWaveNotices();
        }
 
        if (ShowSpawnTimeClient.DEBUG) {
            debugTicks++;
            if (debugTicks >= 100) {
                debugTicks = 0;
                ShowSpawnTimeClient.sendLocalMessage(Text.literal("[SST Debug] "
                        + "isZombies=" + scoreboard.isInZombiesGame()
                        + " | map=" + map.name()
                        + " | round=" + currentRound
                        + " | ms=" + gameMilliseconds
                        + " | started=" + gameStarted
                        + " | title=" + scoreboard.title()
                        + " | lines=" + scoreboard.lines()
                ).formatted(Formatting.YELLOW));
            }
        }
    }
 
private static final Pattern ROUND_PATTERN = 
    Pattern.compile("第(\\d+)回合|Round\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

private int getRoundFromScoreboard() {
    for (String line : scoreboard.lines()) {
        Matcher matcher = ROUND_PATTERN.matcher(line);
        if (matcher.find()) {
            // 遍历所有捕获组（从1开始），返回第一个非空的数字
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String group = matcher.group(i);
                if (group != null) {
                    return Integer.parseInt(group);
                }
            }
        }
    }
    return currentRound;
}
 
    public void onTitle(Text title) {
        String message = ScoreboardSnapshot.trim(title);
        boolean finished = ShowSpawnTimeClient.LANG.contains(message, "zombies.game.youwin")
                || ShowSpawnTimeClient.LANG.contains(message, "zombies.game.gameover");
        if (finished) {
            gameStarted = false;
            gameMilliseconds = 0;
            previousGameMilliseconds = 0;
            currentRound = 0;
            roundTimes = new int[0];
            map = ZombiesMap.NULL;
        }
    }
 
    public void onChat(Text text) {
        String message = ScoreboardSnapshot.trim(text);
        long now = System.nanoTime();
        if (message.equals(lastMessage) && now - lastMessageNanos < 100_000_000L) return;
        lastMessage = message;
        lastMessageNanos = now;
    }
 
    public void onSound(String identifier, float pitch) {}
 
    private void onWorldChanged(net.minecraft.client.world.ClientWorld world) {
        lastWorld = world;
        scoreboard.clear();
        map = ZombiesMap.NULL;
        currentRound = 0;
        roundTimes = new int[0];
        gameStarted = false;
        gameMilliseconds = 0;
        previousGameMilliseconds = 0;
    }
 
    private void playWaveNotices() {
        if (roundTimes.length == 0) return;
        if (!ShowSpawnTimeClient.CONFIG.playWaveSound) return;
        for (int time : roundTimes) {
            int waveMs = time * 1000;
            // 仙帝定制"叮""叮""叮"
            for (int pre = 3; pre >= 1; pre--) {
                int preMs = waveMs - pre * 1000;
                if (preMs > 0 && crossed(preMs)) {
                    ShowSpawnTimeClient.playConfiguredSound(
                            ShowSpawnTimeClient.CONFIG.precededWaveSound,
                            (float) ShowSpawnTimeClient.CONFIG.precededWavePitch);
                }
            }
            // "咚"
            if (crossed(waveMs)) {
                ShowSpawnTimeClient.playConfiguredSound(
                        ShowSpawnTimeClient.CONFIG.finalWaveSound,
                        (float) ShowSpawnTimeClient.CONFIG.finalWavePitch);
            }
        }
    }
 
    private boolean crossed(int milliseconds) {
        return previousGameMilliseconds < milliseconds && gameMilliseconds >= milliseconds;
    }
 
    private void recordPreviousRound() {
        if (currentRound <= 0 || map == ZombiesMap.NULL) return;
        if (ShowSpawnTimeClient.CONFIG.cleanUpTime && roundTimes.length > 0) {
            double cleanup = gameMilliseconds / 1000.0 - roundTimes[roundTimes.length - 1];
            String value = cleanup < 0 ? "--" : new DecimalFormat("#.##").format(cleanup);
            MutableText copyText = SstI18n.text("message.showspawntime.cleanup_time", value);
            sendCopyable(SstI18n.text("message.showspawntime.cleanup_time",
                            Text.literal(value).formatted(Formatting.RED, Formatting.BOLD))
                            .formatted(Formatting.YELLOW), copyText.getString());
        }
    }
 
    private static void sendCopyable(MutableText message, String copy) {
        message.setStyle(message.getStyle()
                .withClickEvent(new ClickEvent.CopyToClipboard(copy))
                .withHoverEvent(new HoverEvent.ShowText(SstI18n.text("tooltip.showspawntime.copy").formatted(Formatting.GREEN))));
        ShowSpawnTimeClient.sendLocalMessage(message);
    }
 
    public ScoreboardSnapshot scoreboard() { return scoreboard; }
    public ZombiesMap map() { return map; }
    public int currentRound() { return currentRound; }
    public int[] roundTimes() { return roundTimes.clone(); }
    public int gameMilliseconds() { return gameMilliseconds; }
    public boolean isInZombies() { return scoreboard.isInZombiesGame(); }
    public boolean isZombiesTitle() { return scoreboard.isZombiesTitle(); }
}