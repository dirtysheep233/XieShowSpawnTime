package com.seosean.showspawntime;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class AutoSplits {
    private boolean guardianTriggered;
    private boolean running;
    private long segmentStartedNanos;

    public void onRoundStarted() {
        guardianTriggered = false;
    }

    public void onSound(String identifier) {
        if (!ShowSpawnTimeClient.CONFIG.autoSplitsEnabled) {
            return;
        }
        String sound = identifier.toLowerCase();
        boolean guardian = sound.endsWith("entity.guardian.curse") || sound.endsWith("mob.guardian.curse");
        boolean trigger = sound.endsWith("entity.wither.spawn") || sound.endsWith("mob.wither.spawn")
                || sound.endsWith("entity.ender_dragon.death") || sound.endsWith("mob.enderdragon.end")
                || guardian && !guardianTriggered;
        if (trigger) {
            guardianTriggered = guardian;
            startOrSplit();
        }
    }

    public void reset() {
        guardianTriggered = false;
        running = false;
        segmentStartedNanos = 0L;
    }

    public boolean toggle() {
        ShowSpawnTimeClient.CONFIG.autoSplitsEnabled = !ShowSpawnTimeClient.CONFIG.autoSplitsEnabled;
        ShowSpawnTimeClient.CONFIG.save();
        return ShowSpawnTimeClient.CONFIG.autoSplitsEnabled;
    }

    public boolean enabled() {
        return ShowSpawnTimeClient.CONFIG.autoSplitsEnabled;
    }

    public boolean running() {
        return running && ShowSpawnTimeClient.CONFIG.autoSplitsMode == SstConfig.AutoSplitsMode.INTERNAL;
    }

    public long elapsedMillis() {
        return running ? (System.nanoTime() - segmentStartedNanos) / 1_000_000L : 0L;
    }

    public String formattedTime() {
        long millis = elapsedMillis();
        return String.format("%d:%02d:%d", millis / 60_000L, millis % 60_000L / 1_000L, millis % 1_000L / 100L);
    }

    private void startOrSplit() {
        if (ShowSpawnTimeClient.CONFIG.autoSplitsMode == SstConfig.AutoSplitsMode.INTERNAL) {
            running = true;
            segmentStartedNanos = System.nanoTime();
            return;
        }
        String host = ShowSpawnTimeClient.CONFIG.autoSplitsHost;
        int port = ShowSpawnTimeClient.CONFIG.autoSplitsPort;
        Thread.ofVirtual().name("ShowSpawnTime LiveSplit").start(() -> sendLiveSplit(host, port));
    }

    private static void sendLiveSplit(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3_000);
            socket.setSoTimeout(3_000);
            try (OutputStreamWriter writer = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write("startorsplit\r\n");
                writer.flush();
            }
        } catch (IOException e) {
            ShowSpawnTimeClient.LOGGER.warn("Failed to send LiveSplit command", e);
            net.minecraft.client.MinecraftClient.getInstance().execute(() -> ShowSpawnTimeClient.sendLocalMessage(
                    SstI18n.text("message.showspawntime.autosplits_connection_failed").formatted(Formatting.RED)));
        }
    }
}
