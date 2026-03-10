package com.github.cinnaio.foodEngine.util;

import com.github.cinnaio.foodEngine.FoodEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("&([0-9a-fA-Fk-orK-OR])");
    private static final Pattern HEX_AMP_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    private static final Pattern HEX_BRACE_PATTERN = Pattern.compile("\\{#([0-9a-fA-F]{6})}");

    private static MiniMessage MINI_MESSAGE;
    private static boolean placeholderApiPresent;
    private static FoodEngine plugin;
    private static Method placeholderSetMethod;

    private TextUtil() {
    }

    public static void initialize(FoodEngine foodEngine) {
        plugin = foodEngine;
        MINI_MESSAGE = MiniMessage.miniMessage();
        placeholderApiPresent = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        placeholderSetMethod = null;
        if (placeholderApiPresent) {
            try {
                Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                placeholderSetMethod = papi.getMethod("setPlaceholders", Player.class, String.class);
            } catch (Throwable ex) {
                placeholderApiPresent = false;
                placeholderSetMethod = null;
                plugin.getLogger().warning("PlaceholderAPI detected but could not be hooked; placeholders will be skipped.");
            }
        }
    }

    public static Component toComponent(String text, Player player) {
        if (text == null) {
            return Component.empty();
        }

        String processed = text;

        if (placeholderApiPresent && player != null) {
            processed = applyPlaceholders(player, processed);
        }

        processed = applyColorConversions(processed);

        return MINI_MESSAGE.deserialize(processed);
    }

    public static String applyPlaceholdersOnly(String text, Player player) {
        if (text == null) {
            return null;
        }
        if (player == null) {
            return text;
        }
        return applyPlaceholders(player, text);
    }

    private static String applyPlaceholders(Player player, String input) {
        if (!placeholderApiPresent || placeholderSetMethod == null) {
            return input;
        }
        try {
            Object result = placeholderSetMethod.invoke(null, player, input);
            return result instanceof String s ? s : input;
        } catch (Throwable ex) {
            if (plugin != null && plugin.isDebug()) {
                plugin.getLogger().warning("PlaceholderAPI expansion failed: " + ex.getMessage());
            }
            return input;
        }
    }

    public static String applyColorConversions(String input) {
        String result = input;

        Matcher hexMatcher = HEX_AMP_PATTERN.matcher(result);
        StringBuffer hexBuf = new StringBuffer();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            hexMatcher.appendReplacement(hexBuf, "<#" + hex + ">");
        }
        hexMatcher.appendTail(hexBuf);
        result = hexBuf.toString();

        Matcher braceMatcher = HEX_BRACE_PATTERN.matcher(result);
        StringBuffer braceBuf = new StringBuffer();
        while (braceMatcher.find()) {
            String hex = braceMatcher.group(1);
            braceMatcher.appendReplacement(braceBuf, "<#" + hex + ">");
        }
        braceMatcher.appendTail(braceBuf);
        result = braceBuf.toString();

        Matcher legacyMatcher = LEGACY_COLOR_PATTERN.matcher(result);
        StringBuffer legacyBuf = new StringBuffer();
        while (legacyMatcher.find()) {
            String code = legacyMatcher.group(1).toLowerCase();
            String replacement = mapLegacyCodeToMiniMessage(code);
            legacyMatcher.appendReplacement(legacyBuf, replacement);
        }
        legacyMatcher.appendTail(legacyBuf);
        result = legacyBuf.toString();

        return result;
    }

    private static String mapLegacyCodeToMiniMessage(String code) {
        return switch (code) {
            case "0" -> "<black>";
            case "1" -> "<dark_blue>";
            case "2" -> "<dark_green>";
            case "3" -> "<dark_aqua>";
            case "4" -> "<dark_red>";
            case "5" -> "<dark_purple>";
            case "6" -> "<gold>";
            case "7" -> "<gray>";
            case "8" -> "<dark_gray>";
            case "9" -> "<blue>";
            case "a" -> "<green>";
            case "b" -> "<aqua>";
            case "c" -> "<red>";
            case "d" -> "<light_purple>";
            case "e" -> "<yellow>";
            case "f" -> "<white>";
            case "k" -> "<obfuscated>";
            case "l" -> "<bold>";
            case "m" -> "<strikethrough>";
            case "n" -> "<underlined>";
            case "o" -> "<italic>";
            case "r" -> "<reset>";
            default -> "";
        };
    }

    public static Component placeholderComponent(String key, String value) {
        return MINI_MESSAGE.deserialize("<" + key + ">", Placeholder.parsed(key, value));
    }

    public static boolean isPlaceholderApiPresent() {
        return placeholderApiPresent;
    }

    public static FoodEngine getPlugin() {
        return plugin;
    }
}
