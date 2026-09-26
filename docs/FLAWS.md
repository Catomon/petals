# Flaws and Technical Debt

A prioritized list of things that are painful, risky or inconsistent in the codebase.
Severity is about how much it hurts to *not* fix it, not how big the fix is.

Legend:

- **P0** - correctness/data-loss/crash risk. Should fix before relying on it.
- **P1** - high: blocks or badly slows down real work.
- **P2** - medium: works, but causes friction and repeated bugs.
- **P3** - low: style/nice-to-have.

## P0 - correctness and data integrity

1. **Component serialization relies on hand-maintained allow/ignore lists.**
   The same set of components is described in several unrelated places that must be kept in
   sync by hand:
   - `UnitActor.toJsonObject` has a blacklist (`ViewComponent`, `MatchUpBonusComponent`,
     `TerrainPropComponent`, `ShopComponent`).
   - `Components.classes` is a separate allow list for loading.
   - `TileActor.fromJsonObject` and `TileActor.makeCopy` use explicit `when` branches per
     component class.
   Adding a new component and forgetting one of these lists silently drops it from saves
   (or crashes on load). There is already a `CopyableComponent` marker interface, but it is
   used inconsistently.
   *Direction:* introduce one explicit marker, e.g. `SyncedComponent` / `PersistedComponent`,
   and have save/load/copy driven by it (plus a class registry) instead of manual lists.

2. **No automated tests.**
   Gameplay, combat math, turn cycle and the AI are only verified by manually running the
   game. Regressions in damage, income, capture or save/load are easy to introduce and hard
   to notice.
   *Direction:* start small: pure-function tests for combat/economy math, plus a headless
   round-trip test for save/load of a unit and a tile.

## P1 - high impact

3. **Global mutable singletons.**
   `assets` is a lateinit global, `game` is fetched from `Gdx.app`, `GamePref`, `strings`
   and `GameConsole` are global objects. This creates init-order coupling, makes unit tests
   impossible, and hides dependencies.
   *Direction:* pass assets/screen dependencies explicitly where practical; keep the global
   only as a thin entry point.

4. **AI evaluation runs on the main thread.**
   Bot turns are computed during `PlayScreen.update`, so a full evaluation can hitch on big
   maps. The RNG half of this is fixed: `CombatEval` now uses deterministic average damage
   from `calculateDmgDefStats`, and `AiBot` has its own seeded RNG, so thinking no longer
   consumes `playScreen.random`.
   *Direction:* move evaluation off the main thread, or cache movement/eval results per turn.

5. **String ids are the only link between code, sprites and maps.**
   Unit/tile ids index atlas regions and `.ptmap` entries. Renaming or adding an id requires
   editing code, `.atlas` files and map files by hand; forgetting the atlas or a map silently
   produces missing sprites, not compile errors. Already bit us during the `doll_` -> `fairy_`
   rename.
   *Direction:* keep ids stable, add a validation pass at startup that every registered unit
   has a sprite and every map id resolves, and log loudly instead of silently.

6. **`PlayScreen` is a god class.**
   It mixes input handling, GUI init, turn/game-over flow, multiplayer branches, command
   console, fog, camera setup and more. It is one of the largest and most fragile files.
   *Direction:* extract responsibilities (level setup, game-over flow, console, input) into
   collaborators over time.

7. **Two sources of truth for building names.**
   Building id strings are declared both in `tile/Tiles.kt` and in `player/species.kt`
   (e.g. the same "fairy_*"/"goblin_*" building names). They can drift and both must be
   renamed together.
   *Direction:* one shared definition consumed by both.

8. **Component accessors assume components exist.**
   Many extension properties use `!!` (`attackRange` -> `cAttack!!`, `mana` -> `cAbilities!!`,
   etc.). Calling them on a unit missing that component crashes instead of returning a safe
   default. Combined with the extension-heavy design, this is easy to trip.
   *Direction:* make accessors nullable or defaulted, or validate component sets at spawn.

## P2 - medium friction

9. **`GameActor` wraps an Ashley `Entity` instead of being one.**
   `GameActor` holds `val components = Entity()` and re-implements `add`/`get`/`del`/`has`.
   It is an extra indirection layer, and the code implicitly relies on Ashley's replace
   semantics when adding two components of the same class (we had to decompile to confirm).
   *Direction:* extend `Entity` (or delegate via an interface) and stop hand-reimplementing
   the container. Lower priority than the serialization issue above.

10. **`PlayScreen.random` getter has a side effect.**
    Reading `random` increments `randomCount`. A property accessor with a side effect is
    surprising and easy to misuse.
    *Direction:* make the count explicit (e.g. an RNG wrapper with a `next()` method).

11. **Turn manager mutates its player list in the `currentPlayer` setter and rotates in place.**
    Reordering `players` to set the current player is surprising and makes reasoning about
    indices (turn/previous/next) fragile.
    *Direction:* keep a stable player order and store a separate current index.

12. **Bespoke movement-grid algorithm.**
    `getMovementGrid` is a custom flood with hardcoded four directions and `try/catch`
    around array bounds, recomputed frequently (and again inside `canMove`). There is no A*;
    longer paths are greedy. It is both a perf and a correctness risk on odd maps.
    *Direction:* replace with a clear, tested pathfinder producing reachable set and paths.

13. **Save/preference handling.**
    Player save data is encrypted with a hardcoded secret and printed decrypted to stdout on
    startup; old save versions only produce a warning. This is fragile and leaks data to logs.
    *Direction:* proper versioned migration and stop logging decrypted saves.

14. **Level/scenario duplication.**
    Dozens of story levels repeat near-identical AI setup and label/spawn hacks, and several
    hardcode unit-id checks. A change to the AI wiring or id scheme must touch many files.
    *Direction:* central helper for "make an AI player", and one place mapping spawn labels.

15. **Two overlapping stat-definition styles.**
    Some units define stats literally in the constructor, others via the experimental
    `UnitStat` objects (`FairyWaterplantStat`, `FairySlinger.Stat`). There is no single
    convention, and the `UnitStat` path is partly unused.
    *Direction:* pick one (prefer code-defined with a validation pass), delete the other.

16. **Registries can drift from species/asset setup.**
    `Units`, `Abilities`, `Species.*.units`, building lists and atlas regions are maintained
    separately. Adding a unit means editing several of them.
    *Direction:* generate or validate the registries from one source.

17. **Exception-driven control flow and `TODO()` in runtime paths.**
    e.g. `Species.getSpeciesUnits` / `getSpeciesBuildings` throw for unknown species, so
    callers wrap them in `runCatching`. This hides real mistakes.
    *Direction:* return an explicit "unsupported" result or model species as data.

18. **Command failure handling is quiet.**
    `CommandManager` logs and returns when a command `canExecute` fails or `execute` returns
    false; many commands swallow failures. This makes gameplay bugs hard to trace.
    *Direction:* surface failures in a debug overlay/console and return structured results.

19. **Multiplayer state sync is manual and divergent.**
    Host/client play screens and snapshots are hand-synchronized, with AI players tracked by
    id lists. Drift between host and client is hard to detect.
    *Direction:* centralize authoritative state and serialize the same model the game uses.

20. **Magic values and sprite-size hacks.**
    Tile sprite sizes are special-cased with literals (24/48/64/128), and gameplay timing
    uses scattered constants. Hard to tune consistently.
    *Direction:* move to named constants/config, ideally one place.

## P3 - low / nice to have

21. **Extension-function sprawl.**
    Most unit/tile behavior lives in extension functions rather than on the classes. This is
    mostly a style/discoverability issue (your guess is right that it is not too bad), but it
    encourages the `!!` accessors above.
    *Direction:* keep pure helpers as extensions, move core behavior onto the classes.

22. **Naming leftovers and typos.**
    e.g. `SW0RDMAN`, `GoblinPike` whose id is `goblin_spear`, unused `Goblin`/`GoblinLeader`
    actors, commented-out code blocks, the deprecated `DevScreen`.
    *Direction:* tidy incrementally when touching those files.

23. **`Config` mixes unrelated concerns.**
    App/meta config (app name, version, release flags, UI scale) and gameplay tuning share
    one object, making it hard to find or override balance values.
    *Direction:* split into `AppConfig` and `GameConfig`.

24. **`Player` is a data class with mutable identity fields.**
    `var id`, `var teamId`, `var name` mean `equals`/`hashCode` change over the object's life
    while it lives in lists/maps/sets. Subtle bug source.
    *Direction:* make identity immutable, or stop using value equality for players.

25. **Inconsistent logging.**
    A mix of `Gdx.app.log`, `Gdx.app.error`, `logErr`, `println` and `Logger`. `GameLog` now
    offers always-on errors plus gated `balance` and `verbose` categories, and the noisiest
    call sites (assets, event logger, turn manager, GUI listener changes, bot actions) go
    through it. Many other call sites still use `logMsg`/`Gdx.app.*` directly.
    *Direction:* migrate the remaining call sites to `GameLog`, one area at a time.

26. **Silent asset fallbacks.**
    `findUnitTextures` falls back to a generic "unit" sprite when a unit texture is missing,
    so a rename mistake looks "fine" until you notice the wrong art.
    *Direction:* log at ERROR and/or show a debug placeholder that is obvious.

27. **`TileActor.isOccupied` casts `stage` to `PlayStage`.**
    Breaks if a tile is ever attached to a non-play stage.
    *Direction:* null-safe lookup through a helper.

## Already addressed recently

Kept here for context so they are not "rediscovered":

- `isCapturable` used to conflate "capturable" and "ownable"; bases are now destroy-only and
  `isOwnable` was added for ownership logic.
- `canBuildBase(tile)` had an operator-precedence bug (any unit on water could build).
- The old ad-hoc bots were replaced by the configurable bot in `bot/v2`, with runtime
  economy detection for no-building maps.
- Experimental content is now gated behind `Config.WIP_CONTENT`.

## Suggested order if you only do a few

1. Component serialization marker + registry (P0.1) and a save/load test (P0.2).
2. AI RNG isolation (P1.4) since it affects determinism and multiplayer.
3. Startup validation of ids against sprites and maps (P1.5).
4. Extract a piece or two out of `PlayScreen` (P1.6) as you touch it.
5. Move at your own pace on the P2/P3 items above.
