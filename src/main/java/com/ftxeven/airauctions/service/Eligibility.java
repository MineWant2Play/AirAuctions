package com.ftxeven.airauctions.service;

import java.util.Map;

public sealed interface Eligibility {

    record Eligible() implements Eligibility {}

    record Denied(String langKey, Map<String, String> placeholders) implements Eligibility {
        public Denied(String langKey) {
            this(langKey, Map.of());
        }
    }

    static Eligibility eligible() {
        return new Eligible();
    }

    static Eligibility denied(String langKey) {
        return new Denied(langKey);
    }

    static Eligibility denied(String langKey, Map<String, String> placeholders) {
        return new Denied(langKey, placeholders);
    }

    default boolean ok() {
        return this instanceof Eligible;
    }
}