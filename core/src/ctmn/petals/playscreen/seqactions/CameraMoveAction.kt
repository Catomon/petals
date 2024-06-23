package ctmn.petals.playscreen.seqactions

import ctmn.petals.playscreen.PlayScreen
import com.badlogic.gdx.graphics.OrthographicCamera

class CameraMoveAction(
    private val moveX: Float,
    private val moveY: Float,
    private var speed: Float = 200f,
) : SeqAction() {

    private var lastX = 0f
    private var lastY = 0f

    private var xDone = false
    private var yDone = false

    private lateinit var camera: OrthographicCamera

    private var stopRange: Float = 64f

    override fun update(deltaTime: Float) {
        if (lastX != camera.position.x)
            xDone = true
        if (lastY != camera.position.y)
            yDone = true

        if (xDone && yDone)
            isDone = true

        if (!isDone) {
            when {
                camera.position.x < moveX - stopRange && !xDone -> camera.position.x += speed * deltaTime * 2f
                camera.position.x > moveX + stopRange && !xDone -> camera.position.x -= speed * deltaTime * 2f
                camera.position.y < moveY - stopRange && !yDone -> camera.position.y += speed * deltaTime * 2f
                camera.position.y > moveY + stopRange && !yDone -> camera.position.y -= speed * deltaTime * 2f
                else -> isDone = true
            }

            lastX = camera.position.x
            lastY = camera.position.y

            playScreen.playStageCameraController.limitPosition()
        }

        if (isDone) playScreen.playStageCameraController.lock = false
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        camera = playScreen.playStage.camera as OrthographicCamera

        lastX = camera.position.x
        lastY = camera.position.y

        stopRange *= (playScreen.playStage.camera as OrthographicCamera).zoom

        playScreen.playStageCameraController.lock = true

        return true
    }
}
