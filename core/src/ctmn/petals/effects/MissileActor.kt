package ctmn.petals.effects

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.RemoveAction
import ctmn.petals.actors.actions.JumpAction
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.assets
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playstage.shiftLayerAt
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.TileData
import ctmn.petals.utils.AnimatedSprite
import ctmn.petals.utils.logErr
import ctmn.petals.utils.setPositionByCenter
import ctmn.petals.utils.tiled

class MissileActor(
    missileName: String = "catapult_missile",
    explosionName: String = "catapult_explosion",
    targetX: Float = 0f,
    targetY: Float = 0f,
    power: Float = 200f,
) : Actor() {

    var impactResultTile = "pit"

    val missileSprite = AnimatedSprite(assets.effectsAtlas.findRegions(missileName))
    val explosionSprite = AnimatedSprite(assets.effectsAtlas.findRegions(explosionName), 0.1f).apply {
        animation.playMode = Animation.PlayMode.NORMAL
    }
    private var currentSprite = missileSprite
    val jumpAction = JumpAction(targetX, targetY, power = power)

    var isLanded = false

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage != null) {
            addAction(jumpAction)
            addAction(DelayAction(5f).apply {
                action = OneAction {
                    logErr("Effect time out ${this@MissileActor}")
                    addAction(RemoveAction())
                }
            })
        }
    }

    fun setStart(x: Float, y: Float) {
        setPosition(x, y)
        jumpAction.startX = x
        jumpAction.startY = y
        jumpAction.setStart(x, y)
    }

    fun setTarget(x: Float, y: Float) {
        jumpAction.endX = x
        jumpAction.endY = y
    }

    override fun act(delta: Float) {
        super.act(delta)

        currentSprite.update(delta)

        if (jumpAction.actor == null && !isLanded) {
            isLanded = true
            currentSprite = explosionSprite

            playStageOrNull?.let { playStage ->
                playStage.getTile(jumpAction.endX.tiled(), jumpAction.endY.tiled())?.let { tile ->
                    if (tile.terrain == TerrainNames.grass) {
                        if (playStage.getTile(
                                tile.tiledX,
                                tile.tiledY,
                                tile.layer - 1
                            )?.terrain == TerrainNames.grass
                        ) {
                            tile.remove()
                        } else {
                            playStage.shiftLayerAt(tile.tiledX, tile.tiledY, -1)
                        }

                        val tileData = TileData.get(impactResultTile)
                        if (tileData != null)
                            playStage.addActor(TileActor(tileData, 1, tile.tiledX, tile.tiledY))
                    }
                }
            }
        }

        if (explosionSprite.animation.isAnimationFinished(explosionSprite.animation.stateTime))
            remove()
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        currentSprite.setPositionByCenter(x, y)
        currentSprite.draw(batch)
    }
}
