package ctmn.petals.playscreen.seqactions

import ctmn.petals.Const
import ctmn.petals.effects.UnitDeathExplosionEffect
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.unit.UnitActor

class KillUnitAction(val unit: UnitActor) : SeqAction() {

    private lateinit var deathEffect: UnitDeathExplosionEffect
    private val deadUnitBodyEffect = ctmn.petals.effects.DeadUnitBodyEffect(unit, 5f)

    override fun update(deltaTime: Float) {
        isDone = deathEffect.lifeTime <= 0
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        unit.remove()

        playScreen.playStage.addActorAfterTiles(deadUnitBodyEffect)

        deathEffect = UnitDeathExplosionEffect(playScreen.assets)

        deathEffect.x = unit.x + Const.TILE_SIZE / 2
        deathEffect.y = unit.y + Const.TILE_SIZE / 2
        playScreen.playStage.addActor(deathEffect)

        deadUnitBodyEffect.remove()

        return true
    }
}