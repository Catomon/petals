package ctmn.petals.effects

import com.badlogic.gdx.graphics.Color
import ctmn.petals.Config
import ctmn.petals.assets
import ctmn.petals.hasAssets
import ctmn.petals.unit.UnitActor
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

object Animations {

    val barrier = RegionAnimation(0.35f, assets.atlases.findRegions("effects/barrier"))
    val waterWaves = RegionAnimation(Config.UNIT_ANIMATION_FRAME_DURATION, assets.atlases.findRegions("effects/unit_water"))
    val burning = RegionAnimation(0.4f, assets.atlases.findRegions("effects/burning"))

    fun update(delta: Float) {
        barrier.update(delta)
        waterWaves.update(delta)
        burning.update(delta)
    }
}

object CreateEffect {

    /**
     * An effect from an atlas region.
     *
     * Headless there is no atlas, so this comes back as a plain effect that only carries its
     * duration: it can sit in a battle's queue and nothing else. Abilities build their effect in
     * `activate`, so without this a server side bot threw on the missing atlas and left the
     * ability half applied. Timing is unchanged, since the duration is the same either way.
     */
    private fun effect(region: String, duration: Float): EffectActor =
        if (hasAssets) AnimationEffect(assets.atlases.findRegions(region), duration)
        else EffectActor().apply { lifeTime = duration }

    val summon get() = effect("effects/doll_summon", 0.10f)
    val lightning get() = effect("effects/ability_lightning", 0.2f)
    val lightningDot get() = effect("effects/ability_lightning_dot", 0.2f)
    val freeze get() = effect("effects/ability_freeze", 0.1f)
    val teleport get() = effect("effects/tp_puff", 0.1f)
    val earthcrack get() = effect("effects/tp_puff", 0.1f)
    val meteorite get(): EffectActor =
        if (hasAssets) MeteoriteEffect() else EffectActor().apply { lifeTime = 0.25f }
    val fog get() = effect("effects/tp_puff", 0.1f)
    val inspiration get() = effect("effects/tp_puff", 0.1f)
    val exhaust get() = effect("effects/tp_puff", 0.1f)
    val invisibility get() = effect("effects/tp_puff", 0.1f)


    fun <T: Actor> shakeActor(actor: T, power: Float, duration: Float) : T {
        actor.addAction(UnitShakeAction(power, duration))

        return actor
    }

    fun damageUnit(unit: UnitActor, visual: Boolean = true, sound: Boolean = true) : UnitActor {
        unit.addAction(
            UnitShakeAction(
                Config.UNIT_SHAKE_POWER,
                Config.UNIT_SHAKE_DURATION
            )
        )
        assets.getSound("hit.ogg").play()

        return unit
    }

    fun immuneEffect(unit: UnitActor) {
        if (unit.playStageOrNull == null) return

        val label = FloatingLabelAnimation("Immune!", "default")
        label.color = Color.YELLOW
        label.setPosition(unit.centerX - label.width / 4, unit.centerY)

        unit.playStageOrNull?.addActor(label)
    }
}
