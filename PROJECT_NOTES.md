# Project Notes and Gotchas

Practical, non-obvious things learned while working on Fae's Feisty. Meant to save a new
session time. See also `AGENTS.md` (general), `FLAWS.md` (technical debt) and `ECONOMY.md`.

## The command pipeline

Human actions and AI actions both go through Commands:

1. Something queues a command with `commandManager.queueCommand(command)`.
2. `Command.canExecute(playScreen)` is checked first; if false, nothing happens.
3. `Command.execute(playScreen)` mutates game state and usually queues one or more
   sequence-actions for the visuals.
4. After a successful execute, `CommandExecutedEvent` is fired and the command history grows.

`CommandManager` only processes the queue when the action manager is empty, so no new
command runs while an animation is playing. This is why bots wait for `actionManager` to
become empty.

Sequence-actions (`seqactions/`) are the animated half: movement, attacks, wait, dialogs,
camera moves. They must not be confused with commands.

Tasks and triggers (`tasks/`, `triggers/`) are story-mission scripting: a task is an
objective, a trigger fires on a game event.

## Event system

- `PlayScreen.fireEvent(event)` fires the event on BOTH `guiStage.root` and `playStage.root`.
  So a listener on either stage sees it, which is why the GUI can react to play-stage events.
- Useful events: `NextTurnEvent`, `NextRoundEvent`, `CommandAddedEvent`,
  `CommandExecutedEvent`, `ActionStartedEvent`, `ActionCompletedEvent`, `UnitMovedEvent`,
  `UnitSelectedEvent`, `UnitDiedEvent`, `UnitBoughtEvent`, `UnitAddedEvent`,
  `UnitLevelUpEvent`, `TileCapturedEvent`, `CreditsChangeEvent`, `GameOverEvent`.
- Turn effects live in `PlayTurnCycleListener` and react to `TurnsCycleListener.TurnCycleEvent`
  (income, action point reset, capture/build/destroy ticking, buffs, damage over time).
  `NextTurnEvent` is fired by the turn manager and is good for refresh hooks.

## Action points

- `Config.ACTION_POINTS = 2` is the standard pool per turn.
- Moving needs at least `ACTION_POINTS_MOVE_MIN = 2`; a move subtracts
  `unit.actionPointsMove` (default `ACTION_POINTS_MOVE = 1`).
- Attacking subtracts `ACTION_POINTS_ATTACK = 2`.
- Capturing subtracts `ACTION_POINTS_ATTACK = 2`.
- Building anything sets the unit's action points to 0 that turn.
- Abilities subtract `ACTION_POINTS_ABILITY = 2` only on the first cast.
- `MoveAfterAttackComponent` lets a unit move after attacking (it restores action points).
- Net effect: at 2 AP you either move OR attack/capture/build, not both, unless a component
  says otherwise.

## Coordinates, tiles and terrain

- `Config.TILE_SIZE = 16`. Actor positions are pixels; `tiledX`/`tiledY` are tile indices.
  Helpers `tiled()` / `unTiled()` convert.
- A tile has a `terrain` string and a `tileName`. Important terrain predicates live in
  `TileExt.kt`: `isBase`, `isCrystal`, `isBuilding`, `isOwnable`, `isCapturable`,
  `isFluid`, `isPassableAndFree`.
- Terrain behavior (movement cost, attack/defense bonuses) comes from `TerrainProps` packs in
  `TerrainProps.kt`: `foot`, `rider` (cavalry, was `horse`), `flier`, `waterOnly`, `giant`,
  `slime`, plus `clear`, `ability`, `view`. A missing terrain in a pack is created with
  default zero values.
- Interesting quirk: several "flying" units use the `flier` movement pack but are LAND type,
  so ground units can still hit them. This is intentional (they fly to reposition, then fight
  low).

## Movement and pathing

- `PlayStage.getMovementGrid(unit, unitsAsImpassable, exceptTeam)` returns a 2D grid where a
  value `> 0` means reachable. It is not a path, just reachability.
- Special values: `UNREACHABLE = 999`, `ONE_TILE = 99` (a unit can finish its move on it but
  not continue).
- `unit.canMove(x, y)` = reachable in the grid AND tile is `isPassableAndFree()` AND the unit
  has enough AP.
- `unit.getClosestTileInMoveRange(destX, destY)` is the go-to greedy "step toward" helper.
- There is no A*; long approaches are greedy. Watch for stalls behind obstacles.

## Combat essentials

- `PlayScreen.calculateDmgDef(attacker, defender)` returns `(damage, defense)`.
- Final damage in `AttackCommand` = attack damage minus the defender's defense (minimum 0),
  with 5 percent misses and 5 percent crits when `Config.MISSES` / `Config.CRITS` are on.
- Damage/defense scale with current HP (`combatDamageHpMod`, `combatDefenseHpMod`), so
  wounded units hit softer and defend worse.
- Attack types: `ATTACK_TYPE_GROUND`, `ATTACK_TYPE_AIR`, `ATTACK_TYPE_ALL`. A ground-only
  unit cannot hit air, and vice versa.
- `Config.ATTACK_BACK = false` right now, so defenders do not retaliate in melee.
- Attacks apply `environmentDmg` to the tile under the defender (`damageTile`), which is how
  crystals/buildings can be destroyed indirectly.
- `calculateDmgDef` uses the shared game RNG, so using it during AI evaluation advances the
  game RNG (see FLAWS.md).

## Ownership, capture and destroy

- Only worker units can capture and build.
- `isCapturable` now means crystal terrain only. `isOwnable` means crystal OR base.
- `getCapturablesOf(player)` returns owned ownable tiles (bases + crystals); ownership drives
  income and win conditions.
- Capturing a crystal flips ownership and keeps the remaining deposit. Bases/buildings are
  destroy-only.
- `setPlayerForCapturableTile` is the function that actually assigns a tile to a player
  (used by capture and by level setup). It also renames the tile sprite for the owner.

## Serialization model

- Components opt into serialization by implementing `SyncedComponent`
  (`utils/serialization/SyncedComponent.kt`). That marker is the single source of truth; there
  are no allow/ignore lists anymore.
- Serialized components are keyed by fully qualified class name and loaded with
  `Class.forName`, so a component only needs the marker to round-trip.
- Serialization is used by multiplayer snapshots (`GameStateSnapshot` / `Jsonable`
  `toJsonObject` / `fromJsonObject`). Maps do NOT store components: a map stores unit ids and
  positions and rebuilds units from `Units`.
- Ashley note: `Entity.add` replaces an existing component of the same class, which is why a
  unit class adding a `ShopComponent` and `Species` adding another with `.cost()` end up with
  the later price.

## Bots

- `BotManager` tracks the bot whose player's turn it currently is. Bots get `onStart()` at
  their turn start and `onEnd()` when the next player's `NextTurnEvent` fires.
- When a bot's `isDone` becomes true, `BotManager` queues `EndTurnCommand` for it.
- The current AI lives in `ctmn.petals.bot.v2`: single `AiBot` with a `Difficulty`
  (EASY/NORMAL/HARD/INSANE) and a `BotFactory`. It plays through the same commands a human
  uses and detects at runtime whether a map has an economy.
- Difficulty scales how well the bot plays, not which units it may buy: `skill` (how often it
  misreads the board and plays a runner-up action), `ambition` (army and economy size) and
  `aggression` (how long wounded units keep pushing). EASY gates abilities, buildings and
  expansion; INSANE gets a bonus income cheat.
- `ArmyPlanner` decides buys, buildings and expansion from map size, bases, crystals, turn
  phase, enemy composition, credits and the roles from `UnitRoles` (WORKER, SCOUT, HEALER,
  RANGED, ARTILLERY, SKIRMISHER, AIR, WATER, MELEE). It buys the best value unit for a role
  instead of the cheapest, and keeps a reserve for the next unlock.
- `CombatEval` uses deterministic average damage from `calculateDmgDefStats`, so evaluation
  never consumes the game RNG. Bots have their own seeded `AiBot.random`.
- gdx `Array` iterators cannot be used nested. Materialize with `toList()` before iterating a
  bot's `speciesUnits` inside another loop over it (this crashed every buy once).
- Logging goes through `ctmn.petals.utils.GameLog`. Errors always print; `botlog` enables the
  balance lines from `BotLog` (map summary, per-turn army state, buys with role slots and
  scored runners-up, builds, cheat payouts, turn action counts); `verboselog` enables engine
  chatter (assets, event logger, turn manager, GUI listener changes, command logs).
- Run a bot vs bot spectate match with:
  `.\gradlew.bat :desktop:run --args="botvsbot"` (optional `easy` / `normal` / `hard` /
  `insane`, plus `botlog` and/or `verboselog`).

## Sprites and ids

- Unit and tile ids are also atlas region names. `units.atlas` holds base sprites; the
  per-color atlases are generated from it by tinting, so you only edit `units.atlas`.
- `findUnitTextures` falls back to a generic "unit" region if a texture is missing, so a
  mistyped id shows wrong art instead of an error.
- Renaming an id requires updating: the id constant, every code reference, the `.atlas`
  region(s), and any `.ptmap` that places that unit or tile.

## Maps and the editor

- `MapSave` is the serialized map; `MapConverted` turns it into actors and labels.
- Spawn points for players are labels named `player` with `data["id"]` (slot) and later
  `data["player_id"]` (assigned player). `setDefaultPlayerIdToLabels` assigns slot + 1.
- `assignPlayersToSpawnPoints` (called from `ready()`) matches player labels to base tiles.
- Map extras can override some per-map settings, e.g. `EXTRA_CREDITS_PER_BASE`,
  `EXTRA_CREDITS_PER_CLUSTER`, and `game_mode`.
- Seasons swap the tile atlas (`season` -> summer/winter) when a level is set.

## Multiplayer snapshot flow

- Host sends `GameStateSnapshot`; client applies it with `applyGameStateToPlayScreen`, which
  clears the stage and rebuilds tiles and units from their JSON.
- AI player ids are stored in the snapshot and re-created with `BotFactory`.
- Snapshot JSON format is internal to a host/client pair on the same build.

## GUI

- `PlayGUIStage` holds a small state machine (`initialState`, `startTurn`, `myTurn`,
  `nextPlayerPrepare`, `theirTurn`, `endTurn`, `gameOverState`) plus map click listeners
  (`selectUnitCL`, `seeInfoCL`, `unitSelectedCL`, ability-placement listener).
- Action buttons (build base / build / capture / destroy / waypoint) are recomputed by
  `updateUnitActionButtons`, which runs on select, move, command executed, action completed
  and next turn. If you add a new state-change path, call it there too.
- Bots' units are auto-selected for visual feedback when `GamePref.showBotGui` is true.

## Dev tools

- F2 toggles the in-game console; `PlayScreen.PlayCslCommandExc` has commands like `addAI`,
  `removeAI`, `endTurn`, `fow`, `empower`, `win`, `lose`, `addPlayer`.
- `Config.DEBUG_MODE` / `CONSOLE_ENABLED` control debug behavior in non-release builds.
- `Config.WIP_CONTENT` gates unfinished content (mage, rat king, their buildings, research).

## Tooling gotchas

See the "Build and run" section of `AGENTS.md` for the JDK requirement, the daemon/IDE file
lock issue on `core/build`, and where unit tests live.

## Where to change common things

| I want to change... | Look at |
| --- | --- |
| Unit stats/appearance docs | the unit class under `unit/actors/...` |
| Unit cost and unlock requirements | `player/species.kt` |
| Species buildings | `player/species.kt` |
| Global gameplay constants | `Config.kt` |
| Economy formulas | `playscreen/PlayExt.kt`, `PlayTurnCycleListener.kt`, see `ECONOMY.md` |
| A player action | `playscreen/commands/` |
| Turn-start effects | `playscreen/PlayTurnCycleListener.kt` |
| AI behaviour | `bot/v2/` |
| Action buttons / HUD | `playscreen/gui/PlayGUIStage.kt` |
| Terrain movement and bonuses | `unit/TerrainProps.kt` |
| Movement reachability | `playstage/PlayStageEx.kt` |
| Maps / spawn labels | `map/MapLoader.kt`, `map/MapConverted.kt`, the editor |
