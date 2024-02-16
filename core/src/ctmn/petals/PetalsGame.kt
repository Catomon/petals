package ctmn.petals

import com.badlogic.gdx.Application
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Logger
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.utils.GameConsole
import ctmn.petals.utils.createCursor

val game: PetalsGame get() = Gdx.app.applicationListener as PetalsGame

class PetalsGame : Game() {

    val assets = Assets()

    override fun create() {
        if (!GameConst.IS_RELEASE) Gdx.app.logLevel = Logger.DEBUG

        GamePref.setEmptyToDefaultPrefs()
        GamePref.overridePrefs()

        ShaderProgram.pedantic = false;

        ctmn.petals.assets = assets

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            createCursor()
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
                GameConst.IS_RELEASE -> setScreen(MenuScreen(this))
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
}
