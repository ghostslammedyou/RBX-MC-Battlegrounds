package com.brawlcore.combat;

import com.brawlcore.net.Payloads;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CombatManager {
    private static final Map<UUID, CombatState> STATES = new HashMap<>();

    public static CombatState get(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), id -> new CombatState());
    }

    /** State for any entity, or null if it is not a tracked player. */
    public static CombatState peek(LivingEntity e) {
        return e instanceof ServerPlayer sp ? STATES.get(sp.getUUID()) : null;
    }

    public static void remove(ServerPlayer p) {
        STATES.remove(p.getUUID());
    }

    public static void clear() {
        STATES.clear();
    }

    /** Called once per server tick for every player. */
    static void tick(ServerPlayer sp) {
        CombatState s = get(sp);
        long now = sp.level().getGameTime();
        int guardMax = CombatConfig.GUARD_MAX.get();
        if (s.guard < 0f) s.guard = guardMax;

        // End of dash: cut horizontal momentum so the dash stops cleanly instead of sliding.
        if (s.dashEndTick != 0 && now >= s.dashEndTick) {
            s.dashEndTick = 0;
            Vec3 m = sp.getDeltaMovement();
            sp.setDeltaMovement(m.x * 0.2, m.y, m.z * 0.2);
            sp.hurtMarked = true;
        }

        // Stun ends -> guard comes back after a guard break.
        if (s.stunnedUntil != 0 && now >= s.stunnedUntil) {
            s.stunnedUntil = 0;
            if (s.guardBroken) {
                s.guardBroken = false;
                s.guard = guardMax;
            }
        }

        BlockSystem.update(sp, s, now);

        // Guard regeneration
        if (!s.blocking && !s.isStunned(now) && now >= s.guardRegenTick && s.guard < guardMax) {
            s.guard = Math.min(guardMax, s.guard + CombatConfig.GUARD_REGEN_PER_TICK.get().floatValue());
        }

        if (s.blocking && CombatConfig.BLOCK_SLOWNESS.get()) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4, 0, false, false, false));
        }

        sync(sp, s, now);
    }

    private static void sync(ServerPlayer sp, CombatState s, long now) {
        int cd = (int) Math.max(0, s.dashReadyTick - now);
        int combo = now > s.comboExpireTick ? 0 : s.combo;
        int flags = 0;
        if (s.blocking) flags |= 1;
        if (s.isStunned(now)) flags |= 2;
        if (s.blocking && !s.perfectUsed && now - s.blockStartTick <= CombatConfig.PERFECT_WINDOW.get()) flags |= 4;
        int state = (combo & 15) | (flags << 4);
        int guard = Math.round(s.guard);

        if (cd == s.lastCd && guard == s.lastGuard && state == s.lastState) return;
        s.lastCd = cd;
        s.lastGuard = guard;
        s.lastState = state;
        PacketDistributor.sendToPlayer(sp, new Payloads.SyncPayload(
                cd, CombatConfig.DASH_COOLDOWN.get(), guard, CombatConfig.GUARD_MAX.get(), state));
    }

    private CombatManager() {}
}
