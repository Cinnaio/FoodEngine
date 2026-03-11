package com.github.cinnaio.foodEngine.parser;

import com.github.cinnaio.foodEngine.FoodEngine;
import com.github.cinnaio.foodEngine.condition.Condition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class ConditionParser {

    private final FoodEngine plugin;
    private final boolean hasPlaceholderApi;
    private final Method placeholderSetMethod;
    private static final ThreadLocal<java.util.Map<String, String>> extraPlaceholders = new ThreadLocal<>();

    public ConditionParser(FoodEngine plugin) {
        this.plugin = plugin;
        boolean present = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        Method method = null;
        if (present) {
            try {
                Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                method = papi.getMethod("setPlaceholders", Player.class, String.class);
            } catch (Throwable ex) {
                present = false;
                method = null;
                plugin.getLogger().warning("PlaceholderAPI detected but could not be hooked; conditions will not expand placeholders.");
            }
        }
        this.hasPlaceholderApi = present;
        this.placeholderSetMethod = method;
    }

    public Condition parse(String expression) {
        String trimmed = expression.trim();
        if (trimmed.isEmpty()) {
            return player -> true;
        }

        return player -> {
            try {
                String expr = trimmed;
                java.util.Map<String, String> ctx = extraPlaceholders.get();
                if (ctx != null && !ctx.isEmpty()) {
                    for (var e : ctx.entrySet()) {
                        String k = e.getKey();
                        String v = e.getValue();
                        if (k != null && v != null) {
                            expr = expr.replace(k, v);
                        }
                    }
                }
                if (hasPlaceholderApi && player != null) {
                    expr = applyPlaceholders(player, expr);
                }

                String op = findOperator(expr);
                if (op == null) {
                    if (plugin.isDebug()) {
                        plugin.getLogger().warning("Invalid condition (no operator): " + expression);
                    }
                    return false;
                }

                String[] parts = expr.split("\\Q" + op + "\\E", 2);
                if (parts.length != 2) {
                    if (plugin.isDebug()) {
                        plugin.getLogger().warning("Invalid condition (split failed): " + expression);
                    }
                    return false;
                }

                String left = parts[0].trim();
                String right = parts[1].trim();

                Double leftNum = tryParseDouble(left);
                Double rightNum = tryParseDouble(right);

                if (leftNum != null && rightNum != null) {
                    return compareNumbers(leftNum, rightNum, op);
                }

                int cmp = left.compareTo(right);
                return compareStrings(cmp, op);
            } catch (Exception ex) {
                if (plugin.isDebug()) {
                    plugin.getLogger().warning("Error evaluating condition '" + expression + "': " + ex.getMessage());
                }
                return false;
            }
        };
    }

    private String applyPlaceholders(Player player, String input) {
        if (!hasPlaceholderApi || placeholderSetMethod == null) {
            return input;
        }
        try {
            Object result = placeholderSetMethod.invoke(null, player, input);
            return result instanceof String s ? s : input;
        } catch (Throwable ex) {
            if (plugin.isDebug()) {
                plugin.getLogger().warning("PlaceholderAPI expansion failed in condition: " + ex.getMessage());
            }
            return input;
        }
    }

    private String findOperator(String expr) {
        String[] ops = {">=", "<=", "==", "!=", ">", "<"};
        for (String op : ops) {
            if (expr.contains(op)) {
                return op;
            }
        }
        return null;
    }

    private Double tryParseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean compareNumbers(double left, double right, String op) {
        return switch (op) {
            case "==" -> Double.compare(left, right) == 0;
            case "!=" -> Double.compare(left, right) != 0;
            case ">" -> left > right;
            case "<" -> left < right;
            case ">=" -> left >= right;
            case "<=" -> left <= right;
            default -> false;
        };
    }

    private boolean compareStrings(int cmp, String op) {
        return switch (op) {
            case "==" -> cmp == 0;
            case "!=" -> cmp != 0;
            case ">" -> cmp > 0;
            case "<" -> cmp < 0;
            case ">=" -> cmp >= 0;
            case "<=" -> cmp <= 0;
            default -> false;
        };
    }

    public static void setContext(java.util.Map<String, String> ctx) {
        extraPlaceholders.set(ctx);
    }

    public static void clearContext() {
        extraPlaceholders.remove();
    }
}
