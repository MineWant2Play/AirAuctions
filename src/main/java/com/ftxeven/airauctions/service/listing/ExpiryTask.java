package com.ftxeven.airauctions.service.listing;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.model.Listing;
import com.ftxeven.airauctions.model.ListingScope;
import com.ftxeven.airauctions.service.listing.workflow.AuctionService;
import com.ftxeven.airauctions.service.listing.workflow.BidService;
import com.ftxeven.airauctions.util.Scheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class ExpiryTask {

    private final ConfigManager configs;
    private final ListingService listings;
    private final AuctionService auctions;
    private final BidService bids;
    private final Logger logger;

    private ScheduledTask task;

    public ExpiryTask(ConfigManager configs, ListingService listings, AuctionService auctions, BidService bids, Logger logger) {
        this.configs = configs;
        this.listings = listings;
        this.auctions = auctions;
        this.bids = bids;
        this.logger = logger;
    }

    public void start() {
        int intervalSeconds = Math.max(1, configs.main().listings().sweepInterval());
        task = Scheduler.runAsyncTimer(this::sweep, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void restart() {
        stop();
        start();
    }

    private void sweep() {
        try {
            expireDue();
            listings.purgeDue(ListingScope.EXPIRED, configs.main().listings().expiredPurgeDelay());
            listings.purgeDue(ListingScope.STORAGE, configs.main().bids().collectPurgeDelay());
        } catch (Exception e) {
            logger.warning("Expiry sweep failed, will retry next cycle: " + e.getMessage());
        }
    }

    private void expireDue() {
        for (Listing.Info due : listings.dueToExpire()) {
            try {
                listings.find(due.id()).ifPresent(this::settle);
            } catch (Exception e) {
                logger.warning("Could not settle expired listing " + due.id() + ", will retry next cycle: " + e.getMessage());
            }
        }
    }

    private void settle(Listing listing) {
        switch (listing) {
            case Listing.Auction auction -> auctions.expire(auction);
            case Listing.Bid bid -> bids.expire(bid);
        }
    }
}