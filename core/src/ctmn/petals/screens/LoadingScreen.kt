package ctmn.petals.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.FillViewport
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel
import ctmn.petals.Const
import ctmn.petals.PetalsGame

class LoadingScreen(private val game: PetalsGame) : Stage(FillViewport(1366f * 0.3f, 768f * 0.3f)), Screen {

    val assets = game.assets

    private val background = VisImage(Texture("loading_screen.png"))
    private val bunnyImage = VisImage(Texture("bunny.png"))
    private val loadingLabel = VisLabel("...%")

    init {
        addActor(background)
        bunnyImage.setSize(64f, 64f)
        addActor(bunnyImage)

        loadingLabel.setFontScale(1f)
        addActor(loadingLabel)

        assets.beginLoadingAll()
    }

    override fun act(delta: Float) {
        super.act(delta)

        if (assets.update()) {
            if (Const.IS_RELEASE)
                game.screen = MenuScreen(game)
            else
                game.screen = DevScreen(game)
        }

        loadingLabel.setText("${(assets.progress * 100).toInt()}%")
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        act()
        draw()
    }

    override fun show() {

    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)

        background.setPosition(
            viewport.worldWidth / 2 - background.width / 2,
            viewport.worldHeight / 2 - background.height / 2)
        bunnyImage.setPosition(
            viewport.worldWidth / 2 - bunnyImage.width / 2,
            viewport.worldHeight / 2 - bunnyImage.height / 2 + viewport.worldHeight / 4)
        loadingLabel.setPosition(
            viewport.worldWidth / 2 - loadingLabel.width / 2,
            viewport.worldHeight / 2 - loadingLabel.height / 2)
    }

    override fun pause() {
        
    }

    override fun resume() {
        
    }

    override fun hide() {
        
    }
}