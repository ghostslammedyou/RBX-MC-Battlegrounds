package com.brawlcore.combat;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Game-bus listeners shared by client and server (they only do anything on the logical server). */
public final class CombatEvents {

    public static void register() {
        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(EventPriority.HIGH, BlockSystem::onIncomingDamage);
        bus.addListener(CombatEvents::onPlayerTick);
        bus.addListener(CombatEvents::onLogout);
        bus.addListener(CombatEvents::onServerStopped);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CombatManager.tick(sp);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            CombatManager.remove(sp);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        CombatManager.clear();
    }

    private CombatEvents() {}
}
