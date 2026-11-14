# Brawl Core (NeoForge 1.21.1)

Battlegrounds-style combat for Minecraft 1.21.1 + NeoForge, inspired by The Strongest Battlegrounds,
Jujutsu Shenanigans and Legends Battlegrounds, plus a main-menu button that creates and opens a
multiplayer world in one click.

## Controls (all rebindable in Options > Controls > Brawl Core)

| Action | Default | Notes |
|---|---|---|
| Dash | `Z` | Hold W/A/S/D for direction (none = forward). Side/back dashes give longer i-frames. |
| M1 | Left click | 4-hit combo, 4th hit is a launching finisher. Works when not aiming at a block. |
| Block | `X` (hold) | Reduces damage, drains guard. A hit right after raising block = perfect block (parry). |

## Systems

- **Dash** (Legends Battlegrounds style): short snappy burst, momentum cut after 5 ticks, 2s cooldown, i-frames, air dash.
- **M1** (TSB / JJS style): aim-assist cone, 6-tick hit cadence, hitstun interrupts the victim's M1 chain, finisher knockback.
- **Block** (TSB / JJS style): guard meter, perfect block window (6 ticks), guard break stun, release cooldown to stop parry spam.
- **Quick Multiplayer World button**: extra main-menu button. Creates a new survival world without the Create World
  screen and opens it to LAN as soon as it loads. The button finds a free spot so it doesn't overlap Essential's widgets.

All numbers are in `saves/<world>/serverconfig/brawlcore-server.toml` (or `defaultconfigs/` for servers).
Client options (hold-to-M1, double-tap dash, HUD, quick-host behaviour) are in `config/brawlcore-client.toml`.

## Build

Requires JDK 21.

```
gradle wrapper --gradle-version 8.10.2     # once, or copy gradlew + gradle/ from the official NeoForge MDK
./gradlew build                            # jar ends up in build/libs/
./gradlew runClient                        # test in a dev client
```

Drop the jar into `mods/` with NeoForge 21.1.x. Install it on the server too if you play on a dedicated server
(combat is server-authoritative).

## Essential compatibility

Brawl Core has no mixins and doesn't touch Essential's classes, so it can't conflict with it. The quick-host
button reads the mod list only to change its label. If Essential's own "open to friends" flow clashes with
the automatic LAN publish, set `quickHostOpensLan = false` in `brawlcore-client.toml`.
