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
    public String language = SstI18n.ZH_CN;
    public boolean playWaveSound = true;
    public String precededWaveSound = "note.pling";
    public String finalWaveSound = "random.orb";
    public double precededWavePitch = 2.0;
    public double finalWavePitch = 0.5;
    public boolean cleanUpTime = true;
 
    public void load() {
        Properties properties = new Properties();
        if (Files.isRegularFile(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                properties.load(input);
            } catch (IOException e) {
                ShowSpawnTimeClient.LOGGER.warn("Failed to load ShowSpawnTime config", e);
            }
        }
        xSpawnTime = getDouble(properties, "XSpawnTime", xSpawnTime);
        ySpawnTime = getDouble(properties, "YSpawnTime", ySpawnTime);
        language = properties.getProperty("Language", language).equalsIgnoreCase(SstI18n.EN_US) ? SstI18n.EN_US : SstI18n.ZH_CN;
        playWaveSound = getBoolean(properties, "Toggle Wave Sound", playWaveSound);
        precededWaveSound = properties.getProperty("Preceded Wave Sound", precededWaveSound);
        finalWaveSound = properties.getProperty("Final Wave Sound", finalWaveSound);
        precededWavePitch = getDouble(properties, "Preceded Wave Pitch", precededWavePitch);
        finalWavePitch = getDouble(properties, "Final Wave Pitch", finalWavePitch);
        cleanUpTime = getBoolean(properties, "Clean Up Time Tips", cleanUpTime);
        save();
    }
 
    public void save() {
        Properties properties = new Properties();
        properties.setProperty("XSpawnTime", Double.toString(xSpawnTime));
        properties.setProperty("YSpawnTime", Double.toString(ySpawnTime));
        properties.setProperty("Language", language);
        properties.setProperty("Toggle Wave Sound", Boolean.toString(playWaveSound));
        properties.setProperty("Preceded Wave Sound", precededWaveSound);
        properties.setProperty("Final Wave Sound", finalWaveSound);
        properties.setProperty("Preceded Wave Pitch", Double.toString(precededWavePitch));
        properties.setProperty("Final Wave Pitch", Double.toString(finalWavePitch));
        properties.setProperty("Clean Up Time Tips", Boolean.toString(cleanUpTime));
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
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
        }
    }
 
    public void resetHudPositions() {
        xSpawnTime = -1.0;
        ySpawnTime = -1.0;
        save();
    }
 
    private static boolean getBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return fallback;
    }
 
    private static double getDouble(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException ignored) { return fallback; }
    }
}