package com.ftxeven.airauctions.gui.action;

import com.ftxeven.airauctions.config.ConfigManager;
import com.ftxeven.airauctions.gui.impl.ViewShulkerGui;
import com.ftxeven.airauctions.model.HistoryEntry;
import com.ftxeven.airauctions.model.Listing;
import com.ftxeven.airauctions.service.ServiceManager;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;

public final class ViewShulkerAction extends PreviewAction {

    public ViewShulkerAction(ServiceManager services, ConfigManager configs) {
        super(services, configs);
    }

    @Override
    protected String guiId() {
        return ViewShulkerGui.ID;
    }

    @Override
    protected boolean applies(Listing listing) {
        return isShulker(listing.info().item());
    }

    @Override
    protected boolean applies(HistoryEntry entry) {
        return isShulker(entry.info().item());
    }

    private static boolean isShulker(ItemStack item) {
        return Tag.SHULKER_BOXES.isTagged(item.getType());
    }
}