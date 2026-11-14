package com.brawlcore.client;

import com.brawlcore.combat.CombatConfig;
import com.brawlcore.combat.DashSystem;
import com.brawlcore.net.Payloads;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Turns key presses into packets. The server decides what actually happens
 * (cooldowns, damage, i-frames), so a modified client can't cheat.
 */
public final class ClientInput {
    public static KeyMapping DASH;
    public static KeyMapping BLOCK;

    private static final String CATEGORY = "key.categories.brawlcore";

    private static boolean blockSent;
    private static long tick;
    private static long lastM1Send = -100;

    // double-tap tracking: index 0 = forward, 1 = back, 2 = left, 3 = right
    private static final boolean[] prevDown = new boolean[4];
    private static final long[] lastPress = {-100, -100, -100, -100};

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        DASH = new KeyMapping("key.brawlcore.dash", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
        BLOCK = new KeyMapping("key.brawlcore.block", KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY);
        event.register(DASH);
        event.register(BLOCK);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        tick++;

        if (mc.player == null || mc.level == null) {
            ClientCombatState.reset();
            blockSent = false;
            return;
        }

        boolean inGame = mc.screen == null;

        // Dash key (+ direction keys held)
        while (DASH.consumeClick()) {
            if (inGame) sendDash(directionFromKeys(mc.options));
        }
        if (inGame && ClientConfig.DOUBLE_TAP_DASH.get()) {
            doubleTap(mc.options);
        }

        // Block (hold)
        boolean wantBlock = inGame && BLOCK.isDown();
        if (wantBlock != blockSent) {
            PacketDistributor.sendToServer(new Payloads.BlockPayload(wantBlock));
            blockSent = wantBlock;
        }

        // Hold left click to keep swinging M1
        if (inGame && ClientConfig.HOLD_TO_M1.get() && mc.options.keyAttack.isDown() && m1Applies(mc)) {
            sendM1();
        }
    }

    /** Replaces the vanilla attack with an M1 swing when the player isn't mining a block. */
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!m1Applies(mc)) return;
        event.setCanceled(true);
        event.setSwingHand(false); // the server plays the swing animation
        sendM1();
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientCombatState.reset();
        blockSent = false;
    }

    private static boolean m1Applies(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) return false;
        CombatConfig.M1Mode mode = CombatConfig.m1Mode();
        if (mode == CombatConfig.M1Mode.OFF) return false;
        if (mode == CombatConfig.M1Mode.EMPTY_HAND && !mc.player.getMainHandItem().isEmpty()) return false;
        HitResult hit = mc.hitResult;
        // looking at a block = mining, leave vanilla alone
        return hit == null || hit.getType() != HitResult.Type.BLOCK;
    }

    private static void sendM1() {
        if (ClientCombatState.blocking || ClientCombatState.stunned) return;
        if (tick - lastM1Send < 2) return; // the server enforces the real cooldown
        lastM1Send = tick;
        PacketDistributor.sendToServer(new Payloads.M1Payload());
    }

    private static void sendDash(int dir) {
        if (ClientCombatState.stunned || ClientCombatState.dashCd > 0) return;
        PacketDistributor.sendToServer(new Payloads.DashPayload(dir));
    }

    private static int directionFromKeys(Options o) {
        boolean left = o.keyLeft.isDown();
        boolean right = o.keyRight.isDown();
        if (left && !right) return DashSystem.LEFT;
        if (right && !left) return DashSystem.RIGHT;
        if (o.keyDown.isDown()) return DashSystem.BACK;
        return DashSystem.FORWARD;
    }

    private static void doubleTap(Options o) {
        boolean[] down = {o.keyUp.isDown(), o.keyDown.isDown(), o.keyLeft.isDown(), o.keyRight.isDown()};
        int[] dirs = {DashSystem.FORWARD, DashSystem.BACK, DashSystem.LEFT, DashSystem.RIGHT};
        for (int i = 0; i < 4; i++) {
            if (down[i] && !prevDown[i]) {
                if (tick - lastPress[i] <= 7) {
                    sendDash(dirs[i]);
                    lastPress[i] = -100;
                } else {
                    lastPress[i] = tick;
                }
            }
            prevDown[i] = down[i];
        }
    }

    private ClientInput() {}
}
