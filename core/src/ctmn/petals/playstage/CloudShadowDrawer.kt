package ctmn.petals.playstage

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Array
import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.assets
import ctmn.petals.playscreen.playStage
import ctmn.petals.playscreen.playStageOrNull
import kotlin.random.Random

class CloudShadowDrawer : Actor() {

    private val shadows = Array<Shadow>(8)

    private var timeToSpawn = 200f // see setStage
    private var timePassed = Random.nextInt(timeToSpawn.toInt() / 2, timeToSpawn.toInt()).toFloat()

    private val shadowRegions = assets.atlases.findRegions("cloud_shadow")

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        shadows.forEach {
            val texture = shadowRegions[it.index]
            batch.draw(texture, it.x, it.y, texture.regionWidth.toFloat(), texture.regionHeight.toFloat())
        }
    }

    override fun act(delta: Float) {
        super.act(delta)

        shadows.forEach {
            it.x -= 2f * delta
            if (it.x <= -shadowRegions[it.index].regionWidth)
                shadows.removeValue(it, false)
        }

        timePassed += delta
        if (timePassed >= timeToSpawn) {
            spawn()
            timePassed = Random.nextInt(0, timeToSpawn.toInt()).toFloat()
        }
    }

    private fun spawn() {
        playStageOrNull ?: return

        val height =
            if (playStage.mapHeight() < 14f * TILE_SIZE)
                Random.nextInt(-48, 14 * TILE_SIZE.toInt()).toFloat()
            else
                Random.nextInt(-48, playStage.mapHeight().toInt() - 48).toFloat()
        shadows.add(
            Shadow(
                Random.nextInt(0, 2),
                playStage.mapWidth(),
                height
            )
        )
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage is PlayStage) {
            addAction(OneAction {
                timeToSpawn = maxOf(200f * (playStage.tiledHeight() / 14f), 200f)
                spawn()
                shadows.first().x = Random.nextInt(
                    0 - shadowRegions.first().regionWidth / 2,
                    playStage.mapWidth().toInt()
                ).toFloat()
            })
        } else {
            if (stage != null)
                throw IllegalArgumentException("Only for PlayStage")
        }
    }

    private inner class Shadow(val index: Int, var x: Float, var y: Float)
}