package com.github.cinnaio.foodEngine.manager;

import com.github.cinnaio.foodEngine.FoodEngine;
import com.github.cinnaio.foodEngine.action.AttributeAction;
import com.github.cinnaio.foodEngine.action.BroadcastAction;
import com.github.cinnaio.foodEngine.action.CommandAction;
import com.github.cinnaio.foodEngine.action.HealAction;
import com.github.cinnaio.foodEngine.action.MessageAction;
import com.github.cinnaio.foodEngine.action.PotionAction;
import com.github.cinnaio.foodEngine.action.TitleAction;
import com.github.cinnaio.foodEngine.action.ActionExecutor;
import com.github.cinnaio.foodEngine.model.ParsedAction;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ActionManager {

    private final FoodEngine plugin;
    private final FoodRegistry registry;
    private final Map<String, ActionExecutor> executors = new HashMap<>();
    private final Random random = new Random();

    public ActionManager(FoodEngine plugin,
                         com.github.cinnaio.foodEngine.parser.ActionParser parser,
                         FoodRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;

        register("title", new TitleAction());
        register("message", new MessageAction());
        register("broadcast", new BroadcastAction());
        register("command", new CommandAction());
        register("potion", new PotionAction());
        register("heal", new HealAction());
        register("attribute", new AttributeAction());
    }

    private void register(String id, ActionExecutor executor) {
        executors.put(id.toLowerCase(Locale.ROOT), executor);
    }

    public void executeActions(Player player, String foodId, List<ParsedAction> actions) {
        for (ParsedAction action : actions) {
            try {
                if (action.getChanceProbability() != null && action.getNestedAction() != null) {
                    double roll = random.nextDouble();
                    if (roll > action.getChanceProbability()) {
                        continue;
                    }
                    executeSingle(player, foodId, action.getNestedAction());
                } else {
                    executeSingle(player, foodId, action);
                }
            } catch (Exception ex) {
                if (plugin.isDebug()) {
                    plugin.getLogger().warning("Error executing action for food '" + foodId + "': " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    private void executeSingle(Player player, String foodId, ParsedAction action) {
        if (action.getCondition() != null) {
            com.github.cinnaio.foodEngine.model.FoodDefinition def = registry.getDefinition(foodId);
            long windowSeconds = 0L;
            if (def != null && def.conditions() != null) {
                windowSeconds = Math.max(0L, def.conditions().maxIntervalSeconds());
            }
            long now = System.currentTimeMillis();
            com.github.cinnaio.foodEngine.manager.DrinkHistoryManager.Result r =
                    plugin.getDrinkHistoryManager().compute(player.getUniqueId(), foodId, windowSeconds, now);
            java.util.Map<String, String> ctx = new java.util.HashMap<>();
            ctx.put("%drink_count%", String.valueOf(r.count()));
            ctx.put("%drink_interval%", String.valueOf(r.intervalSeconds()));
            com.github.cinnaio.foodEngine.parser.ConditionParser.setContext(ctx);
            boolean ok = action.getCondition().evaluate(player);
            com.github.cinnaio.foodEngine.parser.ConditionParser.clearContext();
            if (!ok) {
                return;
            }
        }

        ActionExecutor executor = executors.get(action.getId());
        if (executor == null) {
            if (plugin.isDebug()) {
                plugin.getLogger().warning("No executor registered for action id '" + action.getId() + "'");
            }
            return;
        }
        executor.execute(player, foodId, action);
    }
}
