package com.ftxeven.airauctions.permission;

import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;

import java.util.Set;

public final class NullPermissible implements Permissible {

    public static final NullPermissible INSTANCE = new NullPermissible();

    private NullPermissible() {
    }

    @Override public boolean isPermissionSet(String name) { return false; }
    @Override public boolean isPermissionSet(Permission perm) { return false; }
    @Override public boolean hasPermission(String name) { return false; }
    @Override public boolean hasPermission(Permission perm) { return false; }

    @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
        throw new UnsupportedOperationException("NullPermissible cannot hold attachments");
    }
    @Override public PermissionAttachment addAttachment(Plugin plugin) {
        throw new UnsupportedOperationException("NullPermissible cannot hold attachments");
    }
    @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
        throw new UnsupportedOperationException("NullPermissible cannot hold attachments");
    }
    @Override public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
        throw new UnsupportedOperationException("NullPermissible cannot hold attachments");
    }
    @Override public void removeAttachment(PermissionAttachment attachment) { }
    @Override public void recalculatePermissions() { }
    @Override public Set<PermissionAttachmentInfo> getEffectivePermissions() { return Set.of(); }
    @Override public boolean isOp() { return false; }
    @Override public void setOp(boolean value) {
        throw new UnsupportedOperationException("NullPermissible cannot be opped");
    }
}