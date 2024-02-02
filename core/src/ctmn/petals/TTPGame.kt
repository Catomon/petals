package ctmn.petals

import com.badlogic.gdx.Application
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Logger

val game: TTPGame get() = Gdx.app.applicationListener as TTPGame

class TTPGame : Game() {

    val assets = Assets()

    override fun create() {
        if (!GameConst.IS_RELEASE) Gdx.app.logLevel = Logger.DEBUG

        ShaderProgram.pedantic = false;

        ctmn.petals.assets = assets

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            val pixmap = Pixmap(Gdx.files.internal("cursor.png"))
            val xHotspot = 0
            val yHotspot = 0
            val cursor: Cursor = Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot)
            Gdx.graphics.setCursor(cursor)
            pixmap.dispose()
        }

        assets.loadUI()

        setScreen(LoadingScreen(this))
    }

    override fun render() {
        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            if (GameConst.IS_RELEASE)
                setScreen(MenuScreen(this))
            else
                setScreen(DevScreen(this))
        }

        if (Gdx.input.isKeyPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen)
                Gdx.graphics.setWindowedMode(854, 480)
            else
                Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
        }

        super.render()
    }
}
