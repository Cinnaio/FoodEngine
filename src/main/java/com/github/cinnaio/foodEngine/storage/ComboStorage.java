package com.github.cinnaio.foodEngine.storage;

import java.util.Map;
import java.util.UUID;

public interface ComboStorage extends AutoCloseable {

    long getCooldownUntilMillis(UUID playerId, String foodId, int comboIndex);

    void setCooldownUntilMillis(UUID playerId, String foodId, int comboIndex, long cooldownUntilMillis);

    int incrementTriggerCount(UUID playerId, String foodId, int comboIndex);

    Map<String, Long> getCooldownsFor(UUID playerId, long nowMillis);

    Map<String, Integer> getTriggerCountsFor(UUID playerId);

    @Override
    void close();
}

