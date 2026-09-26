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
- Verified: core, desktop and server compile; a headless bot match runs; an 8 client load
  test formed 4 concurrent matches and exchanged turns with no exceptions.

## Client

- `ctmn.petals.menu.LobbiesStage` is the lobby browser: enter a display name, connect to a
  server by address, browse its lobbies and join one, create a lobby, or use the quick match
  queue. It is reachable from the main menu through the Match and Vs Player buttons, and it
  keeps the old local and LAN host flow one button away.
- `ctmn.petals.multiplayer.OnlinePlayScreen` plays a server match. It builds the map from the
  `map_json` in `match_found`, mirrors the battle locally for the view, applies accepted
  commands from other players, and sends its own commands to the server. A rejection or a
  state gap is repaired with a server snapshot.
- `ctmn.petals.net.ServerClient` is the protocol client both the screens and the headless
  `FakeClient` use.
- The player display name is stored in `GamePref.displayName` and edited in the lobby screen.

## Remaining

1. Per player fog of war filtering of snapshots. The server currently sends full snapshots.
   Extract the visibility computation from `FogOfWarDrawer` into `sim` and filter
   `GameStateSnapshot` per viewer.
2. Lobby map and settings editing (the lobby currently takes a random map from the server
   pool with fog on), plus LAN server discovery with JmDNS in the lobby browser.
3. Reconnect grace period and disconnect handling in matches.
4. Scale pass: snapshot throttling, rate limits, per session budgets, and a larger load test
   (roughly 24 matches and 100 connections).
5. An end to end test with two real game clients. The screens compile and the protocol is
   covered by the fake client load test, but the UI flow still needs a manual pass with two
   game instances.
