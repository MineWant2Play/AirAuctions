package com.ftxeven.airauctions.database.id;

// SQL drivers using AUTO_INCREMENT don't need this - the database assigns the key and
// the repository reads it back. Only comes into play for listing-id-format: UUID on any
// driver, or MongoDB's AUTO_INCREMENT, which has no native equivalent
public interface ListingIdGenerator {

    String next();
}