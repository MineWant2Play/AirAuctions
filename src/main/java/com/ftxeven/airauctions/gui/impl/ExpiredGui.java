package com.ftxeven.airauctions.gui.impl;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.gui.ListingGuiManager;
import com.ftxeven.airauctions.model.ListingScope;
import com.ftxeven.airauctions.service.ServiceManager;
import com.ftxeven.airauctions.service.listing.workflow.ReclaimService;

public final class ExpiredGui extends ListingGridGui {

    public static final String SELF_ID   = "player/expired";
    public static final String TARGET_ID = "target/expired";

    public ExpiredGui(ServiceManager services, ConfigManager configs, ListingGuiManager guis) {
        super(services, configs, guis);
    }

    @Override protected ListingScope scope() { return ListingScope.EXPIRED; }

    @Override protected ReclaimService.ReclaimKind reclaimKind() { return ReclaimService.ReclaimKind.CLAIM; }
}