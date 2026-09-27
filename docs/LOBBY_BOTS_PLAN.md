# Plan: shared setup UI, and server hosted bot lobbies

Status: proposed, not started. Every phase is one commit so any of them can be reverted on
its own.

## Why

`CustomGameSetupStage` (851 lines) is the good lobby setup screen. It already has 8 player
slots with per slot `isAI` and `isHost`, a map preview with per slot spawn marks, a fog
checkbox, game mode, daytime and season. It was left alone while a second, thinner creation
UI was written in `LobbiesStage` (272 lines), which is why server lobbies have no bot slots
today.

The fix is to reuse the existing setup UI rather than grow the new screen, and to let that
same UI drive a server hosted lobby. Extraction rather than dual purpose, so the local flow
keeps working untouched and bot support lands once.

## Invariants

These must hold after every phase:

- The local and LAN flow behaves exactly as before. It is the regression surface for the
  extraction, since it is the only current user of the setup screen.
- No balance or economy change. Bot difficulty uses the existing `bot.v2` factory.
- Unit and tile ids, and the region names in the atlases, stay untouched.
- The server stays authoritative. Clients never run bots, the server does.
- Turn timers stay off by default.

## Known prerequisite

`ServerCore.startMatch` sets `battle.clientControlled = true` on the **server** battle, and
that flag does the opposite of what it says: `Battle.update` skips `botManager.update` when
it is set, and `BotManager.update` returns early. It is currently harmless because
`startMatch` never creates a bot player, so `botManager` is empty. It becomes a silent
blocker the moment a server match contains a bot: the bot simply never acts and the game
waits for a turn that never comes.

The headless `botmatch` mode is a different path (`DedicatedServer.startBotMatch`) that does
not set the flag, which is why bots work there today.

## Phase 0: fix the flag

Remove `battle.clientControlled = true` from `ServerCore.startMatch`, so the authoritative
battle runs its own bots, matching the `botmatch` path.

**Verification**

- `.\gradlew.bat :server:runServer --args="botmatch normal"` still runs to completion with
  the same bot behaviour. This path never set the flag, so it is a pure regression check.
- `.\gradlew.bat :core:test` passes.
- Honest caveat: this phase has no positive test of its own, because no server match has a
  bot until Phase 2. The flag is verified by Phase 2, and until then it is a change with no
  observable behaviour. If you would rather not carry an untested change, Phase 0 can be
  folded into Phase 2 as one commit.

## Phase 1: extract the shared setup UI

Move the reusable half of `CustomGameSetupStage` into a widget both flows can host:

- the player slot strip, including per slot `isAI`, `isHost` and the empty slot affordance,
- the map preview with per slot marks, and the mark logic (`changePlayerMark`),
- the settings row: fog checkbox, game mode, daytime, season.

The widget exposes state and callbacks, and knows nothing about transports. The local flow
(`MpLobbyVariantsStage` plus the P2P `GameServer`/`GameClient` path) keeps using it
unchanged. `CustomGameSetupStage` shrinks and becomes the local flow's host for the widget.

`LobbiesStage` is not touched in this phase.

**Verification**

- `.\gradlew.bat :core:compileKotlin :desktop:classes` and `.\gradlew.bat :core:test`.
- Manual, local flow: main menu, "Local and LAN games", then the custom game screen. Check
  the 8 slots render, a slot opens its action menu, bot and player options still work, the
  map preview updates and its spawn marks follow the selected slot, the fog checkbox, game
  mode, daytime and season still apply, and starting a local bot game still works.
- Manual, map preview: open a map and confirm tiles render (this is the `MapPreview` lazy
  view fix from the last commit, worth re checking here since the preview moves).
- Diff discipline: this phase is a relocation. `CustomGameSetupStage.kt` must get **smaller**,
  and the extracted widget should read as moved code, not rewritten code. If you see logic
  change during the move, that is a red flag for this phase.

## Phase 2: protocol and server side bots

Protocol, in `ctmn.petals.net.proto.Messages` and `ServerCore`:

- `LobbyMember` gains `isAi` and `botDifficulty`. It has neither today, which is the actual
  reason a bot slot cannot be represented.
- `CreateLobby` gains a bot count and a bot difficulty.
- `Lobby` in `ServerCore` (line 682) stores the bot slots alongside `members`, so `toState`
  and `toInfo` can report a lobby of "2 players and 2 bots" without inventing players.
- `startMatch` (line 438) creates a `Player` per bot slot, assigns ids and team ids after the
  humans, registers each with `BotFactory.create(player, battle, difficulty)`, and lets the
  normal tick drive them. No bot related special casing beyond slot creation: the existing
  `Battle.update` and `BotManager.update` already do the right thing once the flag is fixed.

The transport for this already exists and needs no change: `GameStateSnapshot` derives
`aiPlayers` from `battle.botManager.botPlayers`, and the client already registers an inert
local bot for each entry in `snapshot.aiPlayers`. Those client side bots stay inert because
the client sets `clientControlled = true`, which is exactly what stops them double playing.

**Verification**

- New debug mode `botmatch online` (a server hosted match of 1 human plus 1 bot) so this is
  testable without two windows. Expected in the server log, with `verboselog` on:
  a match start, then `Match <id>: ... -> MoveUnitCommand` / `BuyUnitCommand` lines whose
  player is the bot, with no human input. If the bot never acts, the flag or the bot
  registration is wrong.
- A `2 players 2 bots` match over the network, using the two client run in Phase 3.
- Capacity must not regress: `.\gradlew.bat :server:runServer --args="--loadtest 100 --duration 20"`
  still completes with 100 clients and 50 concurrent matches.
- `botmatch normal` (pure bot vs bot) still finishes.

## Phase 3: client wiring, and shrink `LobbiesStage`

- The extracted setup screen gains a server mode: the same slots and map, but backed by the
  server lobby messages, and its start action goes to `OnlinePlayScreen` instead of
  `HostPlayScreen`.
- `LobbiesStage` keeps only what it is good at: display name, server address, connect, the
  lobby list, join, and the quick match queue. Lobby creation moves out to the setup screen.
- The old local and LAN button stays exactly where it is.

**Verification**

- Two client run, and this is the real test:
  - `.\gradlew.bat :server:runServer --args="--port 7396 verboselog"` in one terminal,
  - `.\gradlew.bat :desktop:run --args="onlinetest localhost 7396"` in two others.
- Create a 2 player 2 bot lobby from the first client, join from the second, start it, and
  play a few turns. Check: the bot buys and moves on its own without any human input, both
  clients see the bot's commands (the peer logs `applying ...` at normal log level only for
  gaps, so use `verboselog` on the clients to see per command lines), and no snapshot spam.
- The browser no longer creates a lobby inline; it hands off to the setup screen.
- Local and LAN flow still works end to end, unchanged.
- `.\gradlew.bat :core:test` passes.

## Out of scope, still open

These are known and deliberately not in this plan:

1. **Client side legality versus fog.** The client validates moves against its fog filtered
   mirror, so a tile that looks free because the enemy unit on it is invisible looks legal to
   the player and illegal to the server. A rejected move shows as a brief ghost movement that
   snaps back. This is prediction, not rules, and it is the biggest remaining correctness
   issue. Worth doing next, and bots will make it more visible, not less.
2. **Netty `Connection reset` noise** on abrupt disconnects: the handler does not override
   `exceptionCaught`, so the server prints a stack trace when a client vanishes.
3. **Reconnect has no automated test.** Implemented on both sides, unverified end to end.
4. **JmDNS LAN discovery** in the lobby browser.
5. **Snapshot throttling, soak test, per session budgets** beyond the current flood guard.
6. **A result event protocol**, so damage from attacks inside the fog can be shown.

## Open decision

Should the quick match queue ever fill with bots? The current answer is no, since 1v1 with no
MMR was the requirement, and the queue pairs exactly two humans. A "practice against a bot"
entry point would be a separate, deliberate feature rather than a side effect of lobby
creation.
