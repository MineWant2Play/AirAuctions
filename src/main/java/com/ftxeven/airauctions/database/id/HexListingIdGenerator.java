package com.ftxeven.airauctions.database.id;

import java.security.SecureRandom;

public final class HexListingIdGenerator implements ListingIdGenerator {

    private static final char[] ALPHABET = "0123456789abcdef".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int length;

    public HexListingIdGenerator(int length) {
        this.length = length;
    }

    @Override
    public String next() {
        char[] id = new char[length];
        for (int i = 0; i < length; i++) {
            id[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        }
        return new String(id);
    }
}