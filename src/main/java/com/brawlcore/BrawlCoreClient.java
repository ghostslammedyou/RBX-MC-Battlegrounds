package com.brawlcore;

import com.brawlcore.client.ClientConfig;
import com.brawlcore.client.ClientInput;
import com.brawlcore.client.CombatHud;
import com.brawlcore.client.QuickHost;
import com.brawlcore.client.QuickHostButton;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only entry point: key bindings, input handling, HUD and the main menu button. */
@Mod(value = BrawlCore.MODID, dist = Dist.CLIENT)
public final class BrawlCoreClient {

    public BrawlCoreClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        modBus.addListener(ClientInput::registerKeys);
        modBus.addListener(CombatHud::registerLayer);

        NeoForge.EVENT_BUS.addListener(ClientInput::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientInput::onInteraction);
        NeoForge.EVENT_BUS.addListener(ClientInput::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(QuickHostButton::onScreenInit);
        // The integrated server runs inside the client JVM, so its events reach these listeners.
        NeoForge.EVENT_BUS.addListener(QuickHost::onServerStarted);
        NeoForge.EVENT_BUS.addListener(QuickHost::onPlayerLoggedIn);
    }
}
