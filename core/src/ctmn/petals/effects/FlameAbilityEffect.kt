package ctmn.petals.effects

import ctmn.petals.Assets
import ctmn.petals.GameConst
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getMovementGrid
import ctmn.petals.unit.TerrainCosts
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.utils.setPositionByCenter
import ctmn.petals.utils.unTiled
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Array
import ctmn.petals.newPlaySprite

class FlameAbilityEffect(val range: Int, val tileX: Int, val tileY: Int, assets: Assets) : EffectActor() {

    private val vectors = Array<Vector2>()

    init {
        animation = RegionAnimation(0.10f, assets.textureAtlas.findRegions("effects/ability_fireball"))
        lifeTime = animation!!.animationDuration

        sprite = newPlaySprite(animation!!.currentFrame)

        setPosition(tileX.unTiled() + GameConst.TILE_SIZE / 2, tileY.unTiled() + GameConst.TILE_SIZE / 2)
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)

        if (stage is PlayStage) {
            val matrix = stage.getMovementGrid(range, tileX, tileY, TerrainCosts.ability)
            for (x in matrix.indices) {
                for (y in matrix[x].indices) {
                    if (matrix[x][y] != 0)
                        vectors.add(Vector2(x.unTiled(), y.unTiled()))
                }
            }
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        for (vector in vectors) {
            sprite.setPositionByCenter(vector.x + GameConst.TILE_SIZE / 2, vector.y + GameConst.TILE_SIZE / 2)
            sprite.draw(batch)
        }
    }
}
