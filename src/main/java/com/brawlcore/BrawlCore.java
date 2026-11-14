package com.brawlcore;

import com.brawlcore.combat.CombatConfig;
import com.brawlcore.combat.CombatEvents;
import com.brawlcore.net.Payloads;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Common (client + server) entry point. All gameplay is server-authoritative;
 * the client only sends input and renders the synced state.
 */
@Mod(BrawlCore.MODID)
public final class BrawlCore {
    public static final String MODID = "brawlcore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BrawlCore(IEventBus modBus, ModContainer container) {
        // SERVER config = per-world balance values, synced to joining players.
        container.registerConfig(ModConfig.Type.SERVER, CombatConfig.SPEC);

        modBus.addListener(Payloads::register);
        CombatEvents.register();
    }
}
