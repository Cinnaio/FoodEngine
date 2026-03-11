package com.github.cinnaio.foodEngine.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryComboStorage implements ComboStorage {

    private final Map<String, Integer> triggerCounts = new ConcurrentHashMap<>();

    @Override
    public int incrementTriggerCount(UUID playerId, String foodId, int comboIndex) {
        String k = key(playerId, foodId, comboIndex);
        return triggerCounts.merge(k, 1, Integer::sum);
    }

    @Override
    public Map<String, Integer> getTriggerCountsFor(UUID playerId) {
        String prefix = playerId + "|";
        Map<String, Integer> result = new HashMap<>();
        triggerCounts.forEach((k, count) -> {
            if (!k.startsWith(prefix)) {
                return;
            }
            result.put(k.substring(prefix.length()), count);
        });
        return result;
    }

    @Override
    public void close() {
        // no-op
    }

    private String key(UUID playerId, String foodId, int comboIndex) {
        return playerId + "|" + foodId + "|" + comboIndex;
    }
}
