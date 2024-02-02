package ctmn.petals.playscreen.events

import ctmn.petals.unit.UnitActor


class UnitMovedEvent(val unit: UnitActor, val oldX: Int, val oldY: Int) : PlayStageEvent()