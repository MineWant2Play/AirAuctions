package com.ftxeven.airauctions.service.player;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.database.query.ListingQuery;
import com.ftxeven.airauctions.model.Listing;
import com.ftxeven.airauctions.model.ListingScope;
import com.ftxeven.airauctions.model.PlayerData;
import com.ftxeven.airauctions.service.economy.EconomyService;
import com.ftxeven.airauctions.service.listing.ListingService;
import com.ftxeven.airauctions.service.listing.workflow.BidService;
import com.ftxeven.airauctions.util.ItemDisplay;
import com.ftxeven.airauctions.util.Messenger;
import com.ftxeven.airauctions.util.PlaceholderMap;
import com.ftxeven.airauctions.util.Placeholders;
import com.ftxeven.airauctions.util.Scheduler;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class NotificationService {

    private final ConfigManager configs;
    private final PlayerService players;
    private final EconomyService economy;
    private final ListingService listings;
    private final BidService bids;
    private final Messenger messenger;

    public NotificationService(ConfigManager configs, PlayerService players, EconomyService economy,
                               ListingService listings, BidService bids, Messenger messenger) {
        this.configs = configs;
        this.players = players;
        this.economy = economy;
        this.listings = listings;
        this.bids = bids;
        this.messenger = messenger;
    }

    public void scheduleNotifications(Player player) {
        int delay = configs.main().notifications().joinDelay();
        if (delay < 0) {
            Scheduler.runEntity(player, () -> send(player));
        } else {
            Scheduler.runEntityLater(player, () -> send(player), delay);
        }
    }

    private void send(Player player) {
        if (!configs.main().notifications().enabled()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Optional<PlayerData> data = players.find(uuid);
        Map<String, Double> earnings = data.map(PlayerData::pendingEarnings).orElse(Map.of());
        Map<String, Double> refunds = data.map(PlayerData::pendingRefunds).orElse(Map.of());
        List<Listing.Bid> due = dueForReminder(uuid);

        if (earnings.isEmpty() && refunds.isEmpty() && due.isEmpty()) {
            return;
        }

        List<String> lines = new ArrayList<>(configs.lang().get("notifications.header"));
        Map<String, String> placeholders = new HashMap<>();

        if (!earnings.isEmpty()) {
            appendPending(earnings, "notifications.earnings", "earned", "earnings", lines, placeholders);
        }
        if (!refunds.isEmpty()) {
            appendPending(refunds, "notifications.refunds", "refund", "refunds", lines, placeholders);
        }
        if (!due.isEmpty()) {
            appendWonBids(due, lines, placeholders);
            due.forEach(bid -> listings.markBidReminderShown(bid.info().id()));
        }

        messenger.send(player, lines, placeholders);
        if (!earnings.isEmpty()) {
            deliverPendingEarnings(uuid, earnings);
        }
        if (!refunds.isEmpty()) {
            deliverPendingRefunds(uuid, refunds);
        }
    }

    // Won bids

    private List<Listing.Bid> dueForReminder(UUID uuid) {
        List<Listing.Bid> wonBids = listings.findAll(ListingQuery.owned(uuid, ListingScope.STORAGE)).stream()
                .map(listing -> (Listing.Bid) listing) // STORAGE is bid-only
                .toList();

        int repeatTimes = configs.main().notifications().repeatTimes();
        if (repeatTimes < 0) {
            return wonBids;
        }
        return wonBids.stream().filter(bid -> bid.remindersShown() < repeatTimes).toList();
    }

    private void appendWonBids(List<Listing.Bid> due, List<String> lines, Map<String, String> placeholders) {
        if (due.size() == 1) {
            Listing.Bid bid = due.getFirst();
            lines.addAll(configs.lang().get("notifications.won-bids.single"));
            placeholders.putAll(wonBidPlaceholders(bid));
            placeholders.put("purges", bids.purgesPlaceholder(bid.info().endedAt()));
        } else {
            lines.addAll(configs.lang().get("notifications.won-bids.multiple"));
            placeholders.put("total", String.valueOf(due.size()));
            placeholders.put("bids", joinedWonBids(due));
            placeholders.put("purges", bids.purgesPlaceholder(soonestPurge(due)));
        }
    }

    // shared by both the single-bid message above and each %bids% entry below
    private Map<String, String> wonBidPlaceholders(Listing.Bid bid) {
        Listing.Info info = bid.info();
        return PlaceholderMap.create()
                .put("amount", info.amount())
                .put("item", ItemDisplay.name(info.item(), configs.lang()))
                .put("seller", players.name(info.seller()))
                .put("total_bidders", bid.totalBidders())
                .money(economy, "price", info.economy(), bid.startingPrice())
                .money(economy, "offer", info.economy(), bid.currentPrice())
                .build();
    }

    private String joinedWonBids(List<Listing.Bid> due) {
        String template = configs.lang().get("notifications.won-bids.entry").getFirst();
        String separator = configs.lang().get("notifications.won-bids.separator").getFirst();

        return due.stream()
                .map(bid -> Placeholders.apply(null, template, wonBidPlaceholders(bid)))
                .collect(Collectors.joining(separator));
    }

    // earliest endedAt purges soonest
    private Instant soonestPurge(List<Listing.Bid> due) {
        return due.stream().map(bid -> bid.info().endedAt()).min(Instant::compareTo).orElseThrow();
    }

    // Pending payouts (earnings / refunds)

    private void appendPending(Map<String, Double> pending, String langPrefix, String singlePlaceholder,
                               String multiplePlaceholder, List<String> lines, Map<String, String> placeholders) {
        if (pending.size() == 1) {
            Map.Entry<String, Double> only = pending.entrySet().iterator().next();
            lines.addAll(configs.lang().get(langPrefix + ".single"));
            economy.formatInto(placeholders, singlePlaceholder, only.getKey(), only.getValue());
        } else {
            lines.addAll(configs.lang().get(langPrefix + ".multiple"));
            placeholders.put(multiplePlaceholder, joinedPending(pending, langPrefix));
        }
    }

    private String joinedPending(Map<String, Double> pending, String langPrefix) {
        String template = configs.lang().get(langPrefix + ".entry").getFirst();
        String separator = configs.lang().get(langPrefix + ".separator").getFirst();

        return pending.entrySet().stream()
                .map(entry -> Placeholders.apply(null, template, PlaceholderMap.create()
                        .money(economy, "amount", entry.getKey(), entry.getValue())
                        .put("economy", economy.displayName(entry.getKey()))
                        .build()))
                .collect(Collectors.joining(separator));
    }

    private void deliverPendingEarnings(UUID uuid, Map<String, Double> earnings) {
        Scheduler.runAsync(() -> {
            players.clearPendingEarnings(uuid);
            earnings.forEach((economyId, amount) -> players.payout(uuid, economyId, amount));
        });
    }

    private void deliverPendingRefunds(UUID uuid, Map<String, Double> refunds) {
        Scheduler.runAsync(() -> {
            players.clearPendingRefunds(uuid);
            refunds.forEach((economyId, amount) -> players.refund(uuid, economyId, amount));
        });
    }
}