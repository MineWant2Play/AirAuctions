package com.ftxeven.airauctions.model;

import java.util.List;

public record MatchRules(
        List<String> materials,
        List<String> names,
        List<String> lores,
        List<String> enchantments,
        List<String> nbtKeys,
        List<String> customModelData,
        List<String> itemModels,
        List<String> pluginItems
) {
    public static final MatchRules EMPTY = new MatchRules(
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    public MatchRules {
        materials = List.copyOf(materials);
        names = List.copyOf(names);
        lores = List.copyOf(lores);
        enchantments = List.copyOf(enchantments);
        nbtKeys = List.copyOf(nbtKeys);
        customModelData = List.copyOf(customModelData);
        itemModels = List.copyOf(itemModels);
        pluginItems = List.copyOf(pluginItems);
    }
}