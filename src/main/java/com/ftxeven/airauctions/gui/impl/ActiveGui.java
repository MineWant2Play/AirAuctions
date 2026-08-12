package com.ftxeven.airauctions.gui.impl;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.gui.ListingGuiManager;
import com.ftxeven.airauctions.model.ListingScope;
import com.ftxeven.airauctions.service.ServiceManager;

public final class ActiveGui extends ListingGridGui {

    public static final String SELF_ID = "player/active";
    public static final String TARGET_ID = "target/active";

    public ActiveGui(ServiceManager services, ConfigManager configs, ListingGuiManager guis) {
        super(services, configs, guis);
    }

    @Override
    protected ListingScope scope() {
        return ListingScope.ACTIVE;
    }
}