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

Commands travel by class name. Newer peers send the full class name; the simple name is
still accepted and resolved against `ctmn.petals.playscreen.commands`, so an older client
keeps working.

### Sync model: deltas, not snapshots

A snapshot is the expensive message: it serialises the whole field. So it is only sent when
a client genuinely cannot continue from the command stream:

- at match start,
- after a rejected command, as the repair,
- when a client reports a gap of more than one command,
- on reconnect, to hand back the seat.

Everything else travels as `match_command_broadcast`. The broadcast stream is ordered and
reliable over TCP, so a client that applies every command stays in sync on its own. There
is deliberately no snapshot after each command; doing that made every action cost a full
state transfer.

One case needs care: a command about a unit the client cannot see through the fog cannot be
replayed locally. Instead of paying for a snapshot, the client steps its state counter over
that command and keeps going. What it skips is by definition state it is not allowed to
know, so the local mirror stays a correct partial view and the command stream stays aligned.
If the command turns out to be about something the client *can* see, or the client is more
than one command behind, it falls back to a real snapshot so genuine desync is never masked.

### Logging

`ServerCore` logs match starts, accepted commands, rejections, connects and disconnects
always. The per message trace (`recv <type>`) and the client side per command chatter are
behind the `verboselog` flag, which the server accepts as `verboselog` or `--verbose-log`.
Client divergence (`gap: server state N, local M`) and fog skips are logged at normal level,
since they are rare and worth seeing.

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

# trace every protocol message, useful when watching a live match
.\gradlew.bat :server:runServer --args="--port 7396 verboselog"
```

End to end with two real clients, which is how the online flow was verified:

```
# terminal 1
.\gradlew.bat :server:runServer --args="--port 7396"

# terminals 2 and 3, each opens a game window and joins the quick match queue
# on startup without touching the menus
.\gradlew.bat :desktop:run --args="onlinetest localhost 7396"
```

`onlinetest` is a smoke test entry point in `ctmn.petals.multiplayer.OnlineAutoJoin`: it
connects, queues and drops into `OnlinePlayScreen` as soon as the server finds a match. It
takes an optional host and port and uses a unique client id per run, so several instances on
one machine do not look like a reconnect of each other.

Options (all optional): `--port`, `--name`, `--max-players`, `--max-sessions`,
`--turn-timer <seconds>`, `--maps a,b,c`, `--no-lan`, `--reconnect <seconds>`,
`--loadtest N`, `--duration S`, `verboselog`.
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
- Delta first sync: a full snapshot is no longer sent after every command, and commands
  hidden by fog are skipped instead of triggering a resync (see "Sync model" above).
- `onlinetest` launch argument for driving a real client into a match from the command line,
  which is what the two client end to end run used.

### Verified

- `:core`, `:desktop`, `:server` compile and `:core:test` passes.
- A headless bot match runs with no GL context.
- Capacity: **100 fake clients and 50 concurrent matches** stay stable through the real
  netty server (`--loadtest 100 --duration 20`), which covers the target of a couple dozen
  sessions and around 100 players. Turners and fog filtered snapshots are exercised.
- `:desktop:run --args="dedicatedserver --port 7490"` starts the server headless.
- **Two real clients against one dedicated server**: both connect, get matched, and play.
  Buying, moving, attacking and ending turns all relay correctly in both directions, with
  the state ids lining up (`expected state N, local N+1`, server increments to N+1, peer
  applies it). This run found and fixed four real bugs, listed under Remaining.

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

Bugs and gaps found by the two client run, in the order they were hit:

1. **Client side legality versus fog (open, the biggest one).** The client checks move
   legality against its own fog filtered mirror, so a tile that looks free because the enemy
   unit on it is invisible looks legal to the player and illegal to the server. The server
   is right, so the client is committing actions it should not. It needs to stop committing
   fog optimistic actions: either ask the server to validate a move before animating it, or
   treat the rejection as normal and snap. Until then a rejected move is visible as a brief
   ghost movement followed by a snap back.
2. **`battle.clientControlled = true` on the server battle** (`ServerCore.startMatch`) reads
   backwards. The flag is documented as "driven by a remote server, do not run local bots",
   so the authoritative battle should not set it. Harmless for two human players, but it
   would suppress server side bots.
3. **Netty "Connection reset" noise on the server console** when a client goes away. The
   handler does not override `exceptionCaught`, so an abrupt close prints a full stack trace
   through the default pipeline warning. It should be caught and treated as a disconnect.
4. **Reconnect has no automated test.** Server and client paths are implemented and the
   grace period reaping is visible in the log, but driving a drop mid match from the harness
   proved flaky, so it is unverified end to end.

Still not built:

5. LAN server discovery with JmDNS in the lobby browser, so a local server shows up without
   typing its address. The old P2P discovery still exists on the LAN games screen.
6. Snapshot throttling, a longer soak test, and per session budgets beyond the current
   flood guard.
7. A result event protocol, so damage numbers and deaths could be shown for attacks that
   happen inside the fog instead of the client simply not knowing about them.
