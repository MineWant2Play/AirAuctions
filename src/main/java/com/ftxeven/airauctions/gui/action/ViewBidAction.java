package com.ftxeven.airauctions.gui.action;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.gui.impl.ViewBidGui;
import com.ftxeven.airauctions.model.HistoryEntry;
import com.ftxeven.airauctions.model.Listing;
import com.ftxeven.airauctions.model.ListingType;
import com.ftxeven.airauctions.service.ServiceManager;

public final class ViewBidAction extends PreviewAction {

    public ViewBidAction(ServiceManager services, ConfigManager configs) {
        super(services, configs);
    }

    @Override
    protected String guiId() {
        return ViewBidGui.ID;
    }

    @Override
    protected boolean applies(Listing listing) {
        return listing instanceof Listing.Bid;
    }

    @Override
    protected boolean applies(HistoryEntry entry) {
        return entry.type() == ListingType.BID;
    }
}