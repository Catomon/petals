package ctmn.petals.actors.actions

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import ctmn.petals.utils.Rumble
import ctmn.petals.utils.setPosition

class CameraShakeAction(val power: Float = 3f, duration: Float = 0.5f) : TemporalAction(duration) {

    private val rumble = Rumble()

    override fun begin() {
        super.begin()

        rumble.rumble(power, duration)
    }

    override fun update(percent: Float) {
        rumble.update(Gdx.graphics.deltaTime)
        if (target.stage != null) {
            val camera = target.stage.camera
            camera.setPosition(camera.position.x + rumble.pos.x, camera.position.y + rumble.pos.y)
        }
    }
}
