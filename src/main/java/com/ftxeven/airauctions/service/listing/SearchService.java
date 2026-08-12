package com.ftxeven.airauctions.service.listing;

import com.ftxeven.airauctions.config.ConfigManager;

import java.util.Locale;

public final class SearchService {

    private final ConfigManager configs;

    public SearchService(ConfigManager configs) {
        this.configs = configs;
    }

    public String normalize(String rawQuery) {
        if (rawQuery == null) {
            return null;
        }
        String trimmed = rawQuery.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    public ValidationResult validate(String normalizedQuery) {
        if (normalizedQuery == null) {
            return new ValidationResult.Ok();
        }
        int max = maxLength();
        return max >= 0 && normalizedQuery.length() > max
                ? new ValidationResult.QueryTooLong(normalizedQuery.length(), max)
                : new ValidationResult.Ok();
    }

    public int maxLength() {
        return configs.main().restrictions().maxSearchLength();
    }
}