package ctmn.petals.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.Const
import ctmn.petals.PetalsGame
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.actors.actions.TimeAction
import ctmn.petals.actors.actions.UpdateAction
import ctmn.petals.utils.logErr
import kotlin.concurrent.thread

class LoadingScreen(private val game: PetalsGame) : Stage(ExtendViewport(32f, 720f)), Screen {

    val assets = game.assets

    private val bunnyImage = VisImage(Texture("sleepy.png"))
    private val loadingLabel = VisLabel("...%")
    private val msg = VisLabel("Loading...")
    private var timePast = 0f

    private var isDone = false

    companion object {
        var progress = 0
    }

    init {
        progress = 0

        addActor(VisTable().apply {
            setFillParent(true)
            add(bunnyImage).padTop(12f).size(256f)
            row()
            add(msg).padTop(32f)
        })

        //msg.isVisible = false
        msg.addAction(UpdateAction {
            timePast += it
            if (timePast >= 15) {
                msg.setText("Almost done...")
                msg.pack()
                msg.isVisible = true
                return@UpdateAction true
            }
            false
        })

        assets.beginLoadingAll()

        addAction(Actions.forever(Actions.sequence(DelayAction(0.01f), OneAction {
            if (progress < assets.progress * 100) {
                progress += 1
                loadingLabel.setText("$progress%")
            }
        })))

        addAction(
            Actions.sequence(
                TimeAction {
                    assets.update()
                },
                OneAction {
                    thread { // process the result, e.g. add it to an Array<Result> field of the ApplicationListener.
                        //results.add(result)
                        try {
                            assets.onFinishLoading()
                        } catch (e: Exception) {
                            logErr("Error in Assets.onFinishLoading(). " + e.printStackTrace())
                            Gdx.app.exit()
                        }

                        isDone = true
                    }
                }
            )
        )
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (isDone) {
            if (progress >= 100) {
                if (Const.IS_RELEASE)
                    game.screen = MenuScreen(game)
                else
                    game.screen = DevScreen(game)
            }

            progress = 100
        }
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
        val viewport = viewport as ExtendViewport
        if (Gdx.app.type == Application.ApplicationType.Android)
            if (Const.IS_PORTRAIT) {
                viewport.minWorldWidth = width / Const.GUI_SCALE / 1.25F
                viewport.minWorldHeight = height / Const.GUI_SCALE / 1.25F
            } else {
                viewport.minWorldWidth = width / Const.GUI_SCALE
                viewport.minWorldHeight = height / Const.GUI_SCALE
            }

        viewport.update(width, height)
    }

    override fun pause() {

    }

    override fun resume() {

    }

    override fun hide() {

    }
}