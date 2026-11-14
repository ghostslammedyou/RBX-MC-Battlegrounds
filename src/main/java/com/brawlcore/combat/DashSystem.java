package com.brawlcore.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Battlegrounds-style dash: press the dash key (optionally with W/A/S/D held) to burst in that
 * direction. Side and back dashes grant longer i-frames than a forward dash. Short, snappy, on cooldown.
 */
public final class DashSystem {
    public static final int FORWARD = 0, BACK = 1, LEFT = 2, RIGHT = 3;

    public static void tryDash(ServerPlayer p, int dir) {
        if (p.isSpectator() || p.isDeadOrDying() || p.isPassenger() || p.isFallFlying()
                || p.getAbilities().flying || p.isInWater() && p.isSwimming()) {
            return;
        }
        CombatState s = CombatManager.get(p);
        long now = p.level().getGameTime();
        if (s.isStunned(now) || now < s.dashReadyTick) return;
        if (!p.onGround() && !CombatConfig.DASH_AIR.get()) return;
        if (dir < 0 || dir > 3) dir = FORWARD;

        // dashing out of a block cancels the block
        s.wantsBlock = false;
        BlockSystem.update(p, s, now);

        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));

        Vec3 d;
        double speed;
        int iFrames;
        switch (dir) {
            case BACK -> {
                d = forward.scale(-1);
                speed = CombatConfig.DASH_BACK_SPEED.get();
                iFrames = CombatConfig.DASH_IFRAMES_BACK.get();
            }
            case LEFT -> {
                d = right.scale(-1);
                speed = CombatConfig.DASH_SIDE_SPEED.get();
                iFrames = CombatConfig.DASH_IFRAMES_SIDE.get();
            }
            case RIGHT -> {
                d = right;
                speed = CombatConfig.DASH_SIDE_SPEED.get();
                iFrames = CombatConfig.DASH_IFRAMES_SIDE.get();
            }
            default -> {
                d = forward;
                speed = CombatConfig.DASH_FRONT_SPEED.get();
                iFrames = CombatConfig.DASH_IFRAMES_FRONT.get();
            }
        }

        Vec3 cur = p.getDeltaMovement();
        double vy = p.onGround() ? 0.1 : cur.y;
        p.setDeltaMovement(d.x * speed, vy, d.z * speed);
        p.hurtMarked = true;
        p.fallDistance = 0;

        s.dashReadyTick = now + CombatConfig.DASH_COOLDOWN.get();
        s.iFramesUntil = Math.max(s.iFramesUntil, now + iFrames);
        s.dashEndTick = now + CombatConfig.DASH_END_TICKS.get();

        ServerLevel level = (ServerLevel) p.level();
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 0.6f);
        // afterimage trail at the start position
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.9, p.getZ(), 10, 0.25, 0.5, 0.25, 0.02);
        level.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.1, p.getZ(), 6, 0.2, 0.05, 0.2, 0.02);
    }

    private DashSystem() {}
}
