package com.ftxeven.airauctions.service;

import java.util.Map;

public sealed interface ActionResult<T> {

    record Success<T>(T value) implements ActionResult<T> {}

    record Denied<T>(String langKey, Map<String, String> placeholders) implements ActionResult<T> {}

    static <T> ActionResult<T> success(T value) {
        return new Success<>(value);
    }

    static <T> ActionResult<T> denied(String langKey) {
        return new Denied<>(langKey, Map.of());
    }

    static <T> ActionResult<T> denied(String langKey, Map<String, String> placeholders) {
        return new Denied<>(langKey, placeholders);
    }

    // lifts a denied Eligibility straight into a result of any shape
    static <T> ActionResult<T> denied(Eligibility.Denied denial) {
        return new Denied<>(denial.langKey(), denial.placeholders());
    }

    default boolean ok() {
        return this instanceof Success<T>;
    }
}