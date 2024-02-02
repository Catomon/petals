package ctmn.petals.unit.abilities

import com.badlogic.gdx.utils.Array
import ctmn.petals.effects.CreateEffect
import ctmn.petals.effects.EffectActor
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.queueAction
import ctmn.petals.playscreen.seqactions.WaitAction
import ctmn.petals.unit.*
import ctmn.petals.utils.*
import ctmn.petals.utils.tiledX

class LightningAbility : Ability(
    "lightning",
    Target.ENEMY_UNIT,
    5,
    5,
    4,
    4,
) {

    private var damage: Int = 15 + 5 * level

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val playStage = playScreen.playStage
        val actors = getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        val lightedUnits = Array<UnitActor>()
        actors.firstOrNull { it is UnitActor && it.tiledX == tileX && it.tiledY == tileY }?.let {
            lightedUnits.add(it as UnitActor)
        } ?: return false

        var repeat = true
        while (repeat) {
            repeat = false

            for (unit in lightedUnits) {
                val surroundingUnits = playStage.getSurroundingUnits(unit).filter { actors.contains(it) }
                val notLighted = surroundingUnits.filterNot { lightedUnits.contains(it, false) }
                if (notLighted.isEmpty()) continue

                notLighted.forEach { lightedUnits.add(it) }
                repeat = true
            }
        }

//        for (unit in lightedUnits) {
//            if (!playScreen.friendlyFire && unit.isAlly(unitCaster)) continue
//
//            unit.dealDamage(damage, unitCaster, playScreen)
//        }

        var lastEffect: EffectActor? = null

        for (unit in lightedUnits) {
            val effect =
                if (unit.tiledX == tileX && unit.tiledY == tileY)
                    CreateEffect.lightning
                else
                    CreateEffect.lightningDot

            effect.setPosition(unit.centerX, unit.centerY)

            if (unit.tiledX != tileX || unit.tiledY != tileY)
                playStage.addActor(effect)
            else
                lastEffect = effect

            if (castTime < effect.lifeTime)
                castTime = effect.lifeTime
        }

        playScreen.queueAction(WaitAction(castTime))
        playScreen.queueAction {
            for (unit in lightedUnits) {
                if (!playScreen.friendlyFire && unit.isAlly(unitCaster)) continue

                unit.dealDamage(damage, unitCaster, playScreen)
            }
        }

        if (lastEffect != null)
            playStage.addActor(lastEffect)

        return true
    }
}
