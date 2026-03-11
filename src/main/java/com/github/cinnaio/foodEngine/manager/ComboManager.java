package com.github.cinnaio.foodEngine.manager;

import com.github.cinnaio.foodEngine.model.FoodConditions;
import com.github.cinnaio.foodEngine.model.ParsedAction;
import com.github.cinnaio.foodEngine.storage.ComboStorage;
import org.bukkit.Material;

import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class ComboManager {

    private final ComboStorage storage;

    public ComboManager(ComboStorage storage) {
        this.storage = storage;
    }

    public List<ParsedAction> tryTrigger(UUID playerId,
                                         String currentFoodId,
                                         FoodConditions conditions,
                                         List<ParsedAction> actions,
                                         List<ParsedAction> overuseActions,
                                         Deque<FoodHistoryManager.FoodHistoryEntry> history,
                                         long nowMillis) {
        if (conditions == null) {
            return actions;
        }

        int count = countMatches(conditions, history, nowMillis);
        Integer overuse = conditions.overuseTrigger();
        if (overuse != null && count >= overuse) {
            storage.incrementTriggerCount(playerId, currentFoodId, 0);
            return overuseActions == null ? List.of() : overuseActions;
        }
        if (count >= Math.max(1, conditions.trigger())) {
            storage.incrementTriggerCount(playerId, currentFoodId, 0);
            return actions;
        }
        return null;
    }

    private int countMatches(FoodConditions conditions,
                            Deque<FoodHistoryManager.FoodHistoryEntry> history,
                            long nowMillis) {
        long windowMillis = Math.max(0L, conditions.maxIntervalSeconds()) * 1000L;

        String targetFoodId = conditions.foodEngineId();
        Material targetMaterial = conditions.material();

        if ((targetFoodId == null || targetFoodId.isEmpty()) && targetMaterial == null) {
            return 0;
        }

        int count = 0;
        for (FoodHistoryManager.FoodHistoryEntry entry : history) {
            if (windowMillis > 0 && entry.timestampMillis() < nowMillis - windowMillis) {
                continue;
            }

            boolean ok = false;
            if (targetFoodId != null && !targetFoodId.isEmpty()) {
                ok = entry.foodEngineId() != null && targetFoodId.equalsIgnoreCase(entry.foodEngineId());
            }
            if (!ok && targetMaterial != null) {
                ok = targetMaterial == entry.material();
            }

            if (ok) {
                count++;
            }
        }
        return count;
    }

    public java.util.Map<String, Integer> getTriggerCountsFor(UUID playerId) {
        return storage.getTriggerCountsFor(playerId);
    }
}
