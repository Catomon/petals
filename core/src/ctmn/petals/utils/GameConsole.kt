package ctmn.petals.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.kotcrab.vis.ui.VisUI
import com.strongjoshua.console.CommandExecutor
import com.strongjoshua.console.GUIConsole

object GameConsole {

    var visibilitySwitchKey = Input.Keys.APOSTROPHE

    var commandExecutor: CommandExecutor? = CslCommandEx()
        set(value) {
            field = value

            mConsole?.setCommandExecutor(value)
        }

    var inputProcessorReturnTo: InputProcessor? = null

    val console get() = if (mConsole == null) createConsole() else mConsole!!

    val isVisible get() = mConsole?.isVisible == true

    @Suppress("GDXKotlinStaticResource")
    private var mConsole: GUIConsole? = null

    fun createConsole(): GUIConsole {
        mConsole?.let { switchVisibility() }
        if (mConsole?.isVisible == true) switchVisibility()

        giveBackInput()

        mConsole = GUIConsole(VisUI.getSkin(), true).apply {
            isDisabled = true
            //displayKeyID = Input.Keys.TAB // is taken by console autocomplete
            setCommandExecutor(commandExecutor)
            window.background = null
            window.titleLabel.setText("")
        }

        return mConsole!!
    }

    fun onWindowResize() {
        createConsole()
    }

    private fun borrowInput() {
        if (Gdx.input.inputProcessor is InputMultiplexer
            && !(Gdx.input.inputProcessor as InputMultiplexer).processors.contains(console.inputProcessor)
        )
            console.resetInputProcessing()
    }

    private fun giveBackInput() {
        //Gdx.input.inputProcessor = inputProcessorReturnTo
    }

    fun dispose() {
        if (mConsole?.isVisible == true)
            switchVisibility()

        giveBackInput()

        mConsole?.dispose()
    }

    fun switchVisibility(): Boolean {
        if (mConsole == null) return false

        console.isVisible = !console.isVisible
        console.isDisabled = !console.isDisabled

        if (console.isVisible) {
            console.setPosition(0, 0)
            console.setSizePercent(100f, 50f)

            borrowInput()
        } else {
            giveBackInput()
        }

        return console.isVisible
    }

    val displayKeyInputProcessor by lazy {
        object : InputProcessor {
            override fun keyDown(keycode: Int): Boolean {
                if (keycode == visibilitySwitchKey) {
                    switchVisibility()
                }

                return true
            }

            override fun keyUp(keycode: Int): Boolean {
                return false
            }

            override fun keyTyped(character: Char): Boolean {
                return false
            }

            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                return false
            }

            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                return false
            }

            override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                return false
            }

            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                return false
            }

            override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
                return false
            }

            override fun scrolled(amountX: Float, amountY: Float): Boolean {
                return false
            }
        }
    }

    val displayKeyEventListener by lazy {
        object : InputListener() {
            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == visibilitySwitchKey) {
                    switchVisibility()
                }

                return false
            }
        }
    }
}

class CslCommandEx : CommandExecutor() {
    fun test() {

    }
}