package ctmn.petals.unit.abilities

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import ctmn.petals.Config
import ctmn.petals.actors.actions.CameraShakeAction
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.playscreen.seqactions.ActorAction
import ctmn.petals.playscreen.playStageOrNull
import ctmn.petals.playscreen.stageName
import ctmn.petals.sim.Battle
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.*
import ctmn.petals.utils.setPosByCenter

class HammerAbility : Ability("hammer") {

    var damage: Int = 0

    init {
        target = Target.ENEMY_UNIT
        range = 1
        castTime = 2f
        cooldown = 6
        cost = 5
    }

    override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean =
        applyDecision(battle, unitCaster, tileX, tileY, decide(battle, unitCaster, tileX, tileY))

    /**
     * Rolls the throws: which units are picked up, which way they fly, and how hard they land.
     * Touches nothing, so the server can decide it and the command can carry it.
     */
    override fun decide(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): AbilityDecision {
        val decision = AbilityDecision()

        fun addThrow(unit: UnitActor?, dX: Int, dY: Int, throwDamage: Int) {
            if (unit == null) return

            decision.throws.add(AbilityThrow(unit.stageName, dX, dY, throwDamage))
        }

        getTargets(battle.field, unitCaster, tileX, tileY).firstOrNull()?.let { unit ->
            if (unit is UnitActor)
                addThrow(unit, 0, 0, battle.randomInt75(damage))
        }
        battle.field.getUnit(tileX, tileY + 1)?.let { unit ->
            if (!unit.isAlly(unitCaster))
                addThrow(unit, 0, 1, battle.randomInt75(damage / 2))
        }
        battle.field.getUnit(tileX, tileY - 1)?.let { unit ->
            if (!unit.isAlly(unitCaster))
                addThrow(unit, 0, -1, battle.randomInt75(damage / 2))
        }
        battle.field.getUnit(tileX - 1, tileY)?.let { unit ->
            if (!unit.isAlly(unitCaster))
                addThrow(unit, -1, 0, battle.randomInt75(damage / 2))
        }
        battle.field.getUnit(tileX + 1, tileY)?.let { unit ->
            if (!unit.isAlly(unitCaster))
                addThrow(unit, 1, 0, battle.randomInt75(damage / 2))
        }

        return decision
    }

    /** Applies decided throws, then the earthcrack and the camera shake. */
    override fun applyDecision(
        battle: Battle,
        unitCaster: UnitActor,
        tileX: Int,
        tileY: Int,
        decision: AbilityDecision,
    ): Boolean {
        unitCaster.setAnimation(unitCaster.attackAnimation)

        //resolve the state change immediately, then only queue the visuals
        decision.throws.forEach { thrown ->
            val unit = battle.field.findUnit(thrown.unitId) ?: return@forEach

            battle.throwUnitAction(unit, thrown.dx, thrown.dy, thrown.damage, unitCaster)
        }

        createEarthcrack(battle, tileX, tileY)

        battle.queueSeqAction(ActorAction(unitCaster,
            Actions.sequence(
                Actions.delay(unitCaster.attackAnimation?.animationDuration ?: 0.75f),
                OneAction {
                    unitCaster.playStageOrNull?.addAction(CameraShakeAction(1.5f))
                }
            )))

        return true
    }

    private fun createEarthcrack(battle: Battle, tileX: Int, tileY: Int) {
        val oldTile = battle.field.getTile(tileX, tileY) ?: return
        val replaceTile =
            when (oldTile.terrain) {
                "forest" -> TileActor("fallen_forest", "fallenforest")
                "grass" -> TileActor("earthcrack", "earthcrack")
                "fallenforest" -> TileActor("earthcrack", "earthcrack")
                "earthcrack" -> null
                else -> return
            }

        replaceTile?.let {
            if (battle.field.getTile(
                    oldTile.tiledX,
                    oldTile.tiledY,
                    oldTile.layer - 1
                )?.terrain == TerrainNames.grass
            ) {
                battle.field.removeGameActor(oldTile)
            } else {
                battle.field.shiftLayerAt(oldTile.tiledX, oldTile.tiledY, -1)
            }
            it.setPosition(tileX, tileY)
            battle.field.addGameActor(it)
        }

        //create effect
        val effect = ctmn.petals.effects.CreateEffect.earthcrack
        effect.setPosByCenter(
            (tileX + 1) * Config.TILE_SIZE + Config.TILE_SIZE,
            (tileY + 1) * Config.TILE_SIZE + Config.TILE_SIZE
        )
        battle.queueSeqAction(effect)
    }
}
