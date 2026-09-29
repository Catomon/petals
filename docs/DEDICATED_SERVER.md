# Dedicated server

This document describes the headless dedicated server added to Fae's Feisty, how to run it,
and what is still to do.

## Overview

Multiplayer is being moved off the old "host plays and relays" model onto an authoritative
headless server. The same battle code runs in three places:

- the single player / hot seat / story client, rendered by `PlayScreen`,
- the online client, rendered by `ctmn.petals.multiplayer.OnlinePlayScreen` on top of the
  same view,
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

# the first client to join the queue is seated against a bot instead of
# waiting for a second human, which is how the bot path is checked with a
# single game client
.\gradlew.bat :server:runServer --args="--port 7396 botmatchonline hard"
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
- **Lobby lifecycle and the shared setup pane.** `GameSetupPane` holds the map preview, the
  eight slots and the settings row, and the local, LAN and dedicated flows all drive it. A
  dedicated lobby is created when the host opens the setup screen and is published to the
  browser by a separate press, so it can be arranged in private. The host decides the map,
  and the server carries it: a map that only exists on the host's machine travels as
  `mapJson` and reaches every client in `match_found`. Seats are slots and the slot only
  decides map position; a player's id, which is their colour, is assigned once and never
  changes when they move. `ServerLobbySetupStage` sends the whole arrangement on every edit,
  which is how the LAN room worked, so joiners follow live.
- Delta first sync: a full snapshot is no longer sent after every command, and commands
  hidden by fog are skipped instead of triggering a resync (see "Sync model" above).
- `onlinetest` launch argument for driving a real client into a match from the command line,
  which is what the two client end to end run used.
- **Server hosted bots.** A lobby can carry bot seats, `startMatch` seats them after the human
  players and registers each with `BotFactory.create`, and the server battle is no longer
  flagged `clientControlled`, so `Battle.update` ticks the bots. The transport already existed:
  snapshots carry `aiPlayers` and the client already keeps those bots inert.
- `botmatchonline` debug mode, where the first client to join the quick match queue is seated
  against bots instead of waiting for a second human, so the bot path can be checked with one
  game client.
- The client no longer re-sends commands that came from the server, which is what produced a
  rejected command every time the turn timer ended a turn. `Command.fromServer` marks them.

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
- **Two real clients through the dedicated lobby**: the host opens a lobby, picks a map from
  their own machine, publishes it, the second client finds it in the browser and joins, both
  see the same map and settings and their own colours, and the host starting the match puts
  both into the same game on the host's map.
- **A human against a server hosted bot**: `botmatchonline` seats the first queueing client
  with a bot. The bot buys units and plays its own turns with no human input, the server logs
  no exceptions, and no command is rejected.
- A plain headless bot vs bot match and the 100 client load test both still run clean.

## Client

Playing another person is one chooser: `ctmn.petals.menu.MpLobbyVariantsStage` offers same
screen, LAN and online, each with its own screen behind it.

- Same screen goes to the local setup (`CustomGameSetupStage`).
- LAN is the peer hosted room: host, or join by address or by JmDNS search.
- Online goes to `LobbiesStage`, the dedicated server browser. It connects to
  `Config.DEFAULT_SERVER_HOST` (a server on this machine) on entry, lists the lobbies the
  server has, and offers create, quick match and refresh. The address form lives in
  `ServerConnectStage`, reached from the browser, and is only needed for a server elsewhere.
- `ctmn.petals.menu.ServerLobbySetupStage` is the dedicated lobby, built on the same
  `GameSetupPane` as the local and LAN flows, so the map preview, eight slots, settings row
  and footer behave the same everywhere. The host's lobby exists from the moment this screen
  opens; Open publishes it to the browser and Close takes it back out. Every edit is sent on,
  and the players sitting in the lobby see it immediately.
- The host picks the map, from their own machine or from the maps the server already has. The
  server never chooses a lobby map: it carries the map (`mapJson`) to everyone in the lobby and
  to every client in `match_found`, so a joiner does not need the file.
- A lobby is a room, not a map: it exists from the moment the host opens the setup screen, can
  be published before a map is chosen, and only starting needs one. Every place the map offers
  must be filled before a match can start, on the host's screen and on the server, because an
  empty place is a base nobody owns. A seat past the map's places is a seat out, and its player
  only watches.
- The browser lists open, closed and in game lobbies, sorted with the joinable ones first and
  the fullest of those first. A lobby that started a match stays listed as IN GAME until its
  match ends, and only an open lobby with room can be joined.
- Seats are slots; a player keeps the id the lobby gave them, and the id is the colour. A
  slot past the map's player places means no map place at all, which is how sitting out to
  watch works. The server stores the seat of every member, not just the host's and the bots',
  so the arrangement the host sees is the arrangement everybody gets: a seat the host swapped
  stays swapped, and a species the host picked for somebody sticks.
- Anybody may take a different seat, not only the host. A seat request swaps them with whoever
  is in the way, a bot included, and the ids do not change, so a player keeps their colour
  wherever they sit. The match starts from the lobby's own seating, ids included.
- `ctmn.petals.multiplayer.OnlinePlayScreen` plays a server match. It builds the map from the
  `map_json` in `match_found`, mirrors the battle locally for the view, applies accepted
  commands from other players, and sends its own commands to the server. A rejection or a
  state gap is repaired with a server snapshot.
- `ctmn.petals.net.ServerClient` is the protocol client both the screens and the headless
  `FakeClient` use.
- The player display name is stored in `GamePref.displayName`.

## Remaining

1. **Real spectator slots.** Sitting past the map's player places already watches the match,
   but there is no separate spectator list, no way to join an already running match as a
   watcher, and no spectator cap. That is the next piece.
2. **Reconnect has no automated test.** Server and client paths are implemented and the
   grace period reaping is visible in the log, but driving a drop mid match from a harness
   proved flaky, so it is only verified by hand.
3. **LAN server discovery in the browser** with JmDNS, so a local dedicated server shows up
   without typing its address. The peer to peer discovery still exists on the LAN screen.
4. **Snapshot throttling, a longer soak test**, and per session budgets beyond the current
   flood guard.
5. **A result event protocol**, so damage numbers and deaths could be shown for attacks that
   happen inside the fog instead of the client simply not knowing about them.
