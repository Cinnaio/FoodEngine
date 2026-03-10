package com.github.cinnaio.foodEngine.manager;

import com.github.cinnaio.foodEngine.model.FoodCombo;
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
                                        List<FoodCombo> combos,
                                        Deque<FoodHistoryManager.FoodHistoryEntry> history,
                                        long nowMillis) {
        if (combos == null || combos.isEmpty()) {
            return null;
        }

        boolean anyOnCooldown = false;

        for (int i = 0; i < combos.size(); i++) {
            FoodCombo combo = combos.get(i);
            long until = storage.getCooldownUntilMillis(playerId, currentFoodId, i);
            if (until > nowMillis) {
                anyOnCooldown = true;
                continue;
            }

            if (!matches(combo.conditions(), history, nowMillis)) {
                continue;
            }

            long cdMillis = Math.max(0L, combo.cooldownSeconds()) * 1000L;
            if (cdMillis > 0) {
                storage.setCooldownUntilMillis(playerId, currentFoodId, i, nowMillis + cdMillis);
            }
            storage.incrementTriggerCount(playerId, currentFoodId, i);
            return combo.actions();
        }

        return anyOnCooldown ? List.of() : null;
    }

    private boolean matches(FoodCombo.ComboConditions conditions,
                            Deque<FoodHistoryManager.FoodHistoryEntry> history,
                            long nowMillis) {
        if (conditions == null) {
            return false;
        }
        long windowMillis = Math.max(0L, conditions.maxIntervalSeconds()) * 1000L;
        int trigger = Math.max(1, conditions.trigger());

        String targetFoodId = conditions.foodEngineId();
        Material targetMaterial = conditions.material();

        if ((targetFoodId == null || targetFoodId.isEmpty()) && targetMaterial == null) {
            return false;
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
                if (count >= trigger) {
                    return true;
                }
            }
        }
        return false;
    }

    public java.util.Map<String, Long> getCooldownsFor(UUID playerId, long nowMillis) {
        return storage.getCooldownsFor(playerId, nowMillis);
    }

    public java.util.Map<String, Integer> getTriggerCountsFor(UUID playerId) {
        return storage.getTriggerCountsFor(playerId);
    }
}

