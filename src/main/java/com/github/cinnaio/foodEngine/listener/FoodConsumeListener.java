package com.github.cinnaio.foodEngine.listener;

import com.github.cinnaio.foodEngine.FoodEngine;
import com.github.cinnaio.foodEngine.manager.ActionManager;
import com.github.cinnaio.foodEngine.manager.ComboManager;
import com.github.cinnaio.foodEngine.manager.FoodHistoryManager;
import com.github.cinnaio.foodEngine.manager.FoodRegistry;
import com.github.cinnaio.foodEngine.model.FoodDefinition;
import com.github.cinnaio.foodEngine.model.ParsedAction;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FoodConsumeListener implements Listener {

    private final FoodEngine plugin;
    private final FoodRegistry foodRegistry;
    private final ActionManager actionManager;
    private final FoodHistoryManager historyManager;
    private final ComboManager comboManager;
    private final NamespacedKey foodIdKey;

    public FoodConsumeListener(FoodEngine plugin,
                               FoodRegistry foodRegistry,
                               ActionManager actionManager,
                               FoodHistoryManager historyManager,
                               ComboManager comboManager,
                               NamespacedKey foodIdKey) {
        this.plugin = plugin;
        this.foodRegistry = foodRegistry;
        this.actionManager = actionManager;
        this.historyManager = historyManager;
        this.comboManager = comboManager;
        this.foodIdKey = foodIdKey;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (plugin.isDebug()) {
            plugin.getLogger().info("Item: " + item.getType());
            plugin.getLogger().info("Item meta: " + meta);
            plugin.getLogger().info("PDC keys: " + container.getKeys());
        }

        String foodId = container.get(foodIdKey, PersistentDataType.STRING);
        if (foodId == null || foodId.isEmpty()) {
            // Fallback: accept any namespace, as long as key == "food_engine_id"
            Set<NamespacedKey> keys = container.getKeys();
            for (NamespacedKey key : keys) {
                if (!"food_engine_id".equalsIgnoreCase(key.getKey())) {
                    continue;
                }
                String candidate = container.get(key, PersistentDataType.STRING);
                if (candidate != null && !candidate.isEmpty()) {
                    foodId = candidate;
                    if (plugin.isDebug()) {
                        plugin.getLogger().info("Resolved food id via fallback key '" + key + "': " + foodId);
                    }
                    break;
                }
            }
        }
        if (foodId == null || foodId.isEmpty()) {
            if (plugin.isDebug()) {
                plugin.getLogger().info("No food_engine_id found in PDC for consumed item.");
            }
        }

        long now = System.currentTimeMillis();
        // Decide whether to record history:
        // - Always record if it's a FoodEngine item (has food_engine_id)
        // - If it has no FoodEngine id:
        //     - Record only when it's NOT a CraftEngine custom item
        boolean hasFoodEngineId = foodId != null && !foodId.isEmpty();
        boolean isCraftEngineItem = false;
        try {
            isCraftEngineItem = CraftEngineItems.isCustomItem(item);
        } catch (Throwable ignored) {
            // CraftEngine might be absent or not initialized; treat as non-custom
        }
        if (hasFoodEngineId || !isCraftEngineItem) {
            historyManager.record(player.getUniqueId(), hasFoodEngineId ? foodId : null, item.getType(), now);
        }

        if (foodId == null || foodId.isEmpty()) {
            return;
        }
        plugin.getDrinkHistoryManager().record(player.getUniqueId(), foodId, now);

        FoodDefinition def = foodRegistry.getDefinition(foodId);
        if (def == null) {
            if (plugin.isDebug()) {
                plugin.getLogger().info("No food registered for food id '" + foodId + "'");
            }
            return;
        }

        Deque<FoodHistoryManager.FoodHistoryEntry> history = historyManager.snapshot(player.getUniqueId());
        List<ParsedAction> triggered = comboManager.tryTrigger(
                player.getUniqueId(),
                foodId,
                def.conditions(),
                def.actions(),
                def.overuseActions(),
                history,
                now
        );
        if (triggered != null) {
            if (!triggered.isEmpty()) {
                actionManager.executeActions(player, foodId, triggered);
                return;
            } else {
                return;
            }
        }

        List<ParsedAction> actions = def.actions();
        if (actions == null || actions.isEmpty()) {
            if (plugin.isDebug()) {
                plugin.getLogger().info("No actions registered for food id '" + foodId + "'");
            }
            return;
        }

        actionManager.executeActions(player, foodId, actions);
    }
}
