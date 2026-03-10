package com.github.cinnaio.foodEngine.model;

import java.util.List;

public record FoodDefinition(
        String id,
        List<ParsedAction> actions,
        List<FoodCombo> combos
) {
}

