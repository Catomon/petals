package ctmn.petals.playscreen.events

import ctmn.petals.unit.UnitActor

class UnitDiedEvent(val unit: UnitActor, val killer: UnitActor? = null) : PlayStageEvent()