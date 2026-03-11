package com.github.cinnaio.foodEngine.manager;

import com.github.cinnaio.foodEngine.model.FoodConditions;
import com.github.cinnaio.foodEngine.model.FoodDefinition;
import com.github.cinnaio.foodEngine.model.ParsedAction;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodRegistry {

    private final Map<String, FoodDefinition> foods = new HashMap<>();

    public void clear() {
        foods.clear();
    }

    public void register(String id,
                         List<ParsedAction> actions,
                         FoodConditions conditions,
                         List<ParsedAction> overuseActions) {
        foods.put(id, new FoodDefinition(
                id,
                actions == null ? List.of() : List.copyOf(actions),
                conditions,
                overuseActions == null ? List.of() : List.copyOf(overuseActions)
        ));
    }

    public List<ParsedAction> getActions(String id) {
        FoodDefinition def = foods.get(id);
        return def == null ? Collections.emptyList() : def.actions();
    }

    public FoodDefinition getDefinition(String id) {
        return foods.get(id);
    }

    public Map<String, FoodDefinition> getAll() {
        return Collections.unmodifiableMap(foods);
    }
}
