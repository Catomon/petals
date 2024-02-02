package ctmn.petals.playscreen.gui

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import kotlin.math.abs

class InputManager(val guiStage: PlayGUIStage) {

    val playStage = guiStage.playStage
    val playScreen = guiStage.playScreen

    var buttons = 0

    // scrolling with touch down
    private var scrollingPointer = -1
    private var startX = 0f
    private var startY = 0f
    private val threshold = 10f
    private var currentThreshold = 0f
    private var isScrollingMap = false
    private var isScrollingWithKeyboard = false

    val playScreenInputProcessor = PlayScreenInputProcessor()

    init {
        playScreen.inputMultiplexer.addProcessor(GestureDetector(object : GestureDetector.GestureListener {
            override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
                return false
            }

            override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean {
                return false
            }

            override fun longPress(x: Float, y: Float): Boolean {
                return false
            }

            override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean {
                return false
            }

            override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
                if (Gdx.app.type == Application.ApplicationType.Android) {
                    //if (buttons == 1)
                        //playStage.camera.translate(-deltaX / 2, deltaY / 2, 0f)
                }

                return false
            }

            override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean {
                return false
            }

            override fun zoom(initialDistance: Float, distance: Float): Boolean {
                val amount = distance - initialDistance
                playScreen.playStageCameraController.zoomUp(-amount / 20000)

                return false
            }

            override fun pinch(
                initialPointer1: Vector2?,
                initialPointer2: Vector2?,
                pointer1: Vector2?,
                pointer2: Vector2?
            ): Boolean {
                return false
            }

            override fun pinchStop() {

            }
        }))

        playStage.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                return true
            }

            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                if (buttons == 0 && !isScrollingMap)
                    guiStage.onMapClicked(x, y)

                isScrollingMap = false

                if (guiStage.currentState == guiStage.nextPlayerPrepare)
                    guiStage.currentState = guiStage.startTurn
            }
        })

        playScreen.inputMultiplexer.addProcessor(playScreenInputProcessor)
    }

    inner class PlayScreenInputProcessor : InputProcessor {

        var isStopped = false

        var wasdKeysUp = 0

        override fun keyDown(keycode: Int): Boolean {
            if (isStopped) return false

            when (keycode) {
                Input.Keys.W -> { playScreen.playStageCameraController.up = true; wasdKeysUp++ }
                Input.Keys.S -> { playScreen.playStageCameraController.down = true; wasdKeysUp++ }
                Input.Keys.A -> { playScreen.playStageCameraController.left = true; wasdKeysUp++ }
                Input.Keys.D -> { playScreen.playStageCameraController.right = true; wasdKeysUp++ }
            }

            if (wasdKeysUp > 0) isScrollingWithKeyboard = true

            return false
        }

        override fun keyUp(keycode: Int): Boolean {
            if (isStopped) return false

            when (keycode) {
                Input.Keys.W -> { playScreen.playStageCameraController.up = false; wasdKeysUp-- }
                Input.Keys.S -> { playScreen.playStageCameraController.down = false; wasdKeysUp-- }
                Input.Keys.A -> { playScreen.playStageCameraController.left = false; wasdKeysUp-- }
                Input.Keys.D -> { playScreen.playStageCameraController.right = false; wasdKeysUp-- }

                Input.Keys.SPACE -> {
                    if (guiStage.currentState == guiStage.nextPlayerPrepare)
                        guiStage.currentState = guiStage.startTurn
                }
            }

            if (wasdKeysUp == 0) isScrollingWithKeyboard = false

            return false
        }

        override fun keyTyped(character: Char): Boolean {
            return false
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (isStopped) return false

            val touchPos = playStage.screenToStageCoordinates(Vector2(screenX.toFloat(), screenY.toFloat()))

            buttons++

            if (buttons == 1) {
                if (!isScrollingMap) {
                    currentThreshold = 0f
                }

                scrollingPointer = pointer

                startX = screenX.toFloat()
                startY = screenY.toFloat()
            } else {
                currentThreshold = 0f
                scrollingPointer = -1
            }

            return false
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (isStopped) return false

            val touchPos = playStage.screenToStageCoordinates(Vector2(screenX.toFloat(), screenY.toFloat()))

            buttons--

            return false
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (isStopped) return false

            val touchPos = playStage.screenToStageCoordinates(Vector2(screenX.toFloat(), screenY.toFloat()))

            val deltaX = startX - screenX.toFloat()
            val deltaY = screenY.toFloat() - startY

            if (buttons == 1 && scrollingPointer == pointer) {
                currentThreshold += abs(startX - screenX.toFloat()) + abs(startY - screenY.toFloat())

                if (currentThreshold > threshold) {
                    playStage.camera.translate(deltaX / 2, deltaY / 2, 0f)

                    isScrollingMap = true
                }

                startX = screenX.toFloat()
                startY = screenY.toFloat()
            }

            return false
        }

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            if (isStopped) return false

            if (buttons == 0) {
                val scrollOff = 10

                when {
                    screenX > Gdx.graphics.width - scrollOff -> playScreen.playStageCameraController.right = true
                    screenX < scrollOff -> playScreen.playStageCameraController.left = true
                    else -> {
                        if (!isScrollingWithKeyboard) {
                            playScreen.playStageCameraController.right = false
                            playScreen.playStageCameraController.left = false
                        }
                    }
                }

                when {
                    screenY > Gdx.graphics.height - scrollOff -> playScreen.playStageCameraController.down = true
                    screenY < scrollOff -> playScreen.playStageCameraController.up = true
                    else -> {
                        if (!isScrollingWithKeyboard) {
                            playScreen.playStageCameraController.down = false
                            playScreen.playStageCameraController.up = false
                        }
                    }
                }
            }

            return false
        }

        override fun scrolled(amount: Int): Boolean {
            if (isStopped) return false

            playScreen.playStageCameraController.zoomUp(amount * 0.05f)

            return false
        }
    }
}