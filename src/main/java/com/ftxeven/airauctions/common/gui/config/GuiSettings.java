package com.ftxeven.airauctions.common.gui.config;

import com.ftxeven.airauctions.common.gui.input.InputType;

import java.util.List;

public record GuiSettings(
        String title,
        boolean enabled,
        int rows,
        boolean trimLore,
        boolean forceReopen,
        int refreshInterval,
        InputType inputType,
        List<String> openActions,
        List<String> closeActions
) {
    public GuiSettings {
        openActions = List.copyOf(openActions);
        closeActions = List.copyOf(closeActions);
    }
}