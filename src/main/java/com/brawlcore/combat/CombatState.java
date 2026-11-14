package com.brawlcore.combat;

/** Per-player combat bookkeeping. Server side only. All "Tick" fields are absolute game-time ticks. */
public final class CombatState {
    public enum BlockResult { NONE, BLOCKED, PARRIED, BROKEN }

    // dash
    public long dashReadyTick;
    public long iFramesUntil;
    public long dashEndTick;

    // M1
    public int combo;
    public long comboExpireTick;
    public long nextM1Tick;

    // block
    public boolean wantsBlock;
    public boolean blocking;
    public boolean perfectUsed;
    public boolean guardBroken;
    public long blockStartTick;
    public long blockReadyTick;
    public long guardRegenTick;
    public float guard = -1f; // -1 = not initialised yet, set to max on first tick

    // stun (guard break or being parried)
    public long stunnedUntil;

    // set by BlockSystem so M1System can tell what happened to its hit
    public BlockResult lastResult = BlockResult.NONE;

    // last values sent to the client, to avoid spamming sync packets
    public int lastCd = -1;
    public int lastGuard = -1;
    public int lastState = -1;

    public boolean isStunned(long now) {
        return now < stunnedUntil;
    }
}
