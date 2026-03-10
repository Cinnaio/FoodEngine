package com.github.cinnaio.foodEngine.action;

import com.github.cinnaio.foodEngine.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class AttributeAction implements ActionExecutor {

    private static Attribute findAttribute(String name) {
        String raw = name == null ? "" : name.trim().toLowerCase();
        if (raw.isEmpty()) {
            return null;
        }

        Registry<Attribute> registry = Bukkit.getRegistry(Attribute.class);

        // 1.21.2+ attribute ids removed prefixes like "generic." and "player.".
        // Accept either full key or short key, e.g.:
        // - minecraft:movement_speed (full, new id)
        // - movement_speed (short, new id)
        // - minecraft:generic.movement_speed (legacy full id)
        // - generic.movement_speed (legacy, minecraft namespace implied)
        NamespacedKey direct = raw.contains(":") ? NamespacedKey.fromString(raw) : NamespacedKey.minecraft(raw);
        if (direct != null) {
            Attribute a = registry.get(direct);
            if (a != null) {
                return a;
            }
        }

        String namespace = "minecraft";
        String keyPart = raw;
        int colon = raw.indexOf(':');
        if (colon != -1) {
            namespace = raw.substring(0, colon);
            keyPart = raw.substring(colon + 1);
        }

        if (!keyPart.contains(".")) {
            // Legacy fallback (pre 1.21.2)
            NamespacedKey generic = NamespacedKey.fromString(namespace + ":generic." + keyPart);
            if (generic != null) {
                Attribute a = registry.get(generic);
                if (a != null) {
                    return a;
                }
            }

            NamespacedKey player = NamespacedKey.fromString(namespace + ":player." + keyPart);
            if (player != null) {
                return registry.get(player);
            }
        }

        return null;
    }

    @Override
    public void execute(Player player, String foodId, com.github.cinnaio.foodEngine.model.ParsedAction action) {
        String params = action.getParameters();
        if (params == null || params.isEmpty()) {
            return;
        }
        String[] parts = params.split("\\s+");
        if (parts.length < 3) {
            return;
        }
        Attribute attribute = findAttribute(parts[0]);
        if (attribute == null) {
            return;
        }

        double amount;
        long durationTicks;
        try {
            amount = Double.parseDouble(parts[1]);
            durationTicks = Long.parseLong(parts[2]);
        } catch (NumberFormatException ex) {
            return;
        }

        NamespacedKey modifierKey = NamespacedKey.fromString("foodengine:food-" + foodId + "-" + System.nanoTime());
        if (modifierKey == null) {
            return;
        }

        SchedulerUtil.runForPlayer(player, p -> {
            AttributeInstance instance = p.getAttribute(attribute);
            if (instance == null) {
                return;
            }
            AttributeModifier modifier = new AttributeModifier(modifierKey, amount, AttributeModifier.Operation.ADD_NUMBER);
            instance.addModifier(modifier);
        });

        SchedulerUtil.runForPlayerLater(player, durationTicks, p -> {
            AttributeInstance instance = p.getAttribute(attribute);
            if (instance != null) {
                instance.getModifiers().stream()
                        .filter(mod -> modifierKey.equals(mod.getKey()))
                        .findFirst()
                        .ifPresent(instance::removeModifier);
            }
        });
    }
}

