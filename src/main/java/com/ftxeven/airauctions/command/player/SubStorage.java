package com.ftxeven.airauctions.command.player;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.core.command.tabcomplete.TabCompleteEngine;
import com.ftxeven.airauctions.core.gui.GuiManager;
import com.ftxeven.airauctions.gui.impl.StorageGui;
import com.ftxeven.airauctions.service.ServiceManager;
import com.ftxeven.airauctions.util.Messenger;

public final class SubStorage extends ViewSubcommand {

    public SubStorage(ConfigManager configs, Messenger messenger, ServiceManager services, GuiManager guis, TabCompleteEngine tabComplete) {
        super(configs, messenger, services, guis, tabComplete, "storage", StorageGui.SELF_ID, StorageGui.TARGET_ID);
    }
}