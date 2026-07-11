package com.seosean.showspawntime;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
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
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class ShowSpawnTimeClient implements ClientModInitializer {
    public static final String MOD_ID = "showspawntime";
    public static final String VERSION = "2.1.1";
    public static final String EMOJI_REGEX = "(?:[\\x{1F300}-\\x{1FAFF}]|[\\x{2600}-\\x{27BF}]|[\\x{2194}-\\x{21AA}]|[\\x{2B05}-\\x{2B55}])\\uFE0F?";
    public static final Logger LOGGER = LoggerFactory.getLogger("ShowSpawnTime");
    public static final SstConfig CONFIG = new SstConfig();
    public static final ZombiesLanguage LANG = new ZombiesLanguage();
    public static final PowerupManager POWERUPS = new PowerupManager();
    public static final DpsCounter DPS = new DpsCounter();
    public static final AutoSplits AUTO_SPLITS = new AutoSplits();
    public static final GameState STATE = new GameState();
    public static final OverlayRenderer OVERLAY = new OverlayRenderer();

    private static final String KEY_CATEGORY = "key.categories.showspawntime";
    private static KeyBinding togglePlayersKey;
    private static KeyBinding autoSplitsKey;
    private static KeyBinding configKey;
    private static boolean updateCheckScheduled;
    private static boolean wasConnected;
    private static boolean configTipShown;
    private static int updateCheckTicks;
    private static Screen pendingScreen;
    private static volatile UpdateStatus updateStatus = UpdateStatus.UNKNOWN;
    private static volatile String newestVersion = "";

    @Override
    public void onInitializeClient() {
        LANG.load();
        CONFIG.load();
        SstI18n.load(CONFIG.language);
        togglePlayersKey = KeyBindingHelper.registerKeyBinding(
                KeyBindingCompat.create("key.showspawntime.player_invisible", GLFW.GLFW_KEY_UNKNOWN));
        autoSplitsKey = KeyBindingHelper.registerKeyBinding(
                KeyBindingCompat.create("key.showspawntime.toggle_autosplits", GLFW.GLFW_KEY_SEMICOLON));
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
        if (connected && !wasConnected && !updateCheckScheduled && updateStatus == UpdateStatus.UNKNOWN) {
            updateCheckScheduled = true;
            updateCheckTicks = 60;
        }
        wasConnected = connected;
        if (!configTipShown && connected && STATE.isZombiesTitle()) {
            configTipShown = true;
            showConfigTip();
        }
        if (updateCheckScheduled && --updateCheckTicks <= 0) {
            updateCheckScheduled = false;
            checkUpdates(false);
        }
        boolean allowKeyActions = client.currentScreen == null;
        while (togglePlayersKey.wasPressed()) {
            if (!allowKeyActions) continue;
            CONFIG.playerInvisible = !CONFIG.playerInvisible;
            CONFIG.save();
            sendLocalMessage(toggleMessage("message.showspawntime.player_invisible_toggled", CONFIG.playerInvisible));
        }
        while (autoSplitsKey.wasPressed()) {
            if (!allowKeyActions) continue;
            autoSplitsToggleMessage();
        }
        while (configKey.wasPressed()) {
            if (!allowKeyActions) continue;
            client.setScreen(new SstConfigScreen(null));
        }
    }

    private static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("sstconfig").executes(context -> {
                MinecraftClient client = MinecraftClient.getInstance();
                pendingScreen = new SstConfigScreen(null);
                return 1;
            }));
            dispatcher.register(literal("ssthud").executes(context -> {
                MinecraftClient client = MinecraftClient.getInstance();
                pendingScreen = new SstHudEditorScreen(null);
                return 1;
            }));
            dispatcher.register(literal("sst")
                    .executes(context -> showFeatureHelp())
                    .then(literal("feature").executes(context -> showFeatureHelp()))
                    .then(literal("copy").then(argument("text", StringArgumentType.greedyString()).executes(context -> {
                        MinecraftClient.getInstance().keyboard.setClipboard(StringArgumentType.getString(context, "text"));
                        sendLocalMessage(SstI18n.text("message.showspawntime.copied").formatted(Formatting.GREEN));
                        return 1;
                    })))
                    .then(literal("ins").then(argument("pattern", IntegerArgumentType.integer(2, 3)).executes(context -> {
                        int pattern = IntegerArgumentType.getInteger(context, "pattern");
                        POWERUPS.setInstaPattern(pattern, STATE.map());
                        patternMessage("powerup.showspawntime.insta_kill", pattern);
                        return 1;
                    })))
                    .then(literal("max").then(argument("pattern", IntegerArgumentType.integer(2, 3)).executes(context -> {
                        int pattern = IntegerArgumentType.getInteger(context, "pattern");
                        POWERUPS.setMaxPattern(pattern, STATE.map());
                        patternMessage("powerup.showspawntime.max_ammo", pattern);
                        return 1;
                    })))
                    .then(literal("ss").then(argument("pattern", IntegerArgumentType.integer(5, 7)).executes(context -> {
                        int pattern = IntegerArgumentType.getInteger(context, "pattern");
                        POWERUPS.setSpreePattern(pattern);
                        patternMessage("powerup.showspawntime.shopping_spree", pattern);
                        return 1;
                    })))
                    .then(literal("mode")
                            .then(literal("normal").executes(context -> setDifficulty(GameState.Difficulty.NORMAL)))
                            .then(literal("hard").executes(context -> setDifficulty(GameState.Difficulty.HARD)))
                            .then(literal("rip").executes(context -> setDifficulty(GameState.Difficulty.RIP))))
                    .then(literal("autosplits").executes(context -> autoSplitsToggleMessage()))
                    .then(literal("lang")
                            .executes(context -> showLanguage())
                            .then(literal("zh_cn").executes(context -> setLanguage(SstI18n.ZH_CN)))
                            .then(literal("en_us").executes(context -> setLanguage(SstI18n.EN_US))))
                    .then(literal("checkupdate").executes(context -> {
                        checkUpdates(true);
                        return 1;
                    })));
            dispatcher.register(literal("sstdebug")
                    .then(literal("title").executes(context -> debug(STATE.isZombiesTitle() + " | " + STATE.scoreboard().title())))
                    .then(literal("zbleft").executes(context -> debug(STATE.isInZombies() + " | " + STATE.scoreboard().line(4))))
                    .then(literal("map").executes(context -> debug(STATE.map().name())))
                    .then(literal("round").executes(context -> debug(Integer.toString(STATE.currentRound()))))
                    .then(literal("tick").executes(context -> debug(Integer.toString(STATE.gameMilliseconds()))))
                    .then(literal("version").executes(context -> debug(updateStatus + " | " + newestVersion)))
                    .then(literal("players").executes(context -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        List<String> players = client.world == null ? List.of() : client.world.getPlayers().stream()
                                .map(player -> player.getName().getString()).toList();
                        return debug(players.toString());
                    })));
        });
    }

    private static void showConfigTip() {
        MutableText message = Text.literal("ShowSpawnTime: ").formatted(Formatting.AQUA, Formatting.BOLD)
                .append(SstI18n.text("message.showspawntime.config_tip.intro").formatted(Formatting.WHITE))
                .append(clickableTip("message.showspawntime.config_tip.config",
                        "tooltip.showspawntime.config_tip.config", "/sstconfig", Formatting.GOLD, true))
                .append(clickableTip("message.showspawntime.config_tip.hud",
                        "tooltip.showspawntime.config_tip.hud", "/ssthud", Formatting.GOLD, true))
                .append(clickableTip("message.showspawntime.config_tip.features",
                        "tooltip.showspawntime.config_tip.features", "/sst feature", Formatting.GREEN, false))
                .append(Text.literal("\n"))
                .append(SstI18n.text("message.showspawntime.config_tip.commands").formatted(Formatting.GRAY));
        sendLocalMessage(message);
    }

    private static MutableText clickableTip(String textKey, String hoverKey, String command,
                                            Formatting color, boolean bold) {
        MutableText text = SstI18n.text(textKey).formatted(color);
        if (bold) {
            text.formatted(Formatting.BOLD);
        }
        return text.setStyle(text.getStyle()
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(SstI18n.text(hoverKey).formatted(Formatting.WHITE))));
    }

    private static int showFeatureHelp() {
        sendLocalMessage(SstI18n.text("message.showspawntime.help").formatted(Formatting.GREEN));
        return 1;
    }

    private static int showLanguage() {
        sendLocalMessage(SstI18n.text("message.showspawntime.language_current", SstI18n.language())
                .formatted(Formatting.GREEN));
        return 1;
    }

    private static int setLanguage(String language) {
        CONFIG.language = language;
        CONFIG.save();
        SstI18n.setLanguage(language);
        sendLocalMessage(SstI18n.text("message.showspawntime.language_set", language)
                .formatted(Formatting.GREEN));
        return 1;
    }

    private static int autoSplitsToggleMessage() {
        boolean enabled = AUTO_SPLITS.toggle();
        sendLocalMessage(toggleMessage("message.showspawntime.autosplits_toggled", enabled));
        return 1;
    }

    private static MutableText toggleMessage(String key, boolean enabled) {
        return SstI18n.text(key, SstI18n.text(enabled
                        ? "value.showspawntime.on" : "value.showspawntime.off")
                .formatted(enabled ? Formatting.GREEN : Formatting.RED)).formatted(Formatting.YELLOW);
    }

    private static int setDifficulty(GameState.Difficulty difficulty) {
        STATE.setDifficulty(difficulty);
        sendLocalMessage(SstI18n.text("message.showspawntime.difficulty_set",
                SstI18n.text("value.showspawntime.difficulty."
                        + difficulty.name().toLowerCase(Locale.ROOT)).formatted(Formatting.RED))
                .formatted(Formatting.GREEN));
        return 1;
    }

    private static void patternMessage(String powerupKey, int pattern) {
        sendLocalMessage(SstI18n.text("message.showspawntime.pattern_set", SstI18n.text(powerupKey),
                Text.literal(Integer.toString(pattern)).formatted(Formatting.RED)).formatted(Formatting.GREEN));
        sendLocalMessage(SstI18n.text("message.showspawntime.pattern_auto_replace").formatted(Formatting.GRAY));
    }

    private static int debug(String value) {
        sendLocalMessage(Text.literal(value).formatted(Formatting.DARK_GREEN));
        return 1;
    }

    public static void sendLocalMessage(Text message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(message);
        }
    }

    public static void playConfiguredSound(String configured, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        Identifier id = Identifier.tryParse(remapLegacySound(configured));
        SoundEvent sound = id == null ? null : Registries.SOUND_EVENT.get(id);
        if (sound == null) {
            sound = Registries.SOUND_EVENT.get(Identifier.ofVanilla("block.note_block.pling"));
        }
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

    public static boolean shouldAlterPlayer(AbstractClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        return CONFIG.playerInvisible && client.player != null && player != client.player && !player.isSleeping()
                && player.getMaxHealth() < 100 && client.player.distanceTo(player) < 7.71F;
    }

    public static float playerAlpha(AbstractClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!shouldAlterPlayer(player) || client.player == null) {
            return 1.0F;
        }
        float distance = client.player.distanceTo(player);
        if (distance < 1.4F) return 0.0F;
        if (distance <= 4.0F) return 0.0533F * distance + 0.0258F;
        if (distance <= 6.0F) return 0.15F * distance - 0.4126F;
        if (distance <= 7.71F) return 0.3F * distance - 1.313F;
        return 1.0F;
    }

    public static boolean shouldFullyHidePlayer(AbstractClientPlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        return shouldAlterPlayer(player) && client.player != null && client.player.distanceTo(player) < 1.4F;
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
                        if (matcher.find()) {
                            latest = matcher.group(1);
                            break;
                        }
                    }
                }
                newestVersion = latest;
                int comparison = compareVersions(VERSION, latest);
                updateStatus = comparison < 0 ? UpdateStatus.OUTDATED : comparison > 0 ? UpdateStatus.ADVANCED : UpdateStatus.LATEST;
                if (comparison < 0) {
                    MutableText text = SstI18n.text("message.showspawntime.update_available", latest)
                            .formatted(Formatting.AQUA)
                            .setStyle(SstI18n.text("message.showspawntime.update_available", latest)
                                    .formatted(Formatting.AQUA).getStyle().withClickEvent(new ClickEvent.OpenUrl(
                                            URI.create("https://github.com/Safolour/ShowSpawnTime/releases")))
                                    .withHoverEvent(new HoverEvent.ShowText(
                                            SstI18n.text("tooltip.showspawntime.download"))));
                    MinecraftClient.getInstance().execute(() -> sendLocalMessage(text));
                } else if (reportLatest) {
                    MinecraftClient.getInstance().execute(() -> sendLocalMessage(
                            SstI18n.text("message.showspawntime.up_to_date").formatted(Formatting.GREEN)));
                }
            } catch (Exception e) {
                updateStatus = UpdateStatus.ERROR;
                LOGGER.debug("Update check failed", e);
                if (reportLatest) {
                    MinecraftClient.getInstance().execute(() -> sendLocalMessage(
                            SstI18n.text("message.showspawntime.update_check_failed").formatted(Formatting.RED)));
                }
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

    private enum UpdateStatus {
        UNKNOWN,
        LATEST,
        OUTDATED,
        ADVANCED,
        ERROR
    }
}
