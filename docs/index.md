# Session docs index

These notes are split by topic on purpose. Read only the file that matches what you are about
to do, then go to the code for exact values and type names. The older top level docs
(`../AGENTS.md`, `../PROJECT_NOTES.md`, `../ECONOMY.md`, `../FLAWS.md`) still hold the exact
numbers, current wiring and known debt.

## What to read when

| If you are about to... | Read |
| --- | --- |
| Change AI behavior, buying, building or difficulty | `ai-design.md` |
| Touch income, costs, bases or crystals | `economy-design.md` |
| Change units, factions, armor or content gating | `game-design.md` |
| Add debug output, or tune the AI with spectate logs | `logging-and-debugging.md` |
| Hit something weird and want to save time | `gotchas.md` |

## Working style (learned in this project's sessions)

- Prefer design discussion first, then an explicit agreed list of changes, then code. The
  owner often replies in rounds of "here is what I think, list what we agreed".
- Do not change balance or economy rules on your own initiative. Report findings with
  evidence (logs, before and after numbers, a spectate run) and let the owner decide. They are
  comfortable with the agent proposing, but the call is theirs.
- Verify AI changes with a bot vs bot spectate and the balance log, watching for exceptions and
  a healthy mix of command types.
- Avoid em dashes in code comments and player facing text.
- Tests are optional and the owner is not familiar with them. Add them only when they clearly
  pay off, and keep them small.
- Unit and tile ids double as sprite region names and map keys, so a rename touches code,
  atlases and map files. See `../AGENTS.md` for the checklist.
- New docs should describe intent, not exact values or type names, so they do not go stale.
