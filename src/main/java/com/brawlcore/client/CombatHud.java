package com.brawlcore.client;

import com.brawlcore.BrawlCore;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Small bars under the crosshair: dash cooldown, guard meter, M1 combo pips. */
public final class CombatHud {

    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(BrawlCore.MODID, "combat_hud"), CombatHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !ClientConfig.SHOW_HUD.get()) return;

        int width = 60;
        int x = g.guiWidth() / 2 - width / 2;
        int y = g.guiHeight() / 2 + 16;

        // dash cooldown (fills up as it recharges)
        if (ClientCombatState.dashCd > 0) {
            float f = 1f - (float) ClientCombatState.dashCd / ClientCombatState.dashMax;
            bar(g, x, y, width, f, 0xFF5CC8FF);
            y += 5;
        }

        // guard meter
        if (ClientCombatState.blocking || ClientCombatState.stunned || ClientCombatState.guard < ClientCombatState.guardMax) {
            float f = Math.max(0f, Math.min(1f, (float) ClientCombatState.guard / ClientCombatState.guardMax));
            int color = ClientCombatState.stunned ? 0xFFFF3B3B
                    : ClientCombatState.perfectWindow ? 0xFFFFD84A
                    : 0xFF6BE675;
            bar(g, x, y, width, f, color);
            y += 5;
        }

        // M1 combo pips (4th pip is the finisher)
        if (ClientCombatState.combo > 0) {
            for (int i = 0; i < 4; i++) {
                int px = x + i * 16;
                boolean lit = i < ClientCombatState.combo;
                int color = lit ? (i == 3 ? 0xFFFFD84A : 0xFFFFFFFF) : 0x55FFFFFF;
                g.fill(px, y, px + 12, y + 3, 0x90000000);
                g.fill(px + 1, y, px + 11, y + 2, color);
            }
        }
    }

    private static void bar(GuiGraphics g, int x, int y, int width, float fraction, int color) {
        g.fill(x - 1, y - 1, x + width + 1, y + 4, 0x90000000);
        g.fill(x, y, x + (int) (width * fraction), y + 3, color);
    }

    private CombatHud() {}
}
