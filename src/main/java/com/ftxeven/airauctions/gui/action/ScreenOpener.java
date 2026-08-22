package com.ftxeven.airauctions.gui.action;

import com.ftxeven.airauctions.common.gui.GuiSession;
import com.ftxeven.airauctions.common.gui.OpenOptions;
import com.ftxeven.airauctions.common.gui.action.ActionContext;
import com.ftxeven.airauctions.common.gui.flag.FlagGate;
import com.ftxeven.airauctions.common.gui.nav.GuiContext;
import com.ftxeven.airauctions.common.gui.nav.ScreenKey;
import com.ftxeven.airauctions.common.gui.nav.ScreenState;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ScreenOpener {

    private ScreenOpener() {
    }

    public static void open(ActionContext context, String guiId, Map<String, Object> attributes) {
        GuiSession current = context.session();
        ScreenKey currentScreen = current.screenKey();
        ScreenKey forwardScreen = new ScreenKey(guiId, currentScreen.target());
        GuiContext backLink = new GuiContext(currentScreen, current.originChain(), current.navBack());

        OpenOptions options = OpenOptions.forScreen(FlagGate.NO_FLAGS, forwardScreen,
                ScreenState.DEFAULT, backLink, current.forwardChain(), attributes);
        context.manager().open(context.viewer(), guiId, new LinkedHashMap<>(current.placeholders()), options);
    }
}