package com.ftxeven.airauctions.common.gui.nav;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record ScreenKey(String guiId, @Nullable UUID target) {}