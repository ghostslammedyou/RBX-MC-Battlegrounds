package com.brawlcore.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * M1 = basic attack combo. Four hits; the fourth is a finisher that launches the target.
 * Hits are aim-assisted by a cone, so you don't need pixel-perfect aim like vanilla.
 */
public final class M1System {
    private static final int FINISHER_INDEX = 3;

    public static void tryM1(ServerPlayer p) {
        CombatConfig.M1Mode mode = CombatConfig.M1_MODE.get();
        if (mode == CombatConfig.M1Mode.OFF) return;
        if (mode == CombatConfig.M1Mode.EMPTY_HAND && !p.getMainHandItem().isEmpty()) return;
        if (p.isSpectator() || p.isDeadOrDying()) return;

        CombatState s = CombatManager.get(p);
        long now = p.level().getGameTime();
        if (s.isStunned(now) || s.blocking || now < s.nextM1Tick) return;

        if (now > s.comboExpireTick) s.combo = 0;
        int index = s.combo;
        boolean finisher = index == FINISHER_INDEX;
        s.combo = finisher ? 0 : index + 1;
        s.comboExpireTick = now + CombatConfig.M1_COMBO_RESET.get();
        s.nextM1Tick = now + (finisher ? CombatConfig.M1_FINISHER_DELAY.get() : CombatConfig.M1_DELAY.get());

        p.swing(InteractionHand.MAIN_HAND, true);

        ServerLevel level = (ServerLevel) p.level();
        LivingEntity target = findTarget(p);
        if (target == null) {
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.3f);
            return;
        }

        float damage = CombatConfig.M1_DAMAGE.get().floatValue();
        if (finisher) damage *= CombatConfig.M1_FINISHER_MULT.get().floatValue();

        CombatState ts = CombatManager.peek(target);
        if (ts != null) ts.lastResult = CombatState.BlockResult.NONE;

        target.invulnerableTime = 0; // let rapid combo hits through vanilla's 10-tick i-frames
        boolean hurt = target.hurt(p.damageSources().playerAttack(p), damage);

        CombatState.BlockResult result = ts != null ? ts.lastResult : CombatState.BlockResult.NONE;
        if (!hurt || result != CombatState.BlockResult.NONE) {
            // blocked / parried / dodged: no knockback, no hitstun
            return;
        }

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                finisher ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.0f, finisher ? 0.9f : 1.1f);
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                finisher ? 18 : 8, 0.3, 0.3, 0.3, 0.2);

        if (ts != null) {
            // hitstun: interrupt the victim's own M1 chain briefly
            ts.nextM1Tick = Math.max(ts.nextM1Tick, now + CombatConfig.M1_HITSTUN.get());
            ts.combo = 0;
        }

        if (finisher) {
            Vec3 away = target.position().subtract(p.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-6 ? p.getLookAngle().multiply(1, 0, 1) : away;
            away = away.normalize().scale(CombatConfig.M1_FINISHER_KNOCKBACK.get());
            target.setDeltaMovement(away.x, CombatConfig.M1_FINISHER_LAUNCH.get(), away.z);
            target.hurtMarked = true;
            level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.8, target.getZ(), 1, 0, 0, 0, 0);
        }
    }

    private static LivingEntity findTarget(ServerPlayer p) {
        double range = CombatConfig.M1_RANGE.get();
        double cosHalf = Math.cos(Math.toRadians(CombatConfig.M1_ARC.get() / 2.0));
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        AABB box = p.getBoundingBox().inflate(range);

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box,
                x -> x != p && x.isAlive() && !x.isSpectator() && x.isAttackable())) {
            if (e instanceof Player other && !p.canHarmPlayer(other)) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range + e.getBbWidth() / 2.0) continue;
            if (dist > 1.0e-4 && to.normalize().dot(look) < cosHalf) continue;
            if (!p.hasLineOfSight(e)) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }

    private M1System() {}
}
