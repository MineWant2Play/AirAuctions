package com.ftxeven.airauctions.command.player;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.core.command.tabcomplete.TabCompleteEngine;
import com.ftxeven.airauctions.core.gui.GuiManager;
import com.ftxeven.airauctions.gui.impl.ActiveGui;
import com.ftxeven.airauctions.service.ServiceManager;
import com.ftxeven.airauctions.util.Messenger;

public final class SubListings extends ViewSubcommand {

    public SubListings(ConfigManager configs, Messenger messenger, ServiceManager services, GuiManager guis, TabCompleteEngine tabComplete) {
        super(configs, messenger, services, guis, tabComplete, "listings", ActiveGui.SELF_ID, ActiveGui.TARGET_ID);
    }
}