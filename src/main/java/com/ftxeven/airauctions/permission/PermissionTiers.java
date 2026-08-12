package com.ftxeven.airauctions.permission;

import com.ftxeven.airauctions.config.LangConfig;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.OptionalDouble;

public final class PermissionTiers {

    private PermissionTiers() {
    }

    public enum Pick { LOWEST, HIGHEST }

    public static OptionalDouble resolve(Permissible permissible, String base, Pick pick) {
        String prefix = base + ".";
        Double best = null;

        for (PermissionAttachmentInfo info : permissible.getEffectivePermissions()) {
            if (!info.getValue()) {
                continue;
            }

            String permission = info.getPermission();
            if (!permission.startsWith(prefix)) {
                continue;
            }

            String suffix = permission.substring(prefix.length());
            if (suffix.isEmpty() || suffix.indexOf('.') >= 0) {
                continue; // not an exact "<base>.<number>" tail
            }

            double value;
            try {
                value = Double.parseDouble(suffix);
            } catch (NumberFormatException e) {
                continue;
            }

            if (best == null || (pick == Pick.HIGHEST ? value > best : value < best)) {
                best = value;
            }
        }

        return best == null ? OptionalDouble.empty() : OptionalDouble.of(best);
    }

    // the plain "<base>" permission (no suffix) always means unlimited; otherwise the
    // highest tier the player holds raises 'fallback', but never lowers it
    public static double resolveUnlimitedTier(Permissible permissible, String base, double fallback) {
        if (permissible.hasPermission(base)) {
            return -1;
        }
        if (fallback < 0) {
            return -1; // the config default is already unlimited
        }
        OptionalDouble tier = resolve(permissible, base, Pick.HIGHEST);
        return tier.isPresent() ? Math.max(tier.getAsDouble(), fallback) : fallback;
    }

    public static String display(double value, LangConfig lang) {
        return value < 0 ? lang.get("placeholders.unlimited").getFirst() : String.valueOf((long) value);
    }
}