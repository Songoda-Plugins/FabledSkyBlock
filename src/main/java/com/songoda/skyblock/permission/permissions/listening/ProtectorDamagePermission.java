package com.songoda.skyblock.permission.permissions.listening;

import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.permission.ListeningPermission;
import com.songoda.skyblock.permission.PermissionHandler;
import com.songoda.skyblock.permission.PermissionType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ProtectorDamagePermission extends ListeningPermission {
    public ProtectorDamagePermission(SkyBlock plugin) {
        super("ProtectorDamage", XMaterial.IRON_SWORD, PermissionType.ISLAND);
    }

    @PermissionHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof IronGolem ||
                event.getDamager() instanceof Snowman ||
                (event.getDamager() instanceof Wolf && ((Wolf) event.getDamager()).isTamed())) {
            event.setCancelled(true);
        }
    }
}
