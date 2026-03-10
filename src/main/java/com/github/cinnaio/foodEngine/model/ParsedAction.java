package com.github.cinnaio.foodEngine.model;

import com.github.cinnaio.foodEngine.condition.Condition;

import java.util.Collections;
import java.util.Map;

public class ParsedAction {

    private final String id;
    private final String parameters;
    private final Map<String, String> flags;
    private final Condition condition;

    private final Double chanceProbability;
    private final ParsedAction nestedAction;

    public ParsedAction(String id,
                        String parameters,
                        Map<String, String> flags,
                        Condition condition) {
        this(id, parameters, flags, condition, null, null);
    }

    public ParsedAction(String id,
                        String parameters,
                        Map<String, String> flags,
                        Condition condition,
                        Double chanceProbability,
                        ParsedAction nestedAction) {
        this.id = id.toLowerCase();
        this.parameters = parameters;
        this.flags = flags == null ? Collections.emptyMap() : Collections.unmodifiableMap(flags);
        this.condition = condition;
        this.chanceProbability = chanceProbability;
        this.nestedAction = nestedAction;
    }

    public String getId() {
        return id;
    }

    public String getParameters() {
        return parameters;
    }

    public Map<String, String> getFlags() {
        return flags;
    }

    public Condition getCondition() {
        return condition;
    }

    public Double getChanceProbability() {
        return chanceProbability;
    }

    public ParsedAction getNestedAction() {
        return nestedAction;
    }
}

