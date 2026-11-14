package com.brawlcore.client;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

/**
 * Adds the extra "Quick Multiplayer World" button to the main menu. The spot is picked so it never
 * overlaps vanilla buttons or buttons added by other mods (Essential adds its own widgets there).
 */
public final class QuickHostButton {
    private static final int HEIGHT = 20;

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) return;

        List<GuiEventListener> existing = event.getListenersList();
        int cx = screen.width / 2;
        int top = screen.height / 4 + 48; // y of the vanilla "Singleplayer" button

        // {x, y, width}: preferred spot above Singleplayer, then beside the button column, then a corner.
        int[][] candidates = {
                {cx - 100, top - 24, 200},
                {cx + 104, top, 120},
                {cx - 224, top, 120},
                {4, 4, 130}
        };

        int[] chosen = candidates[candidates.length - 1];
        for (int[] c : candidates) {
            boolean onScreen = c[0] >= 0 && c[0] + c[2] <= screen.width && c[1] >= 0 && c[1] + HEIGHT <= screen.height;
            boolean belowLogo = c[1] >= 80 || c[1] < 30; // logo is drawn roughly between y=30 and y=80
            if (onScreen && belowLogo && !overlapsAny(existing, c[0], c[1], c[2], HEIGHT)) {
                chosen = c;
                break;
            }
        }

        Component label = Component.translatable(EssentialCompat.isLoaded()
                ? "menu.brawlcore.quick_host.essential"
                : "menu.brawlcore.quick_host");

        Button button = Button.builder(label, b -> QuickHost.createAndHost(screen))
                .bounds(chosen[0], chosen[1], chosen[2], HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("menu.brawlcore.quick_host.tooltip")))
                .build();
        event.addListener(button);
    }

    private static boolean overlapsAny(List<GuiEventListener> widgets, int x, int y, int w, int h) {
        for (GuiEventListener l : widgets) {
            if (l instanceof AbstractWidget a
                    && a.getX() < x + w && a.getX() + a.getWidth() > x
                    && a.getY() < y + h && a.getY() + a.getHeight() > y) {
                return true;
            }
        }
        return false;
    }

    private QuickHostButton() {}
}
