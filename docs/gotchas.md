# Gotchas

Things that are not obvious and cost time once. Short list, add to it when you lose an hour.

## libGDX arrays cannot be iterated nested

The libGDX array type reuses one iterator instance. Iterating the same array inside another
iteration of itself throws at runtime ("iterator cannot be used nested"). It is easy to hit
when a helper called from a filter or map loop walks the same collection again. Materialize to
a Kotlin list first, then nest over that.

Symptom to remember: a helper throws, something swallows the exception, and the game looks like
it simply does nothing. This exact bug once made both bots stand still for entire matches.

## Swallowed exceptions make bots look broken

Bot action evaluation goes through a catch-all so one bad action does not stop a turn. That
also hides real bugs. Errors are now logged, but if the AI appears idle, check stderr before
assuming the game logic is at fault. An idle bot usually means every candidate action scored
zero or threw.

## Two output streams

Normal libGDX logs go to stdout with a bracketed tag, errors go to stderr. When verifying a
spectate run, always check both, and check that a run without debug flags is actually quiet.

## Running and stopping a spectate from automation

The desktop app opens a window and blocks until closed. Passing a multi word argument list
through the Gradle batch wrapper on Windows is fragile; building the packaged desktop jar and
launching it with a plain java command plus separate arguments is more reliable. Remember to
terminate the process afterwards. The jar name includes the version, and the launcher expects
the spectate mode and difficulty as separate arguments.

## AI evaluation and the game random

Do not estimate damage by rolling the game's random source. It consumes the shared sequence
during "thinking", which breaks determinism expectations and can disturb multiplayer. Use
deterministic math for estimates and an AI local random for anything that truly needs a roll.

## Preview versus payout

The HUD income preview and the turn cycle payout are separate code paths. If they disagree,
one of them is wrong. The economy went through exactly this: a base income was counted in the
preview but never paid, and only cross checking credits across turns exposed it.

## Unit stat experiments

A parallel static stat table experiment once existed for a couple of units and was misleading;
it has been removed. Define unit stats in the unit types directly. If you ever want shared stat
data, build one source of truth and wire everything to it. Do not leave two.

## Id renames must reach the art sources

Unit and tile ids are atlas region names, and the regions come from source image file names
under the asset texture folders. Renaming an id in code is not enough:

1. rename the source images (units, and both the summer and the winter tile folders),
2. run the texture packer,
3. delete the generated colored unit textures and run the desktop game through Gradle so the
   loader recreates them. Running the packaged jar can skip creation because its embedded
   copies still satisfy the existence check.

Two behaviors to remember: a missing unit region falls back to a placeholder sprite and only
logs, while a missing building region throws inside the buy menu and kills the game. That is
why a code rename can look fine for units and still crash when a base is selected.

## Silence is not success

Missing sprite lookups fall back to a generic sprite and only log. A rename mistake looks
"fine" until someone notices wrong art. Renaming ids touches code, atlases and map files. See
`../AGENTS.md` for the checklist.

## Build and test tooling

- Tests live outside the main source set and run through the core test task. Only a
  serialization test exists, so do not assume gameplay coverage.
- The Gradle version in the wrapper does not support very new JDKs. If a build complains about
  class file versions, point the build at a compatible JDK for that command (see
  `../AGENTS.md`).
- A leftover daemon or IDE language server can lock build outputs. Stop daemons or build with
  no daemon if deleting a build directory fails.
