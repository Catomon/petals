# Logging and debugging notes

How debug output is organized and how to tune the AI with it.

## Philosophy

- Errors always print. Real failures must never be hidden behind a flag.
- Development chatter is opt in from the launcher, so a normal run stays quiet. A normal run
  should show almost nothing except the save line.
- Two gated categories:
  - `botlog`: AI balance lines for tuning.
  - `verboselog`: engine chatter (asset loading, command added/executed events, turn and round
    changes, GUI listener changes, command logs).
- The flags are independent. Use them only while investigating, and do not use them as the
  permanent logging style for new code.

## Running with logs

Add the flag to the spectate argument. For example, a hard bot vs bot match with balance logs:

    .\gradlew.bat :desktop:run --args="botvsbot hard botlog"

For automation, launching the packaged desktop jar directly with separate arguments is more
reliable than fighting quote escaping through the Gradle batch wrapper, and the process must
be killed when done because the game window blocks. See `gotchas.md`.

## What the balance log gives you

Stable prefixes and key=value pairs, so lines are easy to read and easy to grep:

- match summary: map size, resource node count and total deposit, bases per player, bot
  factions, difficulties and starting state. Emitted once, from the first bot turn.
- per turn: credits, income, bases, free bases, owned crystals, unit count, tech reserve,
  total spent, and how full each role slot is, plus the current unit list.
- each buy: unit, price, credits left, role, slot before and after, value score, the runners up
  it beat, and the base position.
- each build and base: cost, credits left, position, and which units the building unlocks.
- cheat payouts, idle turns, and a per turn command count. It counts commands actually issued,
  and approach moves are reported as moves, so "capture" means a capture command, not walking
  towards a crystal.

## How to read it for tuning

- Are role slots filling, and in a sensible order?
- Do upgrades appear once income grows, or does the bot only ever buy entry level units?
- Is it sitting on the tech reserve forever, or does tech actually get built?
- Is expansion deliberate, and does it stop at a sensible number of bases?
- Are buys, captures, builds and attacks all happening, or is one category missing?

## Caveats

- Normal logs go to stdout, errors go to stderr. A run that "does nothing" may still be
  printing stack traces on stderr.
- The match summary is logged once per match from a bot turn start. A level with no bots will
  not log it.
- Gated lines stay silent until the flag is passed, so an empty log is expected without it.
