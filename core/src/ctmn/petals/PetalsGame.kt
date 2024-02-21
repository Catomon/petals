package ctmn.petals

import com.badlogic.gdx.Application
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Logger
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.screens.DevScreen
import ctmn.petals.screens.LoadingScreen
import ctmn.petals.screens.MenuScreen
import ctmn.petals.utils.GameConsole
import ctmn.petals.utils.setMouseCursor

val game: PetalsGame get() = Gdx.app.applicationListener as PetalsGame

class PetalsGame : Game() {

    val assets = Assets()

    // to change through console
    var debugMode = Const.DEBUG_MODE

    override fun create() {
        if (!Const.IS_RELEASE) Gdx.app.logLevel = Logger.DEBUG

        if (Const.IS_DESKTOP) {
            startDiscordRich()
        }

        GameConsole.consoleDisabled = !Const.CONSOLE_ENABLED

        GamePref.setEmptyToDefaultPrefs()
        GamePref.overridePrefs()

        ShaderProgram.pedantic = false;

        ctmn.petals.assets = assets

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            setMouseCursor()
        }

        assets.loadUI()

        setScreen(LoadingScreen(this))
    }

    override fun render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            when {
                GameConsole.isVisible -> {
                    showHideConsole()
                }
                Const.IS_RELEASE -> setScreen(MenuScreen(this))
                else -> setScreen(DevScreen(this))
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            swapFullscreen()
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            showHideConsole()
        }

        super.render()

        GameConsole.console.draw()
    }

    fun swapFullscreen() {
        if (Gdx.graphics.isFullscreen)
            Gdx.graphics.setWindowedMode(854, 480)
        else
            Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
    }

    fun showHideConsole() {
        if (GameConsole.switchVisibility()) {
            val screen = this.screen
            if (screen is PlayScreen) {
                GameConsole.commandExecutor = screen.PlayCslCommandExc()
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)

        GameConsole.onWindowResize()
    }

    override fun dispose() {
        if (Const.IS_DESKTOP)
            stopDiscordRich()

        super.dispose()
    }
}
