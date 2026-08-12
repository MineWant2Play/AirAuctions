package com.ftxeven.airauctions.command.player;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.core.command.tabcomplete.TabCompleteEngine;
import com.ftxeven.airauctions.core.gui.GuiManager;
import com.ftxeven.airauctions.gui.impl.HistoryGui;
import com.ftxeven.airauctions.service.ServiceManager;
import com.ftxeven.airauctions.util.Messenger;

public final class SubHistory extends ViewSubcommand {

    public SubHistory(ConfigManager configs, Messenger messenger, ServiceManager services, GuiManager guis, TabCompleteEngine tabComplete) {
        super(configs, messenger, services, guis, tabComplete, "history", HistoryGui.SELF_ID, HistoryGui.TARGET_ID);
    }
}