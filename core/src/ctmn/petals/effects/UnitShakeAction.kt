package ctmn.petals.effects

import ctmn.petals.unit.sprite
import ctmn.petals.unit.UnitActor
import ctmn.petals.utils.Rumble
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction

class UnitShakeAction(private val power: Float, duration: Float) : TemporalAction(duration) {

    private val rumble = Rumble()

    private val startPos = Vector2()

    override fun begin() {
        super.begin()

        if (target is UnitActor) {
            val unitActor = target!! as UnitActor
            startPos.set(unitActor.sprite!!.x, unitActor.sprite!!.y)
        }

        rumble.rumble(power, duration)
    }

    override fun update(percent: Float) {
        rumble.update(Gdx.graphics.deltaTime)
        if (target is UnitActor) {
            val unitActor = target!! as UnitActor
            unitActor.sprite!!.setPosition(startPos.x + rumble.pos.x, startPos.y + rumble.pos.y)
        }
    }
}
