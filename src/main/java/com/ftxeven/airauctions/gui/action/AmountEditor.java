package com.ftxeven.airauctions.gui.action;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.economy.EconomyProvider;
import com.ftxeven.airauctions.model.Listing;
import com.ftxeven.airauctions.service.ActionResult;
import com.ftxeven.airauctions.service.economy.EconomyService;
import com.ftxeven.airauctions.service.listing.workflow.BidService;
import com.ftxeven.airauctions.util.Messenger;
import org.bukkit.entity.Player;

import java.util.OptionalDouble;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public interface AmountEditor {

    String inputContext();

    double current();

    void set(double value);

    void applyTyped(Player viewer, String raw);

    default boolean valid() {
        return true;
    }

    static AmountEditor price(String inputContext, DoubleSupplier current, DoubleConsumer apply,
                              Supplier<EconomyProvider> provider, EconomyService economy,
                              ConfigManager configs, Messenger messenger) {
        return unclamped(inputContext, current, apply, provider, () -> true, economy, configs, messenger);
    }

    static AmountEditor offer(DoubleSupplier current, DoubleConsumer apply, Supplier<EconomyProvider> provider,
                              BooleanSupplier valid, EconomyService economy, ConfigManager configs, Messenger messenger) {
        return unclamped("bid", current, apply, provider, valid, economy, configs, messenger);
    }

    private static AmountEditor unclamped(String inputContext, DoubleSupplier current, DoubleConsumer apply,
                                          Supplier<EconomyProvider> provider, BooleanSupplier valid,
                                          EconomyService economy, ConfigManager configs, Messenger messenger) {
        return new AmountEditor() {
            @Override
            public String inputContext() {
                return inputContext;
            }

            @Override
            public double current() {
                return current.getAsDouble();
            }

            @Override
            public void set(double value) {
                apply.accept(value);
            }

            @Override
            public void applyTyped(Player viewer, String raw) {
                OptionalDouble parsed = economy.parsePrice(raw, provider.get());
                if (parsed.isEmpty()) {
                    messenger.send(viewer, configs.lang().get("errors.economy.invalid-price"));
                    return;
                }
                set(parsed.getAsDouble());
            }

            @Override
            public boolean valid() {
                return valid.getAsBoolean();
            }
        };
    }

    static AmountEditor quantity(IntSupplier current, IntConsumer apply, IntSupplier max, BooleanSupplier valid,
                                 ConfigManager configs, Messenger messenger) {
        return new AmountEditor() {
            @Override
            public String inputContext() {
                return "quantity";
            }

            @Override
            public double current() {
                return current.getAsInt();
            }

            @Override
            public void set(double value) {
                apply.accept(Math.clamp(Math.round(value), 1, max.getAsInt()));
            }

            @Override
            public void applyTyped(Player viewer, String raw) {
                try {
                    set(Integer.parseInt(raw.trim()));
                } catch (NumberFormatException e) {
                    messenger.send(viewer, configs.lang().get("errors.economy.invalid-amount"));
                }
            }

            @Override
            public boolean valid() {
                return valid.getAsBoolean();
            }
        };
    }

    static AmountEditor bid(Supplier<Listing.Bid> bid, Supplier<EconomyProvider> provider,
                            BidService bids, EconomyService economy, ConfigManager configs, Messenger messenger) {
        return new AmountEditor() {
            @Override
            public String inputContext() {
                return "bid";
            }

            @Override
            public double current() {
                Listing.Bid target = bid.get();
                return target != null ? bids.nextOfferBounds(target).min() : 0;
            }

            @Override
            public void set(double value) {
                // offers are only ever placed through typed input here
            }

            @Override
            public void applyTyped(Player viewer, String raw) {
                Listing.Bid target = bid.get();
                EconomyProvider resolvedProvider = provider.get();
                if (target == null || resolvedProvider == null) {
                    return;
                }

                OptionalDouble parsed = economy.parsePrice(raw, resolvedProvider);
                if (parsed.isEmpty()) {
                    messenger.send(viewer, configs.lang().get("errors.economy.invalid-price"));
                    return;
                }

                ActionResult<BidService.PlaceBidSuccess> result = bids.placeBid(viewer, target, parsed.getAsDouble());
                if (result instanceof ActionResult.Denied<BidService.PlaceBidSuccess> denied) {
                    messenger.send(viewer, configs.lang().get(denied.langKey()), denied.placeholders());
                }
            }

            @Override
            public boolean valid() {
                return bid.get() != null;
            }
        };
    }
}