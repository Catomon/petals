# AGENTS.md

General notes for anyone (human or AI) starting a new session on this repository.

## What this is

Fae's Feisty is a turn based tactics game (Advance Wars style) with economy and base
building. Built with libGDX in Kotlin, targeting desktop and Android. Features include
terrain and matchup bonuses, abilities, building and destroying, fog of war, hot seat and
local multiplayer, a map editor and a story campaign.

## Project docs

- `docs/PROJECT_NOTES.md` - practical notes: command pipeline, events, action points, terrain,
  combat, serialization, bots, maps, GUI, dev tools and tooling gotchas.
- `docs/ECONOMY.md` - how credits and income work right now.
- `docs/FLAWS.md` - prioritized technical debt and suggested fix order.
- `docs/index.md` - short topic notes split by task (AI design, economy design, game design,
  logging, builds, gotchas, working style). Read only the file that matches what you are
  working on instead of reading everything.

## Tech / modules

- `core/` - all game logic and UI (Kotlin, root package `ctmn.petals`).
- `desktop/` - LWJGL desktop launcher (Java).
- `android/` - Android launcher. All game assets live in `android/assets/`.

Gradle wrapper is checked in. Kotlin 1.9, libGDX 1.13, Ashley ECS, VisUI.

## Build and run (Windows, PowerShell)

- Compile core: `.\gradlew.bat :core:compileKotlin`
- Compile desktop: `.\gradlew.bat :desktop:classes`
- Run the game: `.\gradlew.bat :desktop:run`
- Run unit tests: `.\gradlew.bat :core:test` (tests live in `core/test`)
- Build the web bundle: `.\gradlew.bat :web:buildJavaScript` (static output in
  `web/build/dist/webapp`, serve locally with `:web:runWeb`). The web build reaches the menu;
  playing a match still needs the reflection and Gson limitations described in
  `docs/builds-and-release.md` sorted out.

On macOS/Linux use `./gradlew` instead.

Note: Gradle 7.5 does not support very new JDKs. If a build fails with "Unsupported
class file major version", point `JAVA_HOME` at a JDK 17 for that command.

If a build fails with `Could not delete ... core/build/classes/kotlin/main/...`, a leftover
Kotlin daemon or an IDE language server is holding a file lock. Stop the Gradle/Kotlin daemons
and delete `core/build`, or run Gradle with `--no-daemon`.

Unit tests live in `core/test` (not under `src/`, because `src/` is entirely the main source
set) and run with `:core:test`.

The desktop app opens a window and blocks until closed. When running it from automation,
run it in the background or with a timeout if you only need startup logs. A stale
save-version warning on boot is normal and harmless.

## Handy run arguments

- Bot vs bot spectate (two AI players plus a third spectator player, fixed map):
  `.\gradlew.bat :desktop:run --args="botvsbot"`
- Optional difficulty: `--args="botvsbot hard"` or `"botvsbot easy"` / `"botvsbot insane"`
  (default normal).
- Bot balance logging (map resources, per-turn army, every buy with its role slot and the
  scored runners-up): add `botlog`, e.g. `--args="botvsbot hard botlog"`.
- Verbose engine logging (asset loading, event logger, turn manager, GUI listener changes):
  add `verboselog`. Neither flag is needed for normal play or smoke tests.

## Dedicated server

See `docs/DEDICATED_SERVER.md`. Quick commands:

- Standalone server: `.\gradlew.bat :server:runServer --args="--port 7396"`
- In-game headless server: `.\gradlew.bat :desktop:run --args="dedicatedserver"`
- Headless bot match smoke test: `.\gradlew.bat :server:runServer --args="botmatch normal"`
- Fake client load test: `.\gradlew.bat :server:runServer --args="--loadtest 8 --duration 15"`


This is the quickest smoke test for gameplay and for AI changes.

## Architecture in a nutshell

- The active game screen orchestrates the battle: a scene2d `PlayStage`, a turn manager, a
  command manager, a sequence-action manager (animations and effects), a bot manager and
  the play GUI.
- Player intent is expressed as Commands. Each command has a `canExecute` validation and an
  `execute` that mutates game state and queues sequence actions for the visuals.
- Turn start/end effects (income, action point reset, capture and build progress, damage
  over time) are handled by a turn cycle listener rather than in commands.
- Units and tiles are defined as Kotlin classes/code, with sprites and map references keyed
  by string ids. There is no external data file for unit stats.
- Components that take part in serialized game state implement the `SyncedComponent` marker.
  That marker is the single place to opt a component in, instead of manual allow/ignore
  lists. Serialized components are keyed by their fully qualified class name.
- AI lives in `ctmn.petals.bot.v2`: a single configurable bot with an EASY/NORMAL/HARD
  difficulty and a small factory. It scores possible actions and plays through the same
  commands a human uses (buy, build, capture, attack, abilities, destroy, move). It detects
  at runtime whether a map has an economy, so the same bot also works on campaign maps with
  no buildings or resources.

## Game rules worth remembering

- Only worker units can capture tiles and construct buildings.
- Only crystal tiles are capturable. Bases and buildings cannot be captured, only destroyed.
- An "ownable" tile is a crystal or a base; ownership drives income and win conditions.
- The turn cycle resets action points, pays income, and ticks captures/builds/destroys.
- Most balance numbers live in `ctmn.petals.Config` or in the individual unit classes.

## Conventions

- Kotlin, 4-space indentation, package-per-feature under `ctmn.petals`.
- In code comments and player-facing text, avoid the em dash character.
- Unit and tile class docs should describe appearance, role and design intent, but should
  NOT hardcode exact stats, because those go stale when values change.
- New AI work goes in `ctmn.petals.bot.v2`; the older bots were removed.
- Prefer going through existing command classes for any player action.

## Assets and renames (important gotcha)

Sprite regions and map entries reference units/tiles by their string id. Renaming an id
means updating all of:

1. the id constant and every code reference,
2. the region name in the relevant `.atlas` file(s) under `android/assets/`,
3. any `.ptmap` map files that place that unit or tile.

Icon lookups are also keyed by id. Forgetting the atlas or maps silently produces missing
sprites, not compile errors.

## Experimental / work in progress

- `Config.WIP_CONTENT` gates unfinished content (the mage and rat king units, their
  buildings, and the research/tech mechanic). Keep it off unless you are working on those.
- Some stat helpers and systems are experimental; check before assuming they are used.

## Testing tips

- Use the bot vs bot spectate argument as a smoke test and watch the log for exceptions and
  for a healthy mix of buy/build/capture/attack/destroy commands.
- A missing sprite shows up as a "Unit textures not found" log line rather than a crash.
- The bot runs on the main thread and can be expensive if it scans the whole map for every
  unit; keep an eye on frame hitches after AI changes.

## Git

- Check `git status` before committing, the tree sometimes has unrelated in-progress work.
- Keep commit messages short and focused.
