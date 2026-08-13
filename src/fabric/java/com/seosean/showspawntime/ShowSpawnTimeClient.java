package com.seosean.showspawntime;
 
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;
 
public final class ShowSpawnTimeClient implements ClientModInitializer {
    public static final String MOD_ID = "fzb_showspawntime";
    public static final String VERSION = "2.1.4";
    public static final String EMOJI_REGEX = "(?:[\\x{1F300}-\\x{1FAFF}]|[\\x{2600}-\\x{27BF}]|[\\x{2194}-\\x{21AA}]|[\\x{2B05}-\\x{2B55}])\\uFE0F?";
    public static final Logger LOGGER = LoggerFactory.getLogger("ShowSpawnTime");
    public static final SstConfig CONFIG = new SstConfig();
    public static final ZombiesLanguage LANG = new ZombiesLanguage();
    public static final GameState STATE = new GameState();
    public static final OverlayRenderer OVERLAY = new OverlayRenderer();
    public static boolean DEBUG = false;
 
    private static KeyBinding configKey;
    private static boolean updateCheckScheduled;
    private static boolean wasConnected;
    private static boolean configTipShown;
    private static int updateCheckTicks;
    private static Screen pendingScreen;
 
    @Override
    public void onInitializeClient() {
        LANG.load();
        CONFIG.load();
        SstI18n.load(CONFIG.language);
        configKey = KeyBindingHelper.registerKeyBinding(
                KeyBindingCompat.create("key.showspawntime.open_config", GLFW.GLFW_KEY_UNKNOWN));
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
        registerCommands();
        LOGGER.info("ShowSpawnTime {} initialized for Fabric 1.21.8", VERSION);
    }
 
    private void onEndTick(MinecraftClient client) {
        STATE.tick(client);
        if (pendingScreen != null) {
            Screen screen = pendingScreen;
            pendingScreen = null;
            client.setScreen(screen);
        }
        boolean connected = client.world != null && client.player != null && !client.isInSingleplayer();
        if (connected && !wasConnected && !updateCheckScheduled) {
            updateCheckScheduled = true;
            updateCheckTicks = 60;
        }
        wasConnected = connected;
        if (!configTipShown && connected && STATE.isZombiesTitle()) {
            configTipShown = true;
            MutableText message = Text.literal("ShowSpawnTime: ").formatted(Formatting.AQUA, Formatting.BOLD)
                    .append(clickableTip("message.showspawntime.config_tip.hud",
                            "tooltip.showspawntime.config_tip.hud", "/ssthud", Formatting.GOLD, true));
            sendLocalMessage(message);
        }
        if (updateCheckScheduled && --updateCheckTicks <= 0) {
            updateCheckScheduled = false;
            checkUpdates(false);
        }
        while (configKey.wasPressed()) {
            if (client.currentScreen == null) client.setScreen(new SstConfigScreen(null));
        }
    }
 
    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("sstconfig").executes(context -> {
                pendingScreen = new SstConfigScreen(null);
                return 1;
            }));
            dispatcher.register(literal("ssthud").executes(context -> {
                pendingScreen = new SstHudEditorScreen(null);
                return 1;
            }));
            dispatcher.register(literal("sst")
                    .then(literal("debug").executes(context -> {
                        DEBUG = !DEBUG;
                        sendLocalMessage(Text.literal("[SST] Debug: " + (DEBUG ? "ON" : "OFF")).formatted(DEBUG ? Formatting.GREEN : Formatting.RED));
                        return 1;
                    }))
                    .then(literal("sound").executes(context -> {
                        CONFIG.playWaveSound = !CONFIG.playWaveSound;
                        CONFIG.save();
                        sendLocalMessage(Text.literal("[SST] Sound: " + (CONFIG.playWaveSound ? "ON" : "OFF")).formatted(CONFIG.playWaveSound ? Formatting.GREEN : Formatting.RED));
                        return 1;
                    }))
                    .then(literal("lang")
                            .then(literal("zh_cn").executes(context -> setLanguage(SstI18n.ZH_CN)))
                            .then(literal("en_us").executes(context -> setLanguage(SstI18n.EN_US)))));
        });
    }
 
    private static MutableText clickableTip(String textKey, String hoverKey, String command,
                                            Formatting color, boolean bold) {
        MutableText text = SstI18n.text(textKey).formatted(color);
        if (bold) text.formatted(Formatting.BOLD);
        return text.setStyle(text.getStyle()
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(SstI18n.text(hoverKey).formatted(Formatting.WHITE))));
    }
 
    private static int setLanguage(String language) {
        CONFIG.language = language;
        CONFIG.save();
        SstI18n.setLanguage(language);
        sendLocalMessage(SstI18n.text("message.showspawntime.language_set", language).formatted(Formatting.GREEN));
        return 1;
    }
 
    public static void sendLocalMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) client.inGameHud.getChatHud().addMessage(message);
    }
 
    public static void playConfiguredSound(String configured, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        Identifier id = Identifier.tryParse(remapLegacySound(configured));
        SoundEvent sound = id == null ? null : Registries.SOUND_EVENT.get(id);
        if (sound == null) sound = Registries.SOUND_EVENT.get(Identifier.ofVanilla("block.note_block.pling"));
        if (sound != null) {
            client.world.playSoundClient(client.player.getX(), client.player.getY(), client.player.getZ(),
                    sound, SoundCategory.MASTER, 1.0F, pitch, false);
        }
    }
 
    private static String remapLegacySound(String sound) {
        if (sound == null || sound.isBlank()) return "minecraft:block.note_block.pling";
        return switch (sound.toLowerCase(Locale.ROOT)) {
            case "note.pling" -> "minecraft:block.note_block.pling";
            case "random.orb" -> "minecraft:entity.experience_orb.pickup";
            case "random.successful_hit" -> "minecraft:entity.arrow.hit_player";
            case "ambient.weather.thunder" -> "minecraft:entity.lightning_bolt.thunder";
            case "mob.wither.spawn" -> "minecraft:entity.wither.spawn";
            case "mob.enderdragon.end" -> "minecraft:entity.ender_dragon.death";
            default -> sound.contains(":") ? sound : "minecraft:" + sound;
        };
    }
 
    public static void checkUpdates(boolean reportLatest) {
        Thread.ofVirtual().name("ShowSpawnTime update checker").start(() -> {
            try {
                URI uri = URI.create("https://raw.githubusercontent.com/Safolour/ShowSpawnTime/main/build.gradle");
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(20_000);
                connection.setRequestProperty("User-Agent", "ShowSpawnTime update checker");
                String latest = "";
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    Pattern versionPattern = Pattern.compile("version\\s*=\\s*[\"']([^\"']+)[\"']");
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = versionPattern.matcher(line);
                        if (matcher.find()) { latest = matcher.group(1); break; }
                    }
                }
                int comparison = compareVersions(VERSION, latest);
                if (comparison < 0) {
                    MutableText text = SstI18n.text("message.showspawntime.update_available", latest).formatted(Formatting.AQUA)
                            .setStyle(SstI18n.text("message.showspawntime.update_available", latest).formatted(Formatting.AQUA).getStyle()
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/Safolour/ShowSpawnTime/releases")))
                                    .withHoverEvent(new HoverEvent.ShowText(SstI18n.text("tooltip.showspawntime.download"))));
                    MinecraftClient.getInstance().execute(() -> sendLocalMessage(text));
                } else if (reportLatest) {
                    MinecraftClient.getInstance().execute(() -> sendLocalMessage(SstI18n.text("message.showspawntime.up_to_date").formatted(Formatting.GREEN)));
                }
            } catch (Exception e) {
                LOGGER.debug("Update check failed", e);
            }
        });
    }
 
    private static int compareVersions(String current, String latest) {
        if (latest == null || latest.isBlank()) return 0;
        String[] left = current.split("\\.");
        String[] right = latest.split("\\.");
        for (int index = 0; index < Math.max(left.length, right.length); index++) {
            int a = index < left.length ? parseVersionPart(left[index]) : 0;
            int b = index < right.length ? parseVersionPart(right[index]) : 0;
            if (a != b) return Integer.compare(a, b);
        }
        return 0;
    }
 
    private static int parseVersionPart(String part) {
        Matcher matcher = Pattern.compile("\\d+").matcher(part);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }
}