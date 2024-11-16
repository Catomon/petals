package ctmn.petals.unit.abilities

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import ctmn.petals.Const
import ctmn.petals.actors.actions.CameraShakeAction
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.addAction
import ctmn.petals.playscreen.seqactions.ActorAction
import ctmn.petals.playscreen.seqactions.WaitAction
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.shiftLayerAt
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.*
import ctmn.petals.utils.setPosByCenter
import ctmn.petals.utils.unTiled
import kotlin.random.Random

class HammerAbility : Ability("hammer") {

    var damage: Int = 0

    init {
        target = Target.ENEMY_UNIT
        range = 1
        castTime = 2f
        cooldown = 6
        cost = 5
    }

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val playStage = playScreen.playStage

        unitCaster.setAnimation(unitCaster.attackAnimation)

        playScreen.addAction(ActorAction(unitCaster,
            Actions.sequence(
                Actions.delay(unitCaster.attackAnimation?.animationDuration ?: 0.75f),
                OneAction {
                    getTargets(playStage, unitCaster, tileX, tileY).firstOrNull()?.let { unit ->
                        if (unit is UnitActor)
                            playScreen.throwUnitAction(unit, 0, 0, playScreen.randomInt75(damage), unitCaster)
                    }
                    playStage.getUnit(tileX, tileY + 1)?.let { unit ->
                        if (!unit.isAlly(unitCaster))
                            playScreen.throwUnitAction(unit, 0, 1, playScreen.randomInt75(damage / 2), unitCaster)
                    }
                    playStage.getUnit(tileX, tileY - 1)?.let { unit ->
                        if (!unit.isAlly(unitCaster))
                            playScreen.throwUnitAction(unit, 0, -1, playScreen.randomInt75(damage / 2), unitCaster)
                    }
                    playStage.getUnit(tileX - 1, tileY)?.let { unit ->
                        if (!unit.isAlly(unitCaster))
                            playScreen.throwUnitAction(unit, -1, 0, playScreen.randomInt75(damage / 2), unitCaster)
                    }
                    playStage.getUnit(tileX + 1, tileY)?.let { unit ->
                        if (!unit.isAlly(unitCaster))
                            playScreen.throwUnitAction(unit, 1, 0, playScreen.randomInt75(damage / 2), unitCaster)
                    }

                    playStage.addAction(CameraShakeAction(1.5f))

                    createEarthcrack(playStage, tileX, tileY)
                }
            )))

        return true
    }

    private fun createEarthcrack(playStage: PlayStage, tileX: Int, tileY: Int) {
        val oldTile = playStage.getTile(tileX, tileY) ?: return
        val replaceTile =
            when (oldTile.terrain) {
                "forest" -> TileActor("fallen_forest", "fallenforest")
                "grass" -> TileActor("earthcrack", "earthcrack")
                "fallenforest" -> TileActor("earthcrack", "earthcrack")
                "earthcrack" -> null
                else -> return
            }

        replaceTile?.let {
            if (playStage.getTile(
                    oldTile.tiledX,
                    oldTile.tiledY,
                    oldTile.layer - 1
                )?.terrain == TerrainNames.grass
            ) {
                oldTile.remove()
            } else {
                playStage.shiftLayerAt(oldTile.tiledX, oldTile.tiledY, -1)
            }

            it.setPosition(tileX, tileY)
            playStage.addActor(it)
        }

        //create effect
        val effect = ctmn.petals.effects.CreateEffect.earthcrack
        effect.setPosByCenter(
            (tileX + 1).unTiled() + Const.TILE_SIZE,
            (tileY + 1).unTiled() + Const.TILE_SIZE
        )
        playStage.addActor(effect)
    }
}