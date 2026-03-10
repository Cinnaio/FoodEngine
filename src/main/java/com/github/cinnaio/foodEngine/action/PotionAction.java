package com.github.cinnaio.foodEngine.action;

import com.github.cinnaio.foodEngine.util.SchedulerUtil;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionAction implements ActionExecutor {

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
        PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
        if (type == null) {
            return;
        }
        int duration;
        int amplifier;
        try {
            duration = Integer.parseInt(parts[1]);
            amplifier = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ex) {
            return;
        }

        int ticks = duration;
        SchedulerUtil.runForPlayer(player, p ->
                p.addPotionEffect(new PotionEffect(type, ticks, amplifier, true, true, true))
        );
    }
}

