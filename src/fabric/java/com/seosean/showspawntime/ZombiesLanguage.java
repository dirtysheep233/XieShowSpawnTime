package com.seosean.showspawntime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ZombiesLanguage {
    private static final Pattern ENTRY = Pattern.compile("^(.+)\\.([A-Z_0-9]+)\\s*=\\s*(.+)$");
    private static final List<String> ZOMBIES_LEFT = Arrays.asList(
            "Zombies Left", "剩余僵尸", "剩下殭屍數", "Zbývající zombie", "Zombier tilbage", "Zombies over",
            "Zombeja jäljellä", "Zombies restants", "Zombies übrig", "Ζόμπι που Απομένουν", "Hátralévő Zombik",
            "Zombi Rimanenti", "残りゾンビ", "남은 좀비", "Zombier igjen", "Pozostałe zombi", "Zombies restantes",
            "Zumbis restantes", "Zombi Rămași", "Осталось зомби", "Zombies Restantes", "Zombier kvar", "Kalan Zombi",
            "Залишилося зомбі");

    private final Map<String, List<String>> valuesByKey = new HashMap<>();
    private final Map<String, Map<String, String>> valuesByLanguage = new HashMap<>();
    private String detectedLanguage = "EN_US";

    public void load() {
        valuesByKey.clear();
        valuesByLanguage.clear();

        try (InputStream stream = ZombiesLanguage.class.getClassLoader().getResourceAsStream("assets/showspawntime/lang/showspawntime.lang")) {
            if (stream == null) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    Matcher matcher = ENTRY.matcher(line);
                    if (!matcher.matches()) {
                        continue;
                    }
                    String key = matcher.group(1).trim();
                    String language = matcher.group(2).trim();
                    String value = matcher.group(3).trim();
                    valuesByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
                    valuesByLanguage.computeIfAbsent(language, ignored -> new HashMap<>()).put(key, value);
                }
            }
        } catch (IOException e) {
            ShowSpawnTimeClient.LOGGER.warn("Failed to load ShowSpawnTime language table", e);
        }
    }

    public void detect(ScoreboardSnapshot scoreboard) {
        String line = scoreboard.line(4);
        if (line == null || line.isEmpty()) {
            return;
        }
        String leftText = leftPrefix(line);
        if (leftText.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Map<String, String>> entry : valuesByLanguage.entrySet()) {
            String value = entry.getValue().get("zombies.game.zombiesleft");
            if (value != null && leftText.equalsIgnoreCase(value)) {
                detectedLanguage = entry.getKey();
                return;
            }
        }
    }

    public boolean isZombiesLeft(String string) {
        String leftText = leftPrefix(string);
        if (leftText.isEmpty()) {
            return false;
        }
        for (String value : ZOMBIES_LEFT) {
            if (leftText.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return containsAnyValue(leftText, "zombies.game.zombiesleft", false);
    }

    public boolean isRoundTitle(String string) {
        return contains(string, "zombies.game.round");
    }

    public int getRoundNumber(String string) {
        String trimmed = ScoreboardSnapshot.trim(string);
        if (!isRoundTitle(trimmed)) {
            return 0;
        }
        Matcher matcher = Pattern.compile("\\d+").matcher(trimmed);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }

    public boolean equals(String string, String key) {
        return containsAnyValue(ScoreboardSnapshot.trim(string), key, false);
    }

    public boolean contains(String string, String key) {
        return containsAnyValue(ScoreboardSnapshot.trim(string), key, true);
    }

    public String get(String key) {
        Map<String, String> byLanguage = valuesByLanguage.get(detectedLanguage);
        if (byLanguage != null && byLanguage.containsKey(key)) {
            return byLanguage.get(key);
        }
        byLanguage = valuesByLanguage.get("EN_US");
        if (byLanguage != null && byLanguage.containsKey(key)) {
            return byLanguage.get(key);
        }
        List<String> values = valuesByKey.get(key);
        return values == null || values.isEmpty() ? "" : values.get(0);
    }

    public List<String> values(String key) {
        List<String> values = valuesByKey.get(key);
        return values == null ? List.of() : List.copyOf(values);
    }

    private boolean containsAnyValue(String string, String key, boolean substring) {
        if (string == null || string.isEmpty()) {
            return false;
        }
        List<String> values = valuesByKey.get(key);
        if (values == null) {
            return false;
        }
        Set<String> unique = new HashSet<>(values);
        String normalized = string.toLowerCase(Locale.ROOT);
        for (String value : unique) {
            if (value == null || value.isEmpty()) {
                continue;
            }
            String candidate = ScoreboardSnapshot.trim(value).toLowerCase(Locale.ROOT);
            if (substring ? normalized.contains(candidate) : normalized.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String leftPrefix(String string) {
        String trimmed = ScoreboardSnapshot.trim(string);
        if (trimmed.contains(":")) {
            return ScoreboardSnapshot.trim(trimmed.split(":", 2)[0]);
        }
        if (trimmed.contains("：")) {
            return ScoreboardSnapshot.trim(trimmed.split("：", 2)[0]);
        }
        return "";
    }
}
