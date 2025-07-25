package com.songoda.skyblock.utils.version;

import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class PotionUtils {

    // Utility method to prevent code duplication
    public static void setPotionData_V1_13(PotionMeta pm, PotionType type) {
        boolean extended = true;
        boolean upgraded = false;
        String typeName = type.name();
        if (typeName.startsWith("LONG_")) {
            type = PotionType.valueOf(typeName.substring(5));
            extended = true;
            upgraded = false;
        } else if (typeName.startsWith("STRONG_")) {
            type = PotionType.valueOf(typeName.substring(7));
            upgraded = true;
            extended = false;
        }
        if (!type.isExtendable()) {
            extended = false;
        }
        pm.setBasePotionData(new PotionData(type, extended, upgraded));
    }
}
