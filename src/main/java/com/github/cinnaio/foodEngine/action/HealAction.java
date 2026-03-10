package com.github.cinnaio.foodEngine.action;

import com.github.cinnaio.foodEngine.util.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public class HealAction implements ActionExecutor {

    private static Attribute maxHealthAttribute() {
        Registry<Attribute> registry = Bukkit.getRegistry(Attribute.class);
        Attribute modern = registry.get(NamespacedKey.minecraft("max_health"));
        if (modern != null) {
            return modern;
        }
        return registry.get(NamespacedKey.minecraft("generic.max_health"));
    }

    @Override
    public void execute(Player player, String foodId, com.github.cinnaio.foodEngine.model.ParsedAction action) {
        String params = action.getParameters();
        if (params == null || params.isEmpty()) {
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(params.trim());
        } catch (NumberFormatException ex) {
            return;
        }

        Attribute maxHealthAttrType = maxHealthAttribute();
        SchedulerUtil.runForPlayer(player, p -> {
            AttributeInstance maxHealthAttr = maxHealthAttrType != null ? p.getAttribute(maxHealthAttrType) : null;
            double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
            double newHealth = Math.min(maxHealth, p.getHealth() + amount);
            p.setHealth(newHealth);
        });
    }
}

