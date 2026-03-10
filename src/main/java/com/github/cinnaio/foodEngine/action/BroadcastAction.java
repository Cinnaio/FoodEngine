package com.github.cinnaio.foodEngine.action;

import com.github.cinnaio.foodEngine.util.SchedulerUtil;
import com.github.cinnaio.foodEngine.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BroadcastAction implements ActionExecutor {

    @Override
    public void execute(Player player, String foodId, com.github.cinnaio.foodEngine.model.ParsedAction action) {
        String params = action.getParameters();
        if (params == null || params.isEmpty()) {
            return;
        }
        Component message = TextUtil.toComponent(params, player);
        SchedulerUtil.runGlobal(() -> Bukkit.getServer().sendMessage(message));
    }
}

