package ctmn.petals.gameactors

import ctmn.petals.GameConst.TILE_SIZE
import ctmn.petals.assets
import ctmn.petals.playstage.mapHeight
import ctmn.petals.playstage.mapWidth
import ctmn.petals.playscreen.playStage
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.utils.Timer
import ctmn.petals.utils.setPositionByCenter
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import ctmn.petals.newPlaySprite
import ctmn.petals.playstage.GameActor

class Pixie : GameActor() {

    private val timer = Timer(MathUtils.random(5, 12).toFloat())

    var isSeeking = true

    private val animation = RegionAnimation(0.5f, assets.textureAtlas.findRegions("units/pixie"))
    private val sprite = newPlaySprite(animation.currentFrame)

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        sprite.draw(batch)
    }

    fun moveTo(endX: Float, endY: Float) : Action {
        actions.clear()

        val action = Actions.moveTo(endX, endY, Vector2.dst(x, y, endX, endY) / 100)
        addAction(action)

        return action
    }

    override fun act(delta: Float) {
        super.act(delta)

        animation.update(delta)
        sprite.setRegion(animation.currentFrame)
        sprite.setPositionByCenter(x + TILE_SIZE / 2, y + TILE_SIZE / 2)

        if (isSeeking) {
            timer.update(delta)
            if (timer.isDone) {
                timer.start(MathUtils.random(5, 12).toFloat())

                val randomX = MathUtils.random(0f, playStage.mapWidth() - TILE_SIZE)
                val randomY = MathUtils.random(0f, playStage.mapHeight() - TILE_SIZE)

                moveTo(randomX, randomY)
            }
        }
    }
}