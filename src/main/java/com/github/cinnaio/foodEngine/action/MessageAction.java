package com.github.cinnaio.foodEngine.action;

import com.github.cinnaio.foodEngine.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class MessageAction implements ActionExecutor {

    @Override
    public void execute(Player player, String foodId, com.github.cinnaio.foodEngine.model.ParsedAction action) {
        String params = action.getParameters();
        if (params == null || params.isEmpty()) {
            return;
        }
        Component message = TextUtil.toComponent(params, player);
        player.sendMessage(message);
    }
}

