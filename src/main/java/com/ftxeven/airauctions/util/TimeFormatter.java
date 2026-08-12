package com.ftxeven.airauctions.util;

import com.ftxeven.airauctions.config.ExpansionsConfig;
import com.ftxeven.airauctions.config.LangConfig;
import com.ftxeven.airauctions.config.MainConfig;
import com.ftxeven.airauctions.model.Listing;
import com.ftxeven.airauctions.model.ListingScope;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

public final class TimeFormatter {

    private static final String[] UNITS = {"day", "hour", "minute", "second"};

    private TimeFormatter() {
    }

    // Date / time

    public static String date(Instant instant, MainConfig.Formatting formatting) {
        return formatting.date().format(instant);
    }

    public static String time(Instant instant, MainConfig.Formatting formatting) {
        return formatting.time().format(instant);
    }

    // Duration

    // a fixed span, not counted down to anything
    public static String duration(Duration span, MainConfig.Formatting formatting, LangConfig lang) {
        return render(span.isNegative() ? Duration.ZERO : span, formatting.duration(), lang);
    }

    // time remaining until an instant - %expires%, %purges%
    public static String duration(Instant until, MainConfig.Formatting formatting, LangConfig lang) {
        return duration(roundUpToSeconds(Duration.between(Instant.now(), until)), formatting, lang);
    }

    private static Duration roundUpToSeconds(Duration span) {
        int nanos = span.getNano();
        return nanos == 0 ? span : span.plusNanos(1_000_000_000L - nanos);
    }

    // same as above, but for a countdown that might not exist at all
    public static String durationOrNever(@Nullable Instant until, MainConfig.Formatting formatting, LangConfig lang) {
        if (until == null || until.equals(Listing.NEVER_EXPIRES)) {
            return lang.get("placeholders.never").getFirst();
        }
        return duration(until, formatting, lang);
    }

    public static String durationOrUnlimited(long seconds, MainConfig.Formatting formatting, LangConfig lang) {
        if (seconds < 0) {
            return lang.get("placeholders.unlimited").getFirst();
        }
        return duration(Duration.ofSeconds(seconds), formatting, lang);
    }

    // Discord timestamps

    public static String discordTimestamp(@Nullable Instant instant, ExpansionsConfig.TimeFormat format, String fallbackText) {
        if (instant == null || instant.equals(Listing.NEVER_EXPIRES)) {
            return fallbackText;
        }
        long epoch = instant.getEpochSecond();
        return switch (format) {
            case TEXT -> fallbackText;
            case RELATIVE -> "<t:" + epoch + ":R>";
            case DATETIME -> "<t:" + epoch + ":f>";
            case DATETIME_RELATIVE -> "<t:" + epoch + ":f> (<t:" + epoch + ":R>)";
        };
    }

    // %purges% from a raw ended-at instant and an explicit delay
    public static String purgesIn(Instant endedAt, int delaySeconds, MainConfig.Formatting formatting, LangConfig lang) {
        Instant purgeAt = delaySeconds < 0 ? null : endedAt.plusSeconds(delaySeconds);
        return durationOrNever(purgeAt, formatting, lang);
    }

    // %purges% for a listing that has already ended, resolving the purge delay for
    // whichever scope its terminal status belongs
    public static String purgesIn(Listing.Info info, MainConfig config, LangConfig lang) {
        ListingScope scope = ListingScope.forStatus(info.status());
        Instant endedAt = info.endedAt() != null ? info.endedAt() : Instant.now();
        return purgesIn(endedAt, config.purgeDelaySeconds(scope), config.formatting(), lang);
    }

    private static String render(Duration span, MainConfig.DurationStyle style, LangConfig lang) {
        long[] values = {span.toDaysPart(), span.toHoursPart(), span.toMinutesPart(), span.toSecondsPart()};
        int limit = switch (style.mode()) {
            case DETAILED -> values.length;
            case SEQUENTIAL -> 1;
            case CUSTOM -> style.granularity();
        };

        StringBuilder joined = new StringBuilder();
        int shown = 0;
        for (int i = 0; i < values.length && shown < limit; i++) {
            if (values[i] <= 0) {
                continue;
            }
            if (!joined.isEmpty()) {
                joined.append(' ');
            }
            joined.append(values[i]).append(label(lang, UNITS[i], values[i]));
            shown++;
        }

        return shown > 0 ? joined.toString() : "0" + label(lang, "second", 0);
    }

    private static String label(LangConfig lang, String unit, long value) {
        String key = "placeholders.time." + unit + (value == 1 ? "" : "s");
        return lang.get(key).getFirst();
    }
}