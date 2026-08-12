package com.ftxeven.airauctions.gui.action;

import com.ftxeven.airauctions.core.gui.GuiSession;
import com.ftxeven.airauctions.core.gui.OpenOptions;
import com.ftxeven.airauctions.core.gui.action.ActionContext;
import com.ftxeven.airauctions.core.gui.flag.FlagGate;
import com.ftxeven.airauctions.core.gui.nav.GuiContext;
import com.ftxeven.airauctions.core.gui.nav.ScreenKey;
import com.ftxeven.airauctions.core.gui.nav.ScreenState;

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