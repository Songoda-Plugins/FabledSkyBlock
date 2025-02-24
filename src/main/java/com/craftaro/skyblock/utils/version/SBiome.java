package com.craftaro.skyblock.utils.version;

import com.craftaro.core.compatibility.MajorServerVersion;
import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.third_party.com.cryptomorin.xseries.XBiome;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.skyblock.utils.StringUtil;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * A Biome wrapper for supporting Biomes in 1.8-1.13+
 */

//TODO: Use CompatibleBiome
public enum SBiome {
    BADLANDS(true, XMaterial.DEAD_BUSH),
    COLD_OCEAN(true, XMaterial.ICE),
    DARK_FOREST("ROOFED_FOREST", XMaterial.DARK_OAK_SAPLING),
    DESERT(XMaterial.SAND),
    FOREST(XMaterial.FERN),
    JUNGLE(XMaterial.VINE),
    MOUNTAINS("EXTREME_HILLS", XMaterial.EMERALD_ORE),
    MUSHROOM_FIELDS("MUSHROOM_ISLAND", XMaterial.RED_MUSHROOM),
    NETHER("HELL", XMaterial.NETHERRACK),
    PLAINS(XMaterial.SUNFLOWER),
    RIVER(XMaterial.COD),
    SAVANNA(XMaterial.ACACIA_SAPLING),
    SNOWY_BEACH("COLD_BEACH", XMaterial.SNOWBALL),
    SWAMP("SWAMPLAND", XMaterial.SLIME_BALL),
    TAIGA(XMaterial.SPRUCE_SAPLING),
    THE_END(true, XMaterial.END_STONE),
    THE_VOID("SKY", XMaterial.OBSIDIAN),
    WARM_OCEAN(true, XMaterial.TROPICAL_FISH);

    private static final boolean isPostVersion = MajorServerVersion.isServerVersionAtLeast(MajorServerVersion.V1_13);

    private final String legacyName;
    private final boolean isPost13;
    private final XMaterial guiIcon;

    SBiome(XMaterial guiIcon) {
        this(null, false, guiIcon);
    }

    SBiome(String legacyName, XMaterial guiIcon) {
        this(legacyName, false, guiIcon);
    }

    SBiome(boolean isPost13, XMaterial guiIcon) {
        this(null, isPost13, guiIcon);
    }

    SBiome(String legacyName, boolean is13only, XMaterial guiIcon) {
        this.legacyName = legacyName;
        this.isPost13 = is13only;
        this.guiIcon = guiIcon;
    }

    /**
     * Gets an SBiome based on its Gui icon
     *
     * @return An SBiome with a matching Gui icon
     */
    @SuppressWarnings("deprecation")
    public static SBiome getFromGuiIcon(Material material, byte data) {
        return Arrays.stream(values()).filter(biome -> {
            return biome.isAvailable() &&
                    biome.getGuiIcon().getType() == material &&
                    (isPostVersion || biome.getGuiIcon().getData().getData() == data);
        }).findFirst().orElse(null);
    }

    /**
     * Checks if this Biome can be used in the current server version
     *
     * @return True if the current server version supports this Biome, otherwise false
     */
    public boolean isAvailable() {
        return !this.isPost13 || isPostVersion;
    }

    /**
     * Gets an SBiome as it's Bukkit Biome counterpart
     *
     * @return The Biome this SBiome represents, or null if it is not available in this server version
     */
    public XBiome getBiome() {
        return XBiome.of(this.name()).orElse(null);
    }

    /**
     * Gets the name of the Biome formatted for a Gui
     *
     * @return The formatted Biome name
     */
    public String getFormattedBiomeName() {
        return !this.isAvailable() ? null : StringUtil.capitalizeWord(this.getBiome().name().replaceAll("_", " "));
    }

    /**
     * Gets the Gui icon that represents this Biome
     *
     * @return The Gui icon that represents this Biome
     */
    public ItemStack getGuiIcon() {
        return !this.isAvailable() ? null : this.guiIcon.parseItem();
    }
}
