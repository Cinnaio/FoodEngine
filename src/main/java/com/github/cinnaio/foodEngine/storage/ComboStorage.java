package com.github.cinnaio.foodEngine.storage;

import java.util.Map;
import java.util.UUID;

public interface ComboStorage extends AutoCloseable {

    int incrementTriggerCount(UUID playerId, String foodId, int comboIndex);

    Map<String, Integer> getTriggerCountsFor(UUID playerId);

    @Override
    void close();
}
