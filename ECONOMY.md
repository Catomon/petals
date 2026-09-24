# Economy

How credits work in Fae's Feisty as of now. Numbers are the current defaults and may be
overridden per map or per campaign level.

## Player state

Each `Player` has:

- `credits` - spendable gold.
- `creditsPassiveReserve` - the finite starting buffer (defaults to `Config.PLAYER_CREDITS_RESERVE`).
- `income(playScreen)` - a computed snapshot of this player's income this turn (see below).
- `incomeReserve(playScreen)` - remaining passive reserve plus the amount left in owned crystals.

## Income sources

A player earns credits from three places. Income is recalculated at the start of each of the
player's turns.

### 1. Passive income (finite buffer)

While `creditsPassiveReserve > 0`:

- `creditsPassiveReserve -= creditsPassiveIncome`
- `credits += creditsPassiveIncome` (the last step only gives what was actually left)

So the buffer pays out a fixed amount per turn until it is empty. Defaults:
`creditsPassiveIncome = 100`, reserve = `Config.PLAYER_CREDITS_RESERVE`.

### 2. Base income (recurring, sustainable)

Every owned base tile pays a fixed amount per turn:

```
income += PlayScreen.creditsIncomePerBase   // 50 by default
```

This does not run out, and it scales with how many bases you own. This is the main source
that lets matches reach the late game.

### 3. Crystal income (finite node, contested)

Every owned crystal tile pays out each turn and drains its own deposit:

```
perCluster = calcCreditsPerCluster(player)
amountLeft -= perCluster
credits += perCluster        // the last step only gives what was actually left
```

When a crystal's `amountLeft` reaches 0 it is removed from the map.

`calcCreditsPerCluster` grows a little with map control (extra bases beyond the first):

```
calcCreditsPerCluster(player) = creditsPerCluster + max(0, ownedBases - 1) * 25
```

Defaults: `creditsPerCluster = 100`, `Config.CRYSTALS_CLUSTER = 2500`. So one fresh crystal
lasts roughly 25 turns at 100 per turn.

## When income is applied

- **Turn start:** a turn-cycle listener pays passive income, then base income and crystal
  income for the player whose turn is starting. Crystal deposits tick down here.
- **Level start:** the first player receives `creditsPerBase` for every capturable tile they
  start with (this is a one-time starting bonus, not recurring).

`Player.income` is only a preview used by the UI and the AI; the actual credits are added by
the turn-cycle listener using the same sources.

## Where you see it

The play HUD shows `credits (+income/reserve)`, where `income` is `Player.income` and
`reserve` is `Player.incomeReserve`.

## Spending

- Buying units: the unit's shop price (per species).
- Building structures: each building's cost.
- Building a new base: `Config.BASE_BUILD_COST`.

## Crystal nodes, short version

- A crystal tile holds a finite deposit (`CrystalsComponent.amount`, default `CRYSTALS_CLUSTER`).
- Only workers can capture it; capturing flips ownership but keeps the remaining deposit.
- The owner collects `calcCreditsPerCluster` per turn; the deposit drains while owned.
- At 0 it disappears. It can be recaptured by the other player before that.
- Crystals cannot be targeted by the destroy command, but they do have tile HP and can be
  destroyed indirectly by the environment damage from attacks.

## Current defaults

| Value | Where | Default |
| --- | --- | --- |
| Passive income per turn | `PlayScreen.creditsPassiveIncome` | 100 |
| Passive reserve | `Config.PLAYER_CREDITS_RESERVE` | 2500 |
| Recurring income per base | `PlayScreen.creditsIncomePerBase` | 50 |
| Crystal payout per turn | `PlayScreen.creditsPerCluster` | 100 |
| Extra crystal payout per extra base | `calcCreditsPerCluster` | +25 |
| Crystal deposit per tile | `Config.CRYSTALS_CLUSTER` | 2500 |
| One-time start bonus per owned tile | `PlayScreen.creditsPerBase` | 100 |
| Base build cost | `Config.BASE_BUILD_COST` | 200 |

## Map and campaign overrides

- Maps can override `creditsPerBase` and `creditsPerCluster` through map extras
  (`EXTRA_CREDITS_PER_BASE`, `EXTRA_CREDITS_PER_CLUSTER`), read in `PlayScreenTemplate`.
- `creditsIncomePerBase` is not yet exposed as a map extra.
- Campaign levels can set a player's `creditsPassiveReserve` (for example some levels set it
  to 0 so only crystals and bases matter), or set `credits` directly.

## Notes

- `PlayScreen.creditsPassiveIncomeMax` exists but does not appear to be used.
- `Config.GOLD_PER_BASE` exists but does not appear to be used; base income uses
  `creditsIncomePerBase` instead.
- Only bases and crystals generate income. Units never generate credits.
