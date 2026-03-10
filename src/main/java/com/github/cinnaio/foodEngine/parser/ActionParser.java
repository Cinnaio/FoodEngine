package com.github.cinnaio.foodEngine.parser;

import com.github.cinnaio.foodEngine.FoodEngine;
import com.github.cinnaio.foodEngine.condition.Condition;
import com.github.cinnaio.foodEngine.model.ParsedAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActionParser {

    private final ConditionParser conditionParser;

    public ActionParser(ConditionParser conditionParser) {
        this.conditionParser = conditionParser;
    }

    public ParsedAction parse(String raw) {
        String line = raw.trim();
        if (line.isEmpty()) {
            return null;
        }

        Condition condition = null;
        int condStart = line.lastIndexOf("{condition:");
        if (condStart != -1 && line.endsWith("}")) {
            String condExpr = line.substring(condStart + "{condition:".length(), line.length() - 1);
            condition = conditionParser.parse(condExpr);
            line = line.substring(0, condStart).trim();
        }

        if (!line.startsWith("[")) {
            return null;
        }
        int closing = line.indexOf(']');
        if (closing <= 1) {
            return null;
        }

        String id = line.substring(1, closing).trim().toLowerCase(Locale.ROOT);
        String rest = line.substring(closing + 1).trim();

        if (id.equals("chance")) {
            String[] split = rest.split("\\s+", 2);
            if (split.length < 2) {
                return null;
            }
            double probability;
            try {
                probability = Double.parseDouble(split[0]);
            } catch (NumberFormatException ex) {
                return null;
            }
            ParsedAction nested = parse(split[1]);
            if (nested == null) {
                return null;
            }
            return new ParsedAction(id, "", Map.of(), condition, probability, nested);
        }

        Map<String, String> flags = new HashMap<>();
        String parameters = rest;

        if (id.equals("command") && !rest.isEmpty()) {
            String[] tokens = rest.split("\\s+");
            List<String> cmdParts = new ArrayList<>();
            for (String token : tokens) {
                if (token.startsWith("-as:")) {
                    String mode = token.substring("-as:".length()).toLowerCase(Locale.ROOT);
                    flags.put("as", mode);
                } else {
                    cmdParts.add(token);
                }
            }
            parameters = String.join(" ", cmdParts).trim();
        }

        return new ParsedAction(id, parameters, flags, condition);
    }

    public List<ParsedAction> parseList(List<String> list) {
        List<ParsedAction> result = new ArrayList<>();
        if (list == null) {
            return result;
        }
        for (String raw : list) {
            ParsedAction action = parse(raw);
            if (action != null) {
                result.add(action);
            }
        }
        return result;
    }
}

