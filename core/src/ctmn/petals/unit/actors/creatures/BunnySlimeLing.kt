package ctmn.petals.unit.actors.creatures

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import ctmn.petals.unit.*
import ctmn.petals.unit.component.*

class BunnySlimeLing : UnitActor(
    UnitComponent(
        "slime_ling",
        100,
        0,
        4,
        6
    )
) {

    var item: TextureRegion? = null

    init {
        add(FollowerComponent())
        add(
            AttackComponent(
                20,
                25,
                1
            )
        )
        add(TerrainPropComponent(TerrainPropsPack.slime))
        add(MatchUpBonusComponent())
        add(TraitComponent(fireVulnerability = 1.5f))

        hitSounds = arrayOf("slime_hit.ogg")
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        if (item != null) {
            if (cAnimationView?.animation == defaultAnimation)
                batch.draw(item, x - 8f, y - 8f, 32f, 32f)
        }

        super.draw(batch, parentAlpha)
    }
}