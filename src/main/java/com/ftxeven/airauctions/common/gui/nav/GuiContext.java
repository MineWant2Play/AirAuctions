package com.ftxeven.airauctions.common.gui.nav;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record GuiContext(ScreenKey screen, List<String> originChain, @Nullable GuiContext previous) {}