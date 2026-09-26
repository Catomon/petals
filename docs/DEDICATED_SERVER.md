# Dedicated server

This document describes the headless dedicated server added to Fae's Feisty, how to run it,
and what is still to do.

## Overview

Multiplayer is being moved off the old "host plays and relays" model onto an authoritative
headless server. The same battle code runs in three places:

- the single player / hot seat / story client, rendered by `PlayScreen`,
- the online client, rendered by a future `OnlinePlayScreen` on top of the same view,
- the dedicated server, which runs the battle with no view at all.

## Architecture

- `ctmn.petals.sim.Battle` is the render free battle: players, turn and command managers,
  RNG, end conditions, the command pipeline and the resolved rules. It never touches GL,
  assets, a scene2d stage or `Gdx` directly.
- `ctmn.petals.sim.BattleField` is the query surface for units, tiles and labels. It is
  implemented by `PlayStage` (scene2d, client) and by `SimField` (plain lists, server).
- Visual work is requested through hooks on `Battle` (`showFloatingLabel`,
  `showHealthChange`, `showEffect`, `queueSeqAction`, `onUnitDiedView`, ...). They are no-ops
  on the server and overridden by `PlayScreen`.
- `PlayScreen` now owns a scene2d `PlayStage` and delegates all simulation to `Battle`.
- `ctmn.petals.sim.CombatRules.resolveAttack` performs an attack entirely up front
  (damage, splash, deaths, retaliation, randomness) and returns an `AttackOutcome`; the
  `AttackAction` is only an animation over that result. The same idea applies to movement,
  which now fires `UnitMovedEvent` and applies terrain damage in the command.
- Tile definitions no longer come from a `TextureAtlas`. `android/assets/tiles.json` is a
  generated catalog (name plus terrain) that `TileData` loads on both client and server.

## Server core

- `ctmn.petals.net.ServerCore` is the netty server: connections, the lobby registry, the
  quick match queue and running matches. All battle state lives on one `server-session`
  thread; IO threads only parse frames and enqueue them.
- `ctmn.petals.net.ServerClient` is the client side of the protocol.
- `ctmn.petals.net.proto` holds the message types. Every frame is a JSON `Envelope` with a
  `type`, an optional `requestId` and a `payload` (JSON of the concrete message).
- `ctmn.petals.net.FakeClient` is a headless bot client used by the load test.
- `ctmn.petals.server.ServerConfig` and `ctmn.petals.server.DedicatedServer` wire it up.
  `ServerApp` is the headless `ApplicationListener`, `ServerMain` the standalone entry point.

### Protocol summary

Client to server: `hello`, `list_lobbies`, `create_lobby`, `join_lobby`, `leave_lobby`,
`ready`, `start`, `queue_join`, `queue_leave`, `match_command`, `snapshot_request`, `ping`.

Server to client: `welcome`, `error`, `lobby_list`, `lobby_state`, `queue_status`,
`match_found` (includes the map JSON), `match_command_broadcast`, `command_rejected`,
`match_snapshot`, `match_ended`, `pong`.

Matches are authoritative: a client sends `match_command`, the server validates ownership
(`command.playerId` is forced to the sender, and `EndTurnCommand.turnPlayerId` too), checks
`canExecute`, executes on the session thread, increments the match state id and broadcasts
the accepted command. A rejected command answers with `command_rejected` and a fresh
snapshot so the client can resync.

## Running

Standalone server module:

```
.\gradlew.bat :server:runServer --args="--port 7396 --name MyServer"
```

In-game headless server (same code, launched from the game project):

```
.\gradlew.bat :desktop:run --args="dedicatedserver --port 7396"
```

Smoke tests:

```
# one headless bot vs bot match, exits when it ends
.\gradlew.bat :server:runServer --args="botmatch normal"

# N fake clients against the real netty server for the given seconds
.\gradlew.bat :server:runServer --args="--loadtest 8 --duration 15"
```

Options (all optional): `--port`, `--name`, `--max-players`, `--max-sessions`,
`--turn-timer <seconds>`, `--maps a,b,c`, `--no-lan`, `--loadtest N`, `--duration S`.
Turn timers are off by default, as requested.

## Done

- Headless simulation core (`Battle`, `BattleField`, `SimField`, `ServerBattle`,
  `BattleSetup`), including combat/movement resolution moved out of animations.
- Commands, turn cycle, end conditions, abilities and the bot all run against `Battle`.
- Tile catalog and map conversion are GL free; the unit registry constructs without assets.
- Netty server with lobbies (create, join, leave, ready, host start), quick match queue
  (pairs two players on a random map) and authoritative match command relay.
- `server` Gradle module, in-game `dedicatedserver` argument, bot match and load test modes.
- Shared fog of war rules in `ctmn.petals.sim.FogOfWar`, used by the client renderer and by
  the server to filter snapshots: enemy units out of view are not sent at all, and unseen
  tiles keep their terrain for rendering but lose ownership, health, crystals and build
  progress. Implemented in `ctmn.petals.net.SnapshotFilter`.
- Reconnect: a dropped player keeps their seat for a grace period (`--reconnect`, 60s by
  default) and picks the match back up with a fresh snapshot. The online client
  reconnects by itself with the same id.
- Optional server side turn timer (`--turn-timer <seconds>`, off by default) that ends
  stalled turns on the session thread.
- Lobby creation settings (map, player count, fog) and a per connection flood guard that
  drops clients exceeding 120 messages per second.

### Verified

- `:core`, `:desktop`, `:server` compile and `:core:test` passes.
- A headless bot match runs with no GL context.
- Capacity: **100 fake clients and 50 concurrent matches** stay stable through the real
  netty server (`--loadtest 100 --duration 20`), which covers the target of a couple dozen
  sessions and around 100 players. Turners and fog filtered snapshots are exercised.
- `:desktop:run --args="dedicatedserver --port 7490"` starts the server headless.

## Client

- `ctmn.petals.menu.LobbiesStage` is the lobby browser: enter a display name, connect to a
  server by address, browse its lobbies and join one, create a lobby (map, player count and
  fog of war) or use the quick match queue. It is reachable from the main menu through the
  Match and Vs Player buttons, and it keeps the old local and LAN host flow one button away.
  The server advertises its map pool in the welcome message, so the map picker only offers
  maps the server can actually host.
- `ctmn.petals.multiplayer.OnlinePlayScreen` plays a server match. It builds the map from the
  `map_json` in `match_found`, mirrors the battle locally for the view, applies accepted
  commands from other players, and sends its own commands to the server. A rejection or a
  state gap is repaired with a server snapshot.
- `ctmn.petals.net.ServerClient` is the protocol client both the screens and the headless
  `FakeClient` use.
- The player display name is stored in `GamePref.displayName` and edited in the lobby screen.

## Remaining

1. LAN server discovery with JmDNS in the lobby browser, so a local server shows up without
   typing its address. The old P2P discovery still exists on the LAN games screen.
2. Scale polish: snapshot throttling and a longer soak test.
3. Reconnect has no automated test yet. The server and client paths are implemented, but
   driving a drop mid match from the harness proved flaky, so it is unverified.
4. Fog filtered snapshots mean a client cannot replay a command that involves a unit it
   cannot see, so it falls back to a snapshot after such a command. Correct, but it can
   mean a snapshot per command during heavy fighting. A result-event protocol would remove
   that.
5. An end to end test with two real game clients. The screens compile and the protocol is
   covered by the fake client load test, but the UI flow still needs a manual pass with two
   game instances.
