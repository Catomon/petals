# Economy design notes

The intent behind credits. Exact defaults live in `ECONOMY.md` and in code.

## Sources

- A finite passive buffer pays each turn until it is empty. It exists to make the opening
  turns playable, not to fund a long game.
- Resource nodes pay a flat amount per turn while their deposit lasts, then disappear. They
  are captured by workers and can change hands.

That is the whole recurring economy. Everything else spends.

## Bases are not income

- Bases are production slots, vision and forward positions. They do not pay credits.
- Two earlier base income ideas were removed because they made base spam the best move:
  a per base recurring payout that was only previewed and never actually paid, and a bonus
  that increased every owned node's payout based on how many bases the owner had.
- If you are tempted to add base income again, first ask what stops mass bases. A capped
  stipend (only the first few bases pay) is the compromise to consider.

## Preview versus payout

- The HUD and the AI read an income preview. The turn cycle is what actually pays credits.
- Those two must agree. A mismatch is a bug and it is invisible unless someone cross checks
  credits from one turn to the next. Balance logs are good at exposing it.
- The same applies to reserves: reserve is a stock (buffer plus remaining node deposits),
  income is a flow.

## Late game

- With both sources finite, the map gets poorer over time. That pushes matches to end, which
  is currently acceptable.
- If that is too harsh, tune in this order: node deposit size, passive buffer size, or a small
  capped base stipend. Decide with a spectate run, not on paper.

## Map and campaign overrides

Some maps and story levels override income, starting credits or the passive buffer. Campaign
levels in particular may zero the buffer so only captured nodes matter. Check the level setup
before assuming defaults.
