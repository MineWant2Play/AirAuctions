package com.ftxeven.airauctions.api.papi;

import com.ftxeven.airauctions.model.ListingScope;
import com.ftxeven.airauctions.service.ServiceManager;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

final class ListingPlaceholders {

    private final ServiceManager services;

    ListingPlaceholders(ServiceManager services) {
        this.services = services;
    }

    String maxListings(UUID uuid) {
        return String.valueOf(services.validator().maxActiveListings(uuid));
    }

    String availableSlots(UUID uuid) {
        return String.valueOf(services.validator().availableSlots(uuid));
    }

    @Nullable String perPlayer(UUID uuid, String scope) {
        if (scope.equals("total")) {
            return String.valueOf(services.listings().totalCount(uuid));
        }
        ListingScope resolved = scope(scope);
        return resolved != null ? String.valueOf(services.listings().count(uuid, resolved)) : null;
    }

    @Nullable String global(String scope) {
        if (scope.equals("total")) {
            return String.valueOf(services.listings().totalCount(null));
        }
        ListingScope resolved = scope(scope);
        return resolved != null ? String.valueOf(services.listings().count(null, resolved)) : null;
    }

    private @Nullable ListingScope scope(String token) {
        return switch (token) {
            case "active" -> ListingScope.ACTIVE;
            case "expired" -> ListingScope.EXPIRED;
            case "storage" -> ListingScope.STORAGE;
            default -> null;
        };
    }
}