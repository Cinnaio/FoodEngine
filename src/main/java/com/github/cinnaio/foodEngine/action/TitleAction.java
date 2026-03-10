package com.github.cinnaio.foodEngine.action;

import com.github.cinnaio.foodEngine.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public class TitleAction implements ActionExecutor {

    @Override
    public void execute(Player player, String foodId, com.github.cinnaio.foodEngine.model.ParsedAction action) {
        String params = action.getParameters();
        if (params == null || params.isEmpty()) {
            return;
        }
        Component title = TextUtil.toComponent(params, player);
        Title full = Title.title(title, Component.empty(),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500)));
        player.showTitle(full);
    }
}

