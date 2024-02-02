package ctmn.petals.effects

import ctmn.petals.unit.sprite
import ctmn.petals.unit.UnitActor

class DeadUnitBodyEffect(unit: UnitActor, lifeTime: Float) : ctmn.petals.effects.EffectActor() {
    init {
        x = unit.x
        y = unit.y
        sprite = unit.sprite!!
        this.lifeTime = lifeTime
    }
}
