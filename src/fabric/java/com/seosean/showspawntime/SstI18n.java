package com.seosean.showspawntime;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SstI18n {
    public static final String EN_US = "en_us";
    public static final String ZH_CN = "zh_cn";
    private static final Map<String, Map<String, String>> TRANSLATIONS = new HashMap<>();
    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:(\\d+)\\$)?s|%%");
    private static String language = ZH_CN;

    private SstI18n() {
    }

    public static void load(String configuredLanguage) {
        TRANSLATIONS.clear();
        loadLanguage(EN_US);
        loadLanguage(ZH_CN);
        language = normalize(configuredLanguage);
    }

    public static String language() {
        return language;
    }

    public static void setLanguage(String selectedLanguage) {
        language = normalize(selectedLanguage);
    }

    public static boolean supports(String selectedLanguage) {
        return EN_US.equalsIgnoreCase(selectedLanguage) || ZH_CN.equalsIgnoreCase(selectedLanguage);
    }

    public static MutableText text(String key, Object... arguments) {
        String template = template(key);
        MutableText result = Text.empty();
        Matcher matcher = PLACEHOLDER.matcher(template);
        int end = 0;
        int nextArgument = 0;
        while (matcher.find()) {
            result.append(template.substring(end, matcher.start()));
            if ("%%".equals(matcher.group())) {
                result.append("%");
            } else {
                int argumentIndex = matcher.group(1) == null
                        ? nextArgument++ : Integer.parseInt(matcher.group(1)) - 1;
                if (argumentIndex < 0 || argumentIndex >= arguments.length) {
                    ShowSpawnTimeClient.LOGGER.warn("Missing ShowSpawnTime translation argument {} for {}", argumentIndex + 1, key);
                    result.append(matcher.group());
                } else if (arguments[argumentIndex] instanceof Text text) {
                    result.append(text.copy());
                } else {
                    result.append(String.valueOf(arguments[argumentIndex]));
                }
            }
            end = matcher.end();
        }
        return result.append(template.substring(end));
    }

    public static String string(String key, Object... arguments) {
        String template = template(key);
        Object[] visibleArguments = new Object[arguments.length];
        for (int index = 0; index < arguments.length; index++) {
            Object argument = arguments[index];
            visibleArguments[index] = argument instanceof Text text ? text.getString() : argument;
        }
        try {
            return String.format(Locale.ROOT, template, visibleArguments);
        } catch (IllegalArgumentException exception) {
            ShowSpawnTimeClient.LOGGER.warn("Invalid ShowSpawnTime translation format for {}", key, exception);
            return template;
        }
    }

    private static String template(String key) {
        String template = TRANSLATIONS.getOrDefault(language, Map.of()).get(key);
        return template == null
                ? TRANSLATIONS.getOrDefault(EN_US, Map.of()).getOrDefault(key, key)
                : template;
    }

    private static void loadLanguage(String locale) {
        String path = "assets/showspawntime/lang/" + locale + ".json";
        try (InputStream input = SstI18n.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                ShowSpawnTimeClient.LOGGER.error("Missing ShowSpawnTime language resource {}", path);
                return;
            }
            Map<String, String> translations = new Gson().fromJson(
                    new InputStreamReader(input, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>() { }.getType());
            TRANSLATIONS.put(locale, translations == null ? Map.of() : translations);
        } catch (Exception exception) {
            ShowSpawnTimeClient.LOGGER.error("Failed to load ShowSpawnTime language resource {}", path, exception);
        }
    }

    private static String normalize(String selectedLanguage) {
        return EN_US.equalsIgnoreCase(selectedLanguage) ? EN_US : ZH_CN;
    }
}
