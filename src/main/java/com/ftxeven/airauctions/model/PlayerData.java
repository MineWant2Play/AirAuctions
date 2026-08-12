package com.ftxeven.airauctions.model;

import java.util.Map;
import java.util.UUID;

public record PlayerData(
        UUID uuid,
        String name,
        PlayerData.Skin skin,
        int extraSlots,
        Map<String, Double> pendingEarnings,
        Map<String, Double> pendingRefunds
) {
    public PlayerData {
        pendingEarnings = Map.copyOf(pendingEarnings);
        pendingRefunds = Map.copyOf(pendingRefunds);
    }

    public record Skin(String value, String signature) {
        public static final Skin EMPTY = new Skin("", "");

        public boolean isPresent() {
            return value != null && !value.isEmpty() && signature != null && !signature.isEmpty();
        }
    }
}