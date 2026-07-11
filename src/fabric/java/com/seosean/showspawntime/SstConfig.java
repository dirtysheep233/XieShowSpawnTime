package com.seosean.showspawntime;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class SstConfig {
    private final Path file = FabricLoader.getInstance().getConfigDir()
            .resolve("showspawntime/showspawntime.properties");

    public double xSpawnTime = -1.0;
    public double ySpawnTime = -1.0;
    public double xPowerup = -1.0;
    public double yPowerup = -1.0;
    public double xDpsCounter = -1.0;
    public double yDpsCounter = -1.0;
    public double xAutoSplits = -1.0;
    public double yAutoSplits = -1.0;

    public String language = SstI18n.ZH_CN;
    public boolean autoSplitsEnabled = true;
    public AutoSplitsMode autoSplitsMode = AutoSplitsMode.INTERNAL;
    public String autoSplitsHost = "localhost";
    public int autoSplitsPort = 16834;

    public boolean playAaSound = true;
    public boolean playDebbSound = true;
    public String precededWaveSound = "note.pling";
    public String finalWaveSound = "random.orb";
    public double precededWavePitch = 2.0;
    public double finalWavePitch = 0.5;
    public boolean colorAlert = true;
    public String aaRoundsRecord = "ALL";
    public String debbRoundsRecord = "ALL";
    public boolean cleanUpTime = true;
    public boolean powerupAlert = true;
    public boolean powerupPredict = true;
    public boolean powerupCountdown = true;
    public boolean powerupNameTagShadow = false;
    public boolean lightningRodQueue = true;
    public boolean wave3LeftNotice = true;
    public boolean playerHealthNotice = true;
    public boolean dpsCounter = true;
    public boolean debbCountdown = false;
    public String countdownSound = "note.pling";
    public double countdownPitch = 1.5;
    public FastRevivePosition fastRevivePosition = FastRevivePosition.BEHIND;

    public boolean playerInvisible;

    public void load() {
        Properties properties = new Properties();
        if (Files.isRegularFile(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                properties.load(input);
            } catch (IOException e) {
                ShowSpawnTimeClient.LOGGER.warn("Failed to load ShowSpawnTime config", e);
            }
        } else {
            importLegacyConfig(properties);
        }

        xSpawnTime = getDouble(properties, "XSpawnTime", xSpawnTime);
        ySpawnTime = getDouble(properties, "YSpawnTime", ySpawnTime);
        xPowerup = getDouble(properties, "XPowerup", xPowerup);
        yPowerup = getDouble(properties, "YPowerup", yPowerup);
        xDpsCounter = getDouble(properties, "XDPSCounter", xDpsCounter);
        yDpsCounter = getDouble(properties, "YDPSCounter", yDpsCounter);
        xAutoSplits = getDouble(properties, "XAutoSplits", xAutoSplits);
        yAutoSplits = getDouble(properties, "YAutoSplits", yAutoSplits);
        language = normalizeLanguage(properties.getProperty("Language", language));
        autoSplitsEnabled = getBoolean(properties, "AutoSplits Enabled", autoSplitsEnabled);
        autoSplitsMode = getEnum(properties, "AutoSplits Mode", autoSplitsMode);
        autoSplitsHost = properties.getProperty("AutoSplits Host", autoSplitsHost).trim();
        autoSplitsPort = getInt(properties, "AutoSplits Port", autoSplitsPort, 1, 65535);

        playAaSound = getBoolean(properties, "Toggle AA Sound", playAaSound);
        playDebbSound = getBoolean(properties, "Toggle DE BB Sound", playDebbSound);
        precededWaveSound = properties.getProperty("Preceded Wave Sound", precededWaveSound);
        finalWaveSound = properties.getProperty("Final Wave Sound", finalWaveSound);
        precededWavePitch = getDouble(properties, "Preceded Wave Pitch", precededWavePitch);
        finalWavePitch = getDouble(properties, "Final Wave Pitch", finalWavePitch);
        colorAlert = getBoolean(properties, "AA Boss Color Alert", colorAlert);
        aaRoundsRecord = properties.getProperty("AA Rounds Record Timing", aaRoundsRecord);
        debbRoundsRecord = properties.getProperty("DE BB Rounds Record Timing", debbRoundsRecord);
        cleanUpTime = getBoolean(properties, "Clean Up Time Tips", cleanUpTime);
        powerupAlert = getBoolean(properties, "Powerup Alert", powerupAlert);
        powerupPredict = getBoolean(properties, "Powerup Predict", powerupPredict);
        powerupCountdown = getBoolean(properties, "Powerup Count Down", powerupCountdown);
        powerupNameTagShadow = getBoolean(properties, "Powerup NameTag Shadow", powerupNameTagShadow);
        lightningRodQueue = getBoolean(properties, "LR Queue Helper", lightningRodQueue);
        wave3LeftNotice = getBoolean(properties, "Wave 3rd Left Notice", wave3LeftNotice);
        playerHealthNotice = getBoolean(properties, "Player Health Notice", playerHealthNotice);
        dpsCounter = getBoolean(properties, "Individual DPS Counter", dpsCounter);
        debbCountdown = getBoolean(properties, "Toggle DE BB W3 Count Down Sound", debbCountdown);
        countdownSound = properties.getProperty("Count Down Sound", countdownSound);
        countdownPitch = getDouble(properties, "Count Down Pitch", countdownPitch);
        fastRevivePosition = getEnum(properties, "Fast Revive Cool Down", fastRevivePosition);
        playerInvisible = getBoolean(properties, "Player Invisible", playerInvisible);
        save();
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("XSpawnTime", Double.toString(xSpawnTime));
        properties.setProperty("YSpawnTime", Double.toString(ySpawnTime));
        properties.setProperty("XPowerup", Double.toString(xPowerup));
        properties.setProperty("YPowerup", Double.toString(yPowerup));
        properties.setProperty("XDPSCounter", Double.toString(xDpsCounter));
        properties.setProperty("YDPSCounter", Double.toString(yDpsCounter));
        properties.setProperty("XAutoSplits", Double.toString(xAutoSplits));
        properties.setProperty("YAutoSplits", Double.toString(yAutoSplits));
        properties.setProperty("Language", language);
        properties.setProperty("AutoSplits Enabled", Boolean.toString(autoSplitsEnabled));
        properties.setProperty("AutoSplits Mode", autoSplitsMode.name());
        properties.setProperty("AutoSplits Host", autoSplitsHost);
        properties.setProperty("AutoSplits Port", Integer.toString(autoSplitsPort));
        properties.setProperty("Toggle AA Sound", Boolean.toString(playAaSound));
        properties.setProperty("Toggle DE BB Sound", Boolean.toString(playDebbSound));
        properties.setProperty("Preceded Wave Sound", precededWaveSound);
        properties.setProperty("Final Wave Sound", finalWaveSound);
        properties.setProperty("Preceded Wave Pitch", Double.toString(precededWavePitch));
        properties.setProperty("Final Wave Pitch", Double.toString(finalWavePitch));
        properties.setProperty("AA Boss Color Alert", Boolean.toString(colorAlert));
        properties.setProperty("AA Rounds Record Timing", aaRoundsRecord);
        properties.setProperty("DE BB Rounds Record Timing", debbRoundsRecord);
        properties.setProperty("Clean Up Time Tips", Boolean.toString(cleanUpTime));
        properties.setProperty("Powerup Alert", Boolean.toString(powerupAlert));
        properties.setProperty("Powerup Predict", Boolean.toString(powerupPredict));
        properties.setProperty("Powerup Count Down", Boolean.toString(powerupCountdown));
        properties.setProperty("Powerup NameTag Shadow", Boolean.toString(powerupNameTagShadow));
        properties.setProperty("LR Queue Helper", Boolean.toString(lightningRodQueue));
        properties.setProperty("Wave 3rd Left Notice", Boolean.toString(wave3LeftNotice));
        properties.setProperty("Player Health Notice", Boolean.toString(playerHealthNotice));
        properties.setProperty("Individual DPS Counter", Boolean.toString(dpsCounter));
        properties.setProperty("Toggle DE BB W3 Count Down Sound", Boolean.toString(debbCountdown));
        properties.setProperty("Count Down Sound", countdownSound);
        properties.setProperty("Count Down Pitch", Double.toString(countdownPitch));
        properties.setProperty("Fast Revive Cool Down", fastRevivePosition.name());
        properties.setProperty("Player Invisible", Boolean.toString(playerInvisible));

        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "ShowSpawnTime Fabric configuration");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            ShowSpawnTimeClient.LOGGER.warn("Failed to save ShowSpawnTime config", e);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupError) {
                ShowSpawnTimeClient.LOGGER.debug("Failed to clean temporary ShowSpawnTime config", cleanupError);
            }
        }
    }

    private void importLegacyConfig(Properties target) {
        Path legacy = FabricLoader.getInstance().getConfigDir().resolve("showspawntime/showspawntime.cfg");
        if (!Files.isRegularFile(legacy)) {
            return;
        }
        try {
            for (String rawLine : Files.readAllLines(legacy)) {
                String line = rawLine.trim();
                int separator = line.indexOf('=');
                if (separator < 0 || line.startsWith("#")) {
                    continue;
                }
                String key = line.substring(0, separator).trim();
                int typeSeparator = key.indexOf(':');
                if (typeSeparator >= 0) {
                    key = key.substring(typeSeparator + 1);
                }
                if (key.length() >= 2 && key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }
                key = switch (key) {
                    case "Toggle DE/BB Sound" -> "Toggle DE BB Sound";
                    case "DE/BB Rounds Record Timing" -> "DE BB Rounds Record Timing";
                    default -> key;
                };
                String value = line.substring(separator + 1).trim();
                target.setProperty(key, value);
            }
            ShowSpawnTimeClient.LOGGER.info("Imported legacy ShowSpawnTime configuration");
        } catch (IOException e) {
            ShowSpawnTimeClient.LOGGER.warn("Failed to import legacy ShowSpawnTime config", e);
        }
    }

    public void resetHudPositions() {
        xSpawnTime = -1.0;
        ySpawnTime = -1.0;
        xPowerup = -1.0;
        yPowerup = -1.0;
        xDpsCounter = -1.0;
        yDpsCounter = -1.0;
        xAutoSplits = -1.0;
        yAutoSplits = -1.0;
        save();
    }

    private static String normalizeLanguage(String value) {
        return SstI18n.EN_US.equalsIgnoreCase(value) ? SstI18n.EN_US : SstI18n.ZH_CN;
    }

    private static boolean getBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if (value == null) return fallback;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        return fallback;
    }

    private static double getDouble(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int getInt(Properties properties, String key, int fallback, int minimum, int maximum) {
        try {
            return Math.max(minimum, Math.min(maximum,
                    Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)))));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static <T extends Enum<T>> T getEnum(Properties properties, String key, T fallback) {
        try {
            @SuppressWarnings("unchecked")
            Class<T> type = (Class<T>) fallback.getDeclaringClass();
            return Enum.valueOf(type, properties.getProperty(key, fallback.name()).trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public enum AutoSplitsMode {
        INTERNAL,
        LIVESPLIT;

        public AutoSplitsMode next() {
            return this == INTERNAL ? LIVESPLIT : INTERNAL;
        }
    }

    public enum FastRevivePosition {
        OFF,
        FRONT,
        MID,
        BEHIND;

        public FastRevivePosition next() {
            FastRevivePosition[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
}
