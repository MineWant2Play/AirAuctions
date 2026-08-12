package com.ftxeven.airauctions.service.economy;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.config.ExpansionsConfig;
import com.ftxeven.airauctions.economy.EconomyProvider;

import java.util.OptionalDouble;

final class PriceValidator {

    private final ConfigManager configs;

    PriceValidator(ConfigManager configs) {
        this.configs = configs;
    }

    OptionalDouble parse(String raw, EconomyProvider provider) {
        ExpansionsConfig.Economy config = configs.expansions().economy();
        OptionalDouble parsed = NumberConverter.parse(raw, config.allowShorthandInput(), config.numberFormatSuffixes());
        if (parsed.isEmpty()) {
            return OptionalDouble.empty();
        }

        double value = parsed.getAsDouble();
        if (!provider.allowDecimals() && hasFraction(value)) {
            if (config.decimalHandling() == ExpansionsConfig.DecimalHandling.REJECT) {
                return OptionalDouble.empty();
            }
            value = Math.floor(value);
        }

        return value > 0 ? OptionalDouble.of(value) : OptionalDouble.empty();
    }

    double min() {
        return configs.expansions().economy().minPrice();
    }

    double max() {
        return configs.expansions().economy().maxPrice();
    }

    double minPartial() {
        return configs.expansions().economy().minPartialPrice();
    }

    boolean meetsMin(double price) {
        return price >= min();
    }

    boolean meetsMax(double price) {
        return max() < 0 || price <= max();
    }

    boolean isInRange(double price) {
        return meetsMin(price) && meetsMax(price);
    }

    private static boolean hasFraction(double value) {
        return value != Math.floor(value);
    }
}