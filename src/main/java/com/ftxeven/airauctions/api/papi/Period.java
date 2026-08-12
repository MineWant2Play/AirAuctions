package com.ftxeven.airauctions.api.papi;

import com.ftxeven.airauctions.config.MainConfig;
import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

enum Period {

    DAILY, WEEKLY, MONTHLY, YEARLY, TOTAL;

    static @Nullable Period parse(String token) {
        try {
            return valueOf(token.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable Instant since(MainConfig.Formatting formatting) {
        if (this == TOTAL) {
            return null;
        }

        ZoneId zone = formatting.timezone();
        LocalDate today = LocalDate.now(zone);
        LocalDate start = switch (this) {
            case DAILY -> today;
            case WEEKLY -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> today.withDayOfMonth(1);
            case YEARLY -> today.withDayOfYear(1);
            case TOTAL -> throw new IllegalStateException("unreachable");
        };
        return start.atStartOfDay(zone).toInstant();
    }
}