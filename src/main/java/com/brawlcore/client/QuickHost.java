package com.brawlcore.client;

import com.brawlcore.BrawlCore;
import net.minecraft.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.IOException;

/**
 * Creates a brand-new survival world straight from the title screen (skipping the Create World
 * screen) and, once the integrated server is up, opens it to other players.
 */
public final class QuickHost {
    private static volatile boolean pending;
    private static volatile int announcePort = -1;

    public static void createAndHost(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        String name = uniqueName(mc);

        LevelSettings settings = new LevelSettings(
                name, GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        WorldOptions options = WorldOptions.defaultWithRandomSeed();

        pending = ClientConfig.QUICK_HOST_OPEN_LAN.get();
        announcePort = -1;
        BrawlCore.LOGGER.info("Quick Host: creating world '{}' (open to LAN: {})", name, pending);

        mc.createWorldOpenFlows().createFreshLevel(
                name, settings, options, registries -> WorldPresets.createNormalWorldDimensions(registries), parent);
    }

    private static String uniqueName(Minecraft mc) {
        String base = ClientConfig.QUICK_WORLD_NAME.get().trim();
        if (base.isEmpty()) base = "Essential World";
        try {
            return FileUtil.findAvailableName(mc.getLevelSource().getBaseDir(), base, "");
        } catch (IOException | RuntimeException e) {
            return base + " " + (System.currentTimeMillis() / 1000L);
        }
    }

    /** Integrated server finished loading -> publish it so other players can join. */
    public static void onServerStarted(ServerStartedEvent event) {
        if (!pending) return;
        pending = false;
        if (!(event.getServer() instanceof IntegratedServer server)) return;

        int port = HttpUtil.getAvailablePort();
        boolean ok = server.publishServer(server.getDefaultGameType(), false, port);
        announcePort = ok ? port : 0;
    }

    /** Tell the host (once) that the world is open. */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (announcePort < 0 || !(event.getEntity() instanceof ServerPlayer player)) return;
        int port = announcePort;
        announcePort = -1;
        if (port == 0) {
            player.sendSystemMessage(Component.translatable("message.brawlcore.host_failed"));
        } else {
            String key = EssentialCompat.isLoaded() ? "message.brawlcore.hosted.essential" : "message.brawlcore.hosted";
            player.sendSystemMessage(Component.translatable(key, port));
        }
    }

    private QuickHost() {}
}
