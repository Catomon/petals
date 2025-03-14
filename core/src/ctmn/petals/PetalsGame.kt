package ctmn.petals

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Logger
import ctmn.petals.GamePref.fullscreen
import ctmn.petals.GamePref.musicVolume
import ctmn.petals.GamePref.targetFps
import ctmn.petals.GamePref.vSync
import ctmn.petals.GamePref.soundVolume
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.screens.LoadingScreen
import ctmn.petals.utils.GameConsole
import ctmn.petals.utils.setMouseCursor

val game: PetalsGame get() = Gdx.app.applicationListener as PetalsGame

fun updateAppConfigToPrefs() {
    AudioManager.soundVolume = soundVolume
    AudioManager.musicVolume = musicVolume
    strings = getLangStringsByPrefs()
    Gdx.graphics.setForegroundFPS(targetFps)
    Gdx.graphics.setVSync(vSync)
    if (fullscreen)
        Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
    else
        if (Gdx.graphics.isFullscreen)
            Gdx.graphics.setWindowedMode(854, 480)
}

class PetalsGame(val runTexturePacker: Runnable = Runnable {  }, val onCreate: Runnable = Runnable {  }) : Game() {

    val assets = Assets()

    // to change through console
    var debugMode = Const.DEBUG_MODE

    override fun create() {
        onCreate.run()

        updateAppConfigToPrefs()

        if (!Const.IS_RELEASE) Gdx.app.logLevel = Logger.DEBUG

        if (Const.IS_DESKTOP) {
            startDiscordRich()
        }

        GameConsole.consoleDisabled = !Const.CONSOLE_ENABLED

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
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            swapFullscreen()
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            showHideConsole()
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (GameConsole.isVisible)
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

    override fun setScreen(screen: Screen?) {
        val oldScreen = this.screen
        if (oldScreen is PlayScreen && oldScreen != screen && !oldScreen.inSettingsScreen) { //todo just dispose before setScreen
            oldScreen.dispose()
        }

        if (oldScreen is PlayScreen && screen !is PlayScreen && !oldScreen.inSettingsScreen) {
            assets.tilesAtlas = assets.tilesSummerAtlas
        }

        super.setScreen(screen)
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
