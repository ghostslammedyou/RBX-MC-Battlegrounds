package com.brawlcore.client;

import com.brawlcore.net.Payloads;

/** Latest combat state received from the server. Plain data, safe to reference from common code. */
public final class ClientCombatState {
    public static volatile int dashCd;
    public static volatile int dashMax = 1;
    public static volatile int guard = 1;
    public static volatile int guardMax = 1;
    public static volatile int combo;
    public static volatile boolean blocking;
    public static volatile boolean stunned;
    public static volatile boolean perfectWindow;

    public static void apply(Payloads.SyncPayload p) {
        dashCd = p.dashCd();
        dashMax = Math.max(1, p.dashMax());
        guard = p.guard();
        guardMax = Math.max(1, p.guardMax());
        combo = p.state() & 15;
        int flags = p.state() >> 4;
        blocking = (flags & 1) != 0;
        stunned = (flags & 2) != 0;
        perfectWindow = (flags & 4) != 0;
    }

    public static void reset() {
        dashCd = 0;
        guard = guardMax;
        combo = 0;
        blocking = false;
        stunned = false;
        perfectWindow = false;
    }

    private ClientCombatState() {}
}
