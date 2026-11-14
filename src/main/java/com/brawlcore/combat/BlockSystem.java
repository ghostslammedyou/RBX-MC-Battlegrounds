package com.brawlcore.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Block system (hold the block key):
 *  - blocked hits lose most of their damage but drain the guard meter
 *  - a hit in the first few ticks after raising block is a PERFECT BLOCK: no damage, no guard loss,
 *    the attacker is knocked back and staggered
 *  - guard meter at zero = guard break: full damage and a stun
 *  - after releasing, block has a short cooldown so it can't be spammed
 */
public final class BlockSystem {

    public static void setWantsBlock(ServerPlayer p, boolean want) {
        CombatState s = CombatManager.get(p);
        s.wantsBlock = want;
        update(p, s, p.level().getGameTime());
    }

    /** Start or stop blocking according to what the player wants and what is currently allowed. */
    static void update(ServerPlayer p, CombatState s, long now) {
        boolean canBlock = !p.isSpectator() && !p.isDeadOrDying() && !s.isStunned(now);
        if (s.blocking) {
            if (!s.wantsBlock || !canBlock) release(s, now);
        } else if (s.wantsBlock && canBlock && now >= s.blockReadyTick) {
            s.blocking = true;
            s.perfectUsed = false;
            s.blockStartTick = now;
            s.combo = 0;
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                    SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
    }

    private static void release(CombatState s, long now) {
        s.blocking = false;
        s.blockReadyTick = now + CombatConfig.BLOCK_RELEASE_COOLDOWN.get();
        s.guardRegenTick = now + CombatConfig.GUARD_REGEN_DELAY.get();
    }

    /** Runs before damage is applied. Handles dash i-frames and blocking. */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer victim)) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // /kill, void

        CombatState s = CombatManager.peek(victim);
        if (s == null) return;
        long now = victim.level().getGameTime();
        ServerLevel level = (ServerLevel) victim.level();

        // dash i-frames
        if (now < s.iFramesUntil) {
            e.setCanceled(true);
            level.sendParticles(ParticleTypes.CLOUD, victim.getX(), victim.getY() + 1.0, victim.getZ(), 4, 0.3, 0.4, 0.3, 0.02);
            return;
        }

        if (!s.blocking) return;

        Entity direct = src.getDirectEntity() != null ? src.getDirectEntity() : src.getEntity();
        if (direct == null) return; // environmental damage (fire, fall, ...) is never blockable

        if (CombatConfig.BLOCK_FRONT_ONLY.get() && !isInFront(victim, direct)) return;

        float amount = e.getAmount();
        Entity attackerEntity = src.getEntity();

        // ---- perfect block ----
        boolean inWindow = now - s.blockStartTick <= CombatConfig.PERFECT_WINDOW.get();
        if (inWindow && !s.perfectUsed) {
            s.perfectUsed = true;
            s.lastResult = CombatState.BlockResult.PARRIED;
            s.blockReadyTick = 0; // parries refresh the block
            e.setCanceled(true);

            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.4f);
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.7f);
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY() + 1.2, victim.getZ(), 20, 0.4, 0.4, 0.4, 0.3);
            level.sendParticles(ParticleTypes.FLASH, victim.getX(), victim.getY() + 1.2, victim.getZ(), 1, 0, 0, 0, 0);

            if (attackerEntity instanceof LivingEntity attacker) {
                Vec3 away = attacker.position().subtract(victim.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-6) {
                    away = away.normalize().scale(0.7);
                    attacker.setDeltaMovement(away.x, 0.2, away.z);
                    attacker.hurtMarked = true;
                }
                int stagger = CombatConfig.PERFECT_STAGGER.get();
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stagger, 2, false, false, true));
                if (attacker instanceof ServerPlayer ap) {
                    CombatState as = CombatManager.get(ap);
                    as.stunnedUntil = Math.max(as.stunnedUntil, now + stagger);
                    as.combo = 0;
                }
            }
            return;
        }

        // ---- normal block / guard break ----
        float drain = amount * CombatConfig.GUARD_DRAIN_PER_DAMAGE.get().floatValue();
        s.guard -= drain;
        s.guardRegenTick = now + CombatConfig.GUARD_REGEN_DELAY.get();

        if (s.guard <= 0f) {
            s.guard = 0f;
            s.guardBroken = true;
            s.lastResult = CombatState.BlockResult.BROKEN;
            s.stunnedUntil = now + CombatConfig.GUARD_BREAK_STUN.get();
            release(s, now);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CombatConfig.GUARD_BREAK_STUN.get(), 4, false, false, true));
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, 0.9f);
            level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + 1.2, victim.getZ(), 25, 0.5, 0.5, 0.5, 0.3);
            // full damage goes through on a guard break
            return;
        }

        s.lastResult = CombatState.BlockResult.BLOCKED;
        e.setAmount(amount * (1.0f - CombatConfig.BLOCK_REDUCTION.get().floatValue()));
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY() + 1.2, victim.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
    }

    private static boolean isInFront(ServerPlayer victim, Entity source) {
        Vec3 to = source.position().subtract(victim.position()).multiply(1, 0, 1);
        Vec3 look = victim.getLookAngle().multiply(1, 0, 1);
        if (to.lengthSqr() < 1e-6 || look.lengthSqr() < 1e-6) return true;
        double cos = to.normalize().dot(look.normalize());
        return cos >= Math.cos(Math.toRadians(CombatConfig.BLOCK_FRONT_ARC.get() / 2.0));
    }

    private BlockSystem() {}
}
