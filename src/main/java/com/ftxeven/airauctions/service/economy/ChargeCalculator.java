package com.ftxeven.airauctions.service.economy;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.config.ExpansionsConfig;
import com.ftxeven.airauctions.economy.EconomyProvider;
import com.ftxeven.airauctions.permission.PermissionTiers;
import org.bukkit.permissions.Permissible;

final class ChargeCalculator {

    private final ConfigManager configs;

    ChargeCalculator(ConfigManager configs) {
        this.configs = configs;
    }

    // charged to the seller when a listing is created
    EconomyService.ChargeResult fee(Permissible seller, EconomyProvider provider, double price) {
        return resolve(EconomyService.ChargeKind.FEE, configs.expansions().economy().listingFee(), seller, provider, price);
    }

    // deducted from the seller's payout when a sale completes
    EconomyService.ChargeResult tax(Permissible seller, EconomyProvider provider, double price) {
        return resolve(EconomyService.ChargeKind.TAX, configs.expansions().economy().salesTax(), seller, provider, price);
    }

    // a percentage-based tax charged against a listing's initial price needs recomputing
    // against the final sale price once that's known - taxRate < 0 means the original charge
    // was FIXED or waived (already final)
    double finalizeTax(EconomyProvider provider, double taxRate, double storedTax, double finalPrice) {
        if (taxRate < 0) {
            return storedTax;
        }
        return amount(provider, configs.expansions().economy().salesTax(), taxRate, finalPrice, true);
    }

    private EconomyService.ChargeResult resolve(EconomyService.ChargeKind kind, ExpansionsConfig.Charge config, Permissible seller, EconomyProvider provider, double price) {
        if (!config.enabled() || seller.hasPermission(kind.bypassPermission())) {
            return EconomyService.ChargeResult.waived(kind);
        }

        double value = PermissionTiers.resolve(seller, kind.bypassPermission(), PermissionTiers.Pick.LOWEST)
                .orElse(config.defaultValue());

        boolean percentage = config.type() == ExpansionsConfig.ChargeType.PERCENTAGE;
        double amount = amount(provider, config, value, price, percentage);

        return new EconomyService.ChargeResult(kind, amount, percentage ? value : -1, false);
    }

    private double amount(EconomyProvider provider, ExpansionsConfig.Charge config, double value, double price, boolean percentage) {
        double amount = percentage ? clamp(price * (value / 100.0), config.min(), config.max()) : value;
        return provider.allowDecimals() ? amount : Math.round(amount);
    }

    private static double clamp(double amount, double min, double max) {
        return max < 0 ? Math.max(amount, min) : Math.clamp(amount, min, max);
    }
}