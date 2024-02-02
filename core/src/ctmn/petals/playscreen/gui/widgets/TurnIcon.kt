package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.assets
import ctmn.petals.player.Player
import ctmn.petals.playscreen.GameType
import ctmn.petals.playscreen.events.NextTurnEvent
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.strings
import ctmn.petals.unit.isAlly
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.widgets.newLabel
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions

class TurnIcon : Actor() {

    private val yourTurnIcon = assets.textureAtlas.findRegions("gui/animated/your_turn")
    private val enemyTurnIcon = assets.textureAtlas.findRegions("gui/animated/enemy_turn")
    private val allyTurnIcon = assets.textureAtlas.findRegions("gui/animated/ally_turn")
    private val neutralTurnIcon = assets.textureAtlas.findRegions("gui/animated/neutral_turn")

    private val animation = RegionAnimation(0.2f, yourTurnIcon)

    private val sprite = Sprite(animation.currentFrame)

    private val hideAfter = 2f
    private var mHideAfter = hideAfter

    val label = newLabel("Hello???" , "font_5")

    init {
//        if (sprite.width > 18)
//            sprite.setSize(sprite.width * 0.675f, sprite.height * 0.675f)
        setSize(sprite.width, sprite.height)
    }

    private fun onNextTurn(player: Player, playerTurnMaker: Player) {
        val guiStage = stage as PlayGUIStage

        if (guiStage.playScreen.gameType == GameType.PVP_SAME_SCREEN) {
                animation.setFrames(when (playerTurnMaker.id) {
                    Player.BLUE -> {
                        label.setText("Blue's turn")
                        yourTurnIcon
                    }
                    Player.RED -> {
                        label.setText("Red's turn")
                        enemyTurnIcon
                    }
                    Player.GREEN -> {
                        label.setText("Green's turn")
                        allyTurnIcon
                    }
                    else -> {
                        label.setText("Other's turn")
                        neutralTurnIcon
                    }
                })
            return
        }

        when {
            player.id == playerTurnMaker.id -> {
                label.setText(strings.play.your_turn)
                animation.setFrames(yourTurnIcon)
            }
            playerTurnMaker.isAlly(player.teamId) -> {
                label.setText(strings.play.ally_turn)
                animation.setFrames(allyTurnIcon)
            }
            else -> {
                label.setText(strings.play.enemy_turn)
                animation.setFrames(enemyTurnIcon)
            }
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        animation.update(delta)

        val guiStage = stage ?: return
        if (guiStage !is PlayGUIStage) return

        if (mHideAfter > 0 && guiStage.currentState == guiStage.myTurn) {
            mHideAfter -= delta

            if (mHideAfter <= 0 &&  guiStage.playScreen.gameType != GameType.PVP_SAME_SCREEN)
                addAction(Actions.fadeOut(1f))
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        label.color.a = color.a

        sprite.setRegion(animation.currentFrame)
        sprite.setAlpha(color.a)
        sprite.setPosition(x, y)
        sprite.draw(batch)
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage is PlayGUIStage) {
            onNextTurn(stage.player, stage.player)

            stage.playStage.addListener {
                when (it) {
                    is NextTurnEvent -> {
                        onNextTurn(stage.player, it.nextPlayer)
                        mHideAfter = hideAfter
                        addAction(Actions.fadeIn(0.5f))
                    }
                }

                false
            }
        }
    }
}