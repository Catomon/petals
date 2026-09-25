# AI design notes

How the bot is meant to think. For the current wiring and type names, read the bot package;
this file explains the intent and the traps.

## Principles

- The bot plays through the same commands a human does. If a human cannot do it, the bot
  cannot.
- Evaluation must be deterministic and must never consume the game's random source. Estimate
  damage from average rolls and keep a separate AI local random for tie breaks and mistakes.
  Consuming the shared random during "thinking" breaks replay and multiplayer expectations.
- Failures must be visible. Action evaluation and execution should log real errors instead of
  being silently swallowed. A bot that does nothing looks like a game bug, and it once was
  exactly that.

## Decision flow

1. Refresh a snapshot of the world: own units, enemies, bases, buildings, capturable nodes,
   income, map size, turn.
2. Build candidate actions: attacks, abilities, buys, worker jobs, destruction, then movement.
3. Score them and pick the best. Movement is evaluated last because it is the expensive part.
4. Lower difficulty may replace the best pick with a close runner up. That is the error rate.

## Army composition

- Composition is context driven, not a fixed roster per difficulty. Map size, number of owned
  bases, resource nodes, turn phase, enemy composition, income and fog all shift the targets.
- Roles describe jobs, not unit types: worker, scout, healer, ranged, artillery (long range
  with a dead zone), skirmisher (hit and run or flying ground units), air, water, melee.
- Targets are roster aware. If a faction has no unit for a role, the target is zero instead of
  a slot the bot can never fill.
- A water base and a land base want different units. The decision must use the base being
  filled, not the first free base.
- Watch the enemy: extra artillery against armored enemies, anti air when enemy fliers exist,
  skirmishers against enemy artillery.

## Buying and building

- Buy the best value unit for a role the army still needs, not always the cheapest. Price
  should matter with diminishing weight, otherwise the tech tree never gets used.
- Keep a reserve for the next unlock so the bot saves for tech instead of dumping everything
  into entry level units.
- Have fallbacks: the first worker, and "there is no army yet and hostiles are close".
- Only build tech that unlocks a unit the current plan wants, plus rare defensive structures.
- Expand bases for production and vision capacity, not for payback math.

## Difficulty knobs

- `skill`: decision quality and mistake rate.
- `ambition`: army and economy size.
- `aggression`: how long wounded units keep pushing.
- Easy may also have whole systems disabled (abilities, buildings, expansion).
- Cheating (extra income) is reserved for a separate top difficulty.

## When changing the AI

- Run a bot vs bot spectate with the balance log on (`logging-and-debugging.md`) and look for:
  role slots filling, unit upgrades appearing as income grows, deliberate base expansion, and a
  mix of attacks, captures, builds and destroys.
- Check stderr for evaluation errors. The balance log alone will not show a swallowed
  exception.
- Watch for the array iteration trap in `gotchas.md`.
