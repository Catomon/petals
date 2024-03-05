package ctmn.petals.effects

import com.badlogic.gdx.graphics.Color
import ctmn.petals.Const
import ctmn.petals.assets
import ctmn.petals.unit.UnitActor
import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.newPlaySprite
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

object Animations {

    val barrier = RegionAnimation(0.35f, assets.textureAtlas.findRegions("effects/barrier"))
    val waterWaves = RegionAnimation(Const.UNIT_ANIMATION_FRAME_DURATION, assets.textureAtlas.findRegions("effects/unit_water"))

    fun update(delta: Float) {
        barrier.update(delta)
        waterWaves.update(delta)
    }
}

object CreateEffect {


    val summon get() = AnimationEffect(
        assets.textureAtlas.findRegions("effects/doll_summon"),
        0.10f
    )
    val lightning get() = AnimationEffect(
        assets.textureAtlas.findRegions("effects/ability_lightning"),
        0.2f
    )
    val lightningDot get() = AnimationEffect(
        assets.textureAtlas.findRegions("effects/ability_lightning_dot"),
        0.2f
    )
    val freeze get() = AnimationEffect(
        assets.textureAtlas.findRegions("effects/ability_freeze"),
        0.1f
    )
    val teleport get() = AnimationEffect(
        assets.textureAtlas.findRegions("effects/tp_puff"),
        0.1f
    )
    val earthcrack get() = AnimationEffect(
        assets.textureAtlas.findRegions("effects/tp_puff"),
        0.1f
    )
    val meteorite get() = MeteoriteEffect()
    val fog get() = AnimationEffect(
        assets.textureAtlas.findRegions("effects/tp_puff"),
        0.1f
    )
    val inspiration get() = AnimationEffect(
        assets.textureAtlas.findRegions("effects/tp_puff"),
        0.1f
    )
    val exhaust get() = AnimationEffect(
        assets.textureAtlas.findRegions("effects/tp_puff"),
        0.1f
    )
    val invisibility get() = AnimationEffect(
        assets.textureAtlas.findRegions("effects/tp_puff"),
        0.1f
    )


    fun <T: Actor> shakeActor(actor: T, power: Float, duration: Float) : T {
        actor.addAction(UnitShakeAction(power, duration))

        return actor
    }

    fun damageUnit(unit: UnitActor, visual: Boolean = true, sound: Boolean = true) : UnitActor {
        unit.addAction(
            UnitShakeAction(
                Const.UNIT_SHAKE_POWER,
                Const.UNIT_SHAKE_DURATION
            )
        )
        assets.getSound("hit.ogg").play()

        return unit
    }

    fun immuneEffect(unit: UnitActor) {
        if (unit.playStageOrNull == null) return

        val label = FloatingLabelAnimation("Immune!", "font_5")
        label.color = Color.YELLOW
        label.setPosition(unit.centerX - label.width / 4, unit.centerY)

        unit.playStageOrNull?.addActor(label)
    }
}
