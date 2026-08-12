package com.ftxeven.airauctions.model;

import java.util.UUID;

public record BidEntry(
        UUID bidder,
        double offer,
        int totalOffers
) {}