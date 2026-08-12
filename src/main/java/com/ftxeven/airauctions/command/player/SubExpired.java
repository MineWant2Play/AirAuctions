package com.ftxeven.airauctions.command.player;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.core.command.tabcomplete.TabCompleteEngine;
import com.ftxeven.airauctions.core.gui.GuiManager;
import com.ftxeven.airauctions.gui.impl.ExpiredGui;
import com.ftxeven.airauctions.service.ServiceManager;
import com.ftxeven.airauctions.util.Messenger;

public final class SubExpired extends ViewSubcommand {

    public SubExpired(ConfigManager configs, Messenger messenger, ServiceManager services, GuiManager guis, TabCompleteEngine tabComplete) {
        super(configs, messenger, services, guis, tabComplete, "expired", ExpiredGui.SELF_ID, ExpiredGui.TARGET_ID);
    }
}