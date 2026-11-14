package com.brawlcore.net;

import com.brawlcore.BrawlCore;
import com.brawlcore.client.ClientCombatState;
import com.brawlcore.combat.BlockSystem;
import com.brawlcore.combat.DashSystem;
import com.brawlcore.combat.M1System;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Payloads {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(DashPayload.TYPE, DashPayload.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) DashSystem.tryDash(sp, payload.dir());
        }));

        registrar.playToServer(M1Payload.TYPE, M1Payload.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) M1System.tryM1(sp);
        }));

        registrar.playToServer(BlockPayload.TYPE, BlockPayload.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) BlockSystem.setWantsBlock(sp, payload.held());
        }));

        registrar.playToClient(SyncPayload.TYPE, SyncPayload.CODEC, (payload, ctx) -> ctx.enqueueWork(() ->
                ClientCombatState.apply(payload)));
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(BrawlCore.MODID, path));
    }

    /** Client -> server: dash in a direction (DashSystem.FORWARD/BACK/LEFT/RIGHT). */
    public record DashPayload(int dir) implements CustomPacketPayload {
        public static final Type<DashPayload> TYPE = type("dash");
        public static final StreamCodec<ByteBuf, DashPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.VAR_INT, DashPayload::dir, DashPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: the player pressed attack and wants an M1 swing. */
    public record M1Payload() implements CustomPacketPayload {
        public static final Type<M1Payload> TYPE = type("m1");
        public static final StreamCodec<ByteBuf, M1Payload> CODEC = StreamCodec.unit(new M1Payload());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: block key held / released. */
    public record BlockPayload(boolean held) implements CustomPacketPayload {
        public static final Type<BlockPayload> TYPE = type("block");
        public static final StreamCodec<ByteBuf, BlockPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, BlockPayload::held, BlockPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server -> client: HUD state.
     * state = combo (low 4 bits) | flags << 4, flags: 1 = blocking, 2 = stunned, 4 = perfect-block window open.
     */
    public record SyncPayload(int dashCd, int dashMax, int guard, int guardMax, int state) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = type("sync");
        public static final StreamCodec<ByteBuf, SyncPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SyncPayload::dashCd,
                ByteBufCodecs.VAR_INT, SyncPayload::dashMax,
                ByteBufCodecs.VAR_INT, SyncPayload::guard,
                ByteBufCodecs.VAR_INT, SyncPayload::guardMax,
                ByteBufCodecs.VAR_INT, SyncPayload::state,
                SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private Payloads() {}
}
