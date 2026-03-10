package com.github.cinnaio.foodEngine.condition;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface Condition {

    boolean evaluate(Player player);
}

