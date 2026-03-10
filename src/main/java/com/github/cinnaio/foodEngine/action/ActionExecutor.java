package com.github.cinnaio.foodEngine.action;

import com.github.cinnaio.foodEngine.model.ParsedAction;
import org.bukkit.entity.Player;

public interface ActionExecutor {

    void execute(Player player, String foodId, ParsedAction action);
}

