package com.seosean.showspawntime;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PowerupManager {
    private static final Integer[] R2_MAX_DE = {2, 8, 12, 16, 21, 26};
    private static final Integer[] R2_MAX_BB = {2, 5, 8, 12, 16, 21, 26};
    private static final Integer[] R3_MAX_DEBB = {3, 6, 9, 13, 17, 22, 27};
    private static final Integer[] R2_MAX_TL = {2, 8, 12, 16, 21, 26, 31, 36};
    private static final Integer[] R3_MAX_TL = {3, 6, 9, 13, 17, 22, 27, 32, 37};
    private static final Integer[] R2_MAX_PR = {2, 5, 8, 12, 16, 21, 26};
    private static final Integer[] R3_MAX_PR = {3, 6, 9, 13, 17, 22, 27};
    private static final Integer[] R2_MAX_AA = {2, 5, 8, 12, 16, 21, 26, 31, 36, 41, 46, 51, 61, 66, 71, 76, 81, 86, 91, 96};
    private static final Integer[] R3_MAX_AA = {3, 6, 9, 13, 17, 22, 27, 32, 37, 42, 47, 52, 62, 67, 72, 77, 82, 87, 92, 97};
    private static final Integer[] R2_INS_DE = {2, 8, 11, 14, 17, 23};
    private static final Integer[] R2_INS_BB = {2, 5, 8, 11, 14, 17, 23};
    private static final Integer[] R3_INS_DEBB = {3, 6, 9, 12, 18, 21, 24};
    private static final Integer[] R2_INS_AA = {2, 5, 8, 11, 14, 17, 20, 23};
    private static final Integer[] R3_INS_AA = {3, 6, 9, 12, 15, 18, 21};
    private static final Integer[] R2_INS_TL = {2, 8, 11, 14, 17, 23};
    private static final Integer[] R3_INS_TL = {3, 6, 9, 12, 18, 21, 24};
    private static final Integer[] R2_INS_PR = {2, 5, 8, 11, 14, 17, 23};
    private static final Integer[] R3_INS_PR = {3, 6, 9, 12, 15, 18, 21, 24};
    private static final Integer[] R5_SS_AA = {5, 15, 45, 55, 65, 75, 85, 95, 105};
    private static final Integer[] R6_SS_AA = {6, 16, 26, 36, 46, 66, 76, 86, 96};
    private static final Integer[] R7_SS_AA = {7, 17, 27, 37, 47, 67, 77, 87, 97};

    private final Map<UUID, ActivePowerup> active = new LinkedHashMap<>();
    private final Map<UUID, Tombstone> tombstones = new LinkedHashMap<>();
    private final List<PendingActivation> pendingActivations = new ArrayList<>();
    private final EnumSet<PowerupType> incoming = EnumSet.noneOf(PowerupType.class);
    private List<Integer> instaRounds = new ArrayList<>();
    private List<Integer> maxRounds = new ArrayList<>();
    private List<Integer> spreeRounds = new ArrayList<>();

    public void reset() {
        active.clear();
        tombstones.clear();
        pendingActivations.clear();
        incoming.clear();
        instaRounds = new ArrayList<>();
        maxRounds = new ArrayList<>();
        spreeRounds = new ArrayList<>();
    }

    public void tick(MinecraftClient client, GameState state) {
        if (client.world == null) {
            return;
        }

        tombstones.entrySet().removeIf(entry -> entry.getValue().expiresAtTick <= state.clientTicks());
        for (var iterator = active.entrySet().iterator(); iterator.hasNext();) {
            ActivePowerup powerup = iterator.next().getValue();
            if (powerup.remainingTicks <= 0 || powerup.entity.isRemoved()) {
                iterator.remove();
                tombstones.put(powerup.entity.getUuid(),
                        new Tombstone(powerup.type, state.clientTicks() + 20));
                continue;
            }
            powerup.remainingTicks--;
        }
        processPendingActivations(state.clientTicks());
        if (!ShowSpawnTimeClient.CONFIG.powerupAlert) {
            return;
        }

        if (state.clientTicks() % 5 == 0 && state.isInZombies()) {
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof ArmorStandEntity armorStand && armorStand.hasCustomName()) {
                    detectArmorStand(armorStand, state);
                }
            }
        }

    }

    public void onRoundStarted(int round, ZombiesMap map, int gameMilliseconds) {
        incoming.clear();
        if (bossRounds(map).contains(round)) {
            active.clear();
        }
        if (instaRounds.contains(round)) {
            incoming.add(PowerupType.INSTA_KILL);
        }
        if (maxRounds.contains(round)) {
            incoming.add(PowerupType.MAX_AMMO);
        }
        if (spreeRounds.contains(round)) {
            incoming.add(PowerupType.SHOPPING_SPREE);
        }
    }

    public PowerupType onActivated(String message, GameState state) {
        if (!ShowSpawnTimeClient.CONFIG.powerupAlert || !state.isInZombies()) {
            return PowerupType.NULL;
        }
        if (!ShowSpawnTimeClient.LANG.contains(message, "zombies.game.activated")
                && !ShowSpawnTimeClient.LANG.contains(message, "zombies.game.activated.2")) {
            return PowerupType.NULL;
        }

        PowerupType type = typeFromActivatedMessage(message);
        if (type == PowerupType.NULL) {
            return type;
        }

        learnFromActivationChat(type, state.currentRound(), state.gameMilliseconds(), state.map());
        pendingActivations.add(new PendingActivation(type, state.clientTicks() + 5));
        return type;
    }

    public void setInstaPattern(int baseRound, ZombiesMap map) {
        instaRounds = instaPattern(baseRound, map);
    }

    public void setMaxPattern(int baseRound, ZombiesMap map) {
        maxRounds = maxPattern(baseRound, map);
    }

    public void setSpreePattern(int baseRound) {
        spreeRounds = switch (baseRound) {
            case 5 -> asList(R5_SS_AA);
            case 6 -> asList(R6_SS_AA);
            case 7 -> asList(R7_SS_AA);
            default -> new ArrayList<>();
        };
    }

    public List<Text> predictions(int currentRound) {
        List<Text> predictions = new ArrayList<>();
        addPrediction(predictions, PowerupType.INSTA_KILL, instaRounds, currentRound);
        addPrediction(predictions, PowerupType.MAX_AMMO, maxRounds, currentRound);
        addPrediction(predictions, PowerupType.SHOPPING_SPREE, spreeRounds, currentRound);
        return predictions;
    }

    public List<ActivePowerup> active() {
        return active.values().stream()
                .sorted(Comparator.comparingInt(ActivePowerup::remainingTicks))
                .toList();
    }

    public List<PowerupType> incoming() {
        return List.copyOf(incoming);
    }

    public ActivePowerup find(Entity entity) {
        return entity == null ? null : active.get(entity.getUuid());
    }

    private void detectArmorStand(ArmorStandEntity armorStand, GameState state) {
        if (active.containsKey(armorStand.getUuid()) || tombstones.containsKey(armorStand.getUuid())) {
            return;
        }
        Text customName = armorStand.getCustomName();
        if (customName == null) {
            return;
        }
        PowerupType type = PowerupType.fromName(customName.getString());
        if (type == PowerupType.NULL) {
            return;
        }
        active.put(armorStand.getUuid(), new ActivePowerup(type, armorStand, customName));
        incoming.remove(type);
        learnFromEntityObservation(type, state.currentRound(), state.gameMilliseconds(), state.map());
    }

    private void learnFromActivationChat(PowerupType type, int round, int gameMilliseconds, ZombiesMap map) {
        if (round <= 0) {
            return;
        }
        if (type == PowerupType.INSTA_KILL && instaRounds.isEmpty()) {
            if (round == 2 || round == 3 && gameMilliseconds <= 500) setInstaPattern(2, map);
            else if (round == 3 || round == 4) setInstaPattern(3, map);
        } else if (type == PowerupType.MAX_AMMO && maxRounds.isEmpty()) {
            if (round == 2 || round == 3 && gameMilliseconds <= 500) setMaxPattern(2, map);
            else if (round == 3 || round == 4) setMaxPattern(3, map);
        } else if (type == PowerupType.SHOPPING_SPREE && spreeRounds.isEmpty()) {
            if (round == 5 || round == 6 && gameMilliseconds <= 500) setSpreePattern(5);
            else if (round == 6 || round == 7 && gameMilliseconds <= 500) setSpreePattern(6);
            else if (round == 7 || round == 8) setSpreePattern(7);
        }
    }

    private void learnFromEntityObservation(PowerupType type, int round, int gameMilliseconds, ZombiesMap map) {
        learnPattern(type, round, gameMilliseconds, map, 1_000);
    }

    private void processPendingActivations(int clientTick) {
        if (pendingActivations.isEmpty()) {
            return;
        }
        for (var iterator = pendingActivations.iterator(); iterator.hasNext();) {
            PendingActivation pending = iterator.next();
            if (pending.executeAtTick > clientTick) {
                continue;
            }
            boolean claimed = tombstones.entrySet().removeIf(entry -> entry.getValue().type == pending.type);
            if (!claimed) {
                incoming.remove(pending.type);
            }
            iterator.remove();
        }
    }

    private void claim(ActivePowerup powerup, int clientTick) {
        active.remove(powerup.entity.getUuid());
        tombstones.put(powerup.entity.getUuid(), new Tombstone(powerup.type, clientTick + 20));
    }

    private void learnPattern(PowerupType type, int round, int gameMilliseconds, ZombiesMap map,
                              int thresholdMilliseconds) {
        if (round <= 0) {
            return;
        }
        if (type == PowerupType.INSTA_KILL && instaRounds.isEmpty()) {
            int base = inferPattern(round, gameMilliseconds, thresholdMilliseconds,
                    instaPattern(2, map), instaPattern(3, map), 2, 3);
            if (base != 0) {
                setInstaPattern(base, map);
            }
        } else if (type == PowerupType.MAX_AMMO && maxRounds.isEmpty()) {
            int base = inferPattern(round, gameMilliseconds, thresholdMilliseconds,
                    maxPattern(2, map), maxPattern(3, map), 2, 3);
            if (base != 0) {
                setMaxPattern(base, map);
            }
        } else if (type == PowerupType.SHOPPING_SPREE && spreeRounds.isEmpty()) {
            int base = inferSpreePattern(round, gameMilliseconds, thresholdMilliseconds);
            if (base != 0) {
                setSpreePattern(base);
            }
        }
    }

    private static int inferPattern(int round, int gameMilliseconds, int thresholdMilliseconds,
                                    List<Integer> earlierPattern, List<Integer> laterPattern,
                                    int earlierBase, int laterBase) {
        if (earlierPattern.contains(round)) {
            return earlierBase;
        }
        if (laterPattern.contains(round) && gameMilliseconds <= thresholdMilliseconds) {
            return earlierBase;
        }
        if (laterPattern.contains(round)
                || gameMilliseconds <= thresholdMilliseconds && laterPattern.contains(round - 1)) {
            return laterBase;
        }
        return 0;
    }

    private static int inferSpreePattern(int round, int gameMilliseconds, int thresholdMilliseconds) {
        List<Integer> roundFive = asList(R5_SS_AA);
        List<Integer> roundSix = asList(R6_SS_AA);
        List<Integer> roundSeven = asList(R7_SS_AA);
        if (roundFive.contains(round)) return 5;
        if (roundSix.contains(round) && gameMilliseconds <= thresholdMilliseconds) return 5;
        if (roundSix.contains(round)) return 6;
        if (roundSeven.contains(round) && gameMilliseconds <= thresholdMilliseconds) return 6;
        if (roundSeven.contains(round)
                || gameMilliseconds <= thresholdMilliseconds && roundSeven.contains(round - 1)) return 7;
        return 0;
    }

    private static List<Integer> instaPattern(int baseRound, ZombiesMap map) {
        return switch (map) {
            case DEAD_END -> asList(baseRound == 2 ? R2_INS_DE : R3_INS_DEBB);
            case BAD_BLOOD -> asList(baseRound == 2 ? R2_INS_BB : R3_INS_DEBB);
            case THE_LAB -> asList(baseRound == 2 ? R2_INS_TL : R3_INS_TL);
            case ALIEN_ARCADIUM -> asList(baseRound == 2 ? R2_INS_AA : R3_INS_AA);
            case PRISON -> asList(baseRound == 2 ? R2_INS_PR : R3_INS_PR);
            default -> List.of();
        };
    }

    private static List<Integer> maxPattern(int baseRound, ZombiesMap map) {
        return switch (map) {
            case DEAD_END -> asList(baseRound == 2 ? R2_MAX_DE : R3_MAX_DEBB);
            case BAD_BLOOD -> asList(baseRound == 2 ? R2_MAX_BB : R3_MAX_DEBB);
            case THE_LAB -> asList(baseRound == 2 ? R2_MAX_TL : R3_MAX_TL);
            case ALIEN_ARCADIUM -> asList(baseRound == 2 ? R2_MAX_AA : R3_MAX_AA);
            case PRISON -> asList(baseRound == 2 ? R2_MAX_PR : R3_MAX_PR);
            default -> List.of();
        };
    }

    private PowerupType typeFromActivatedMessage(String message) {
        if (ShowSpawnTimeClient.LANG.contains(message, "zombies.game.instakill.lower")) {
            return PowerupType.INSTA_KILL;
        }
        if (ShowSpawnTimeClient.LANG.contains(message, "zombies.game.maxammo.lower")) {
            return PowerupType.MAX_AMMO;
        }
        if (ShowSpawnTimeClient.LANG.contains(message, "zombies.game.doublegold.lower")) {
            return PowerupType.DOUBLE_GOLD;
        }
        if (ShowSpawnTimeClient.LANG.contains(message, "zombies.game.bonusgold.lower")) {
            return PowerupType.BONUS_GOLD;
        }
        if (ShowSpawnTimeClient.LANG.contains(message, "zombies.game.carpenter.lower")) {
            return PowerupType.CARPENTER;
        }
        if (ShowSpawnTimeClient.LANG.contains(message, "zombies.game.shoppingspree.lower")) {
            return PowerupType.SHOPPING_SPREE;
        }
        return PowerupType.NULL;
    }

    private static List<Integer> bossRounds(ZombiesMap map) {
        return switch (map) {
            case ALIEN_ARCADIUM -> List.of(25, 35, 56, 57, 101);
            case DEAD_END -> List.of(5, 10, 15, 20, 25, 30);
            case BAD_BLOOD -> List.of(10, 15, 20, 25, 30);
            case THE_LAB -> List.of(5, 10, 15, 20, 25, 30, 35, 40);
            case PRISON -> List.of(10, 20, 30);
            default -> List.of();
        };
    }

    private static void addPrediction(List<Text> target, PowerupType type, List<Integer> rounds, int currentRound) {
        MutableText powerup = type.predictionText().formatted(type.color);
        MutableText now = SstI18n.text("value.showspawntime.powerup_prediction.now")
                .formatted(Formatting.GREEN, Formatting.BOLD);
        MutableText in = SstI18n.text("value.showspawntime.powerup_prediction.in").formatted(Formatting.WHITE);
        for (int index = 0; index < rounds.size(); index++) {
            int round = rounds.get(index);
            if (round >= currentRound) {
                if (round == currentRound && index + 1 < rounds.size()) {
                    target.add(SstI18n.text("message.showspawntime.powerup_prediction.now_next",
                            powerup, now, SstI18n.text("message.showspawntime.powerup_prediction.next", rounds.get(index + 1))
                                    .formatted(Formatting.GRAY)));
                } else if (round == currentRound) {
                    target.add(SstI18n.text("message.showspawntime.powerup_prediction.now", powerup, now));
                } else {
                    target.add(SstI18n.text("message.showspawntime.powerup_prediction.round", powerup, in,
                            Text.literal(Integer.toString(round)).formatted(Formatting.AQUA)));
                }
                return;
            }
        }
    }

    private static List<Integer> asList(Integer[] values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    public enum PowerupType {
        NULL(Formatting.WHITE, "powerup.showspawntime.unknown"),
        INSTA_KILL(Formatting.RED, "powerup.showspawntime.insta_kill"),
        MAX_AMMO(Formatting.BLUE, "powerup.showspawntime.max_ammo"),
        DOUBLE_GOLD(Formatting.GOLD, "powerup.showspawntime.double_gold"),
        CARPENTER(Formatting.DARK_BLUE, "powerup.showspawntime.carpenter"),
        BONUS_GOLD(Formatting.YELLOW, "powerup.showspawntime.bonus_gold"),
        SHOPPING_SPREE(Formatting.DARK_PURPLE, "powerup.showspawntime.shopping_spree");

        public final Formatting color;
        private final String translationKey;

        PowerupType(Formatting color, String translationKey) {
            this.color = color;
            this.translationKey = translationKey;
        }

        public MutableText text() {
            return SstI18n.text(translationKey);
        }

        public MutableText predictionText() {
            return switch (this) {
                case INSTA_KILL -> SstI18n.text("powerup.showspawntime.prediction.insta_kill");
                case MAX_AMMO -> SstI18n.text("powerup.showspawntime.prediction.max_ammo");
                case SHOPPING_SPREE -> SstI18n.text("powerup.showspawntime.prediction.shopping_spree");
                default -> text();
            };
        }

        public static PowerupType fromName(String name) {
            if (ShowSpawnTimeClient.LANG.equals(name, "zombies.game.instakill.upper")) return INSTA_KILL;
            if (ShowSpawnTimeClient.LANG.equals(name, "zombies.game.maxammo.upper")) return MAX_AMMO;
            if (ShowSpawnTimeClient.LANG.equals(name, "zombies.game.doublegold.upper")) return DOUBLE_GOLD;
            if (ShowSpawnTimeClient.LANG.equals(name, "zombies.game.bonusgold.upper")) return BONUS_GOLD;
            if (ShowSpawnTimeClient.LANG.equals(name, "zombies.game.carpenter.upper")) return CARPENTER;
            if (ShowSpawnTimeClient.LANG.equals(name, "zombies.game.shoppingspree.upper")) return SHOPPING_SPREE;
            return NULL;
        }
    }

    private record PendingActivation(PowerupType type, int executeAtTick) {
    }

    private record Tombstone(PowerupType type, int expiresAtTick) {
    }

    public final class ActivePowerup {
        private final PowerupType type;
        private final ArmorStandEntity entity;
        private final Text originalName;
        private int remainingTicks = 1_200;

        private ActivePowerup(PowerupType type, ArmorStandEntity entity, Text originalName) {
            this.type = type;
            this.entity = entity;
            this.originalName = originalName.copy();
        }

        public PowerupType type() {
            return type;
        }

        public int remainingTicks() {
            return Math.max(0, remainingTicks);
        }

        public int secondsRemaining() {
            return Math.max(0, remainingTicks / 20);
        }

        public Text countdownName() {
            Text name = ShowSpawnTimeClient.CONFIG.powerupCountdown
                    ? originalName.copy().append(Text.literal(" 00:" + String.format("%02d", secondsRemaining()))
                            .formatted(Formatting.AQUA))
                    : originalName;
            return ShowSpawnTimeClient.CONFIG.powerupNameTagShadow
                    ? name.copy().setStyle(name.getStyle().withShadowColor(0xFF000000))
                    : name;
        }
    }
}
