package com.brawlcore.client;

import net.neoforged.fml.ModList;

/**
 * Soft detection of the Essential mod. Brawl Core never depends on Essential classes or mixins,
 * so the two mods cannot conflict; this only changes labels/messages.
 */
public final class EssentialCompat {
    private static Boolean cached;

    public static boolean isLoaded() {
        if (cached == null) {
            boolean found = false;
            ModList mods = ModList.get();
            if (mods != null) {
                found = mods.isLoaded("essential") || mods.isLoaded("essential-loader");
            }
            if (!found) {
                try {
                    Class.forName("gg.essential.api.EssentialAPI", false, EssentialCompat.class.getClassLoader());
                    found = true;
                } catch (Throwable ignored) {
                    // not present
                }
            }
            cached = found;
        }
        return cached;
    }

    private EssentialCompat() {}
}
