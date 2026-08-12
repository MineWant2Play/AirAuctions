package com.ftxeven.airauctions.gui.impl;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.gui.ListingGuiManager;
import com.ftxeven.airauctions.service.ServiceManager;

public final class ViewShulkerGui extends ListingPreviewGui {

    public static final String ID = "browsing/view_shulker";

    public ViewShulkerGui(ServiceManager services, ConfigManager configs, ListingGuiManager guis) {
        super(services, configs, guis);
    }
}