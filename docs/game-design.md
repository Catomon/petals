# Game design notes

Design intent this project keeps coming back to. Exact values and type names live in code and
in `../ECONOMY.md`; this file is about the reasoning behind them.

## Core loop

Turn based tactics in the Advance Wars spirit, with base building and an economy on top.
Units are bought at owned bases. Workers capture resource nodes and construct buildings, and
buildings unlock better units. Matches are meant to be decided by tempo and map control, not
by one dominant strategy.

## Economy intent

- The real economy is finite and contested. Resource nodes pay while their deposit lasts, and
  a finite starting buffer keeps the opening turns moving. Both run out eventually.
- Bases are production slots, vision and forward positions. They are deliberately not an
  income source. Paying per base, or scaling node payout with base count, makes base spam the
  best move in every game.
- The owner treats "spam one thing" strategies as a design smell. When adding a feature, ask
  what prevents the degenerate mass strategy.
- Long games getting poorer late is acceptable for now. If matches need a longer money tail,
  the knobs are node deposit size, the starting buffer, or a small capped base stipend. None of
  those were chosen yet; playtest first.

## Combat intent

- Armor is flat subtraction on purpose. Buying cheap swarm units into armor should be a bad
  decision, not merely a slightly inefficient one. Do not soften this without asking.
- The planned counterplay to armor is specific units and abilities (anti armor tools, the
  work in progress caster content), not a change to the damage formula.
- Combat is swingy by design: damage is rolled in a range, with small miss and crit chances.
  There is no attack back, so ranged pieces are safe when positioned well.
- Matchups, terrain and auras matter more than raw stats. That is where faction identity lives.

## Faction intent

- Basic line units are mirrored between factions so neither side starts with a free edge.
  Differences belong in the mid and elite tiers, special mechanics and terrain profiles.
- Some asymmetry is fine when it is deliberate and each side gets a comparable answer.
- A couple of weapon units (the mace and hammer style ones) were still being designed at the
  time of writing. Their identity is intentionally open; do not "fix" them into mirrors.

## Movement and targeting axes

Three independent things, easy to conflate:

1. Unit type: ground, water or air. Air can only be hit by attacks that can reach air.
2. Movement profile: what terrain the unit crosses (some ground units use a flier profile).
3. Attack capability: whether the attack can hit air targets.

Consequences seen in practice: a flying ground unit repositions like a flyer but is still
hittable by ground attacks, and a genuinely airborne bomber can be unable to hit other fliers.
Any AI or targeting code must treat these as separate axes.

## Difficulty intent

- Easy, Normal and Hard play by the same rules with the same planner. Difficulty changes
  decision quality (how often the AI misjudges and plays a runner up action), how big an army
  and economy it aims for, and how long it pushes at low health.
- Difficulty must not be expressed as "Easy is only allowed to want cheap units". If the AI
  looks too strong, weaken its decisions, not its menu.
- A separate top tier may cheat explicitly (extra income), and that is reserved for that tier
  only. Hard stays honest.

## Progression intent

- Buildings unlock units. The tech tree is still being shaped, so treat it as in progress.
- Work in progress content (casters, research) is gated behind a flag and should not leak into
  normal play.
