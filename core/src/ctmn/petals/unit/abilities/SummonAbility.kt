package ctmn.petals.unit.abilities

import com.badlogic.gdx.Gdx
import ctmn.petals.Const
import ctmn.petals.effects.CreateEffect
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playstage.getUnitsForLeader
import ctmn.petals.unit.*
import ctmn.petals.utils.unTiled
import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.SummonerComponent

class SummonAbility : Ability(
    SUMMON,
    Target.TILE_PASSABLE,
    5,
    10,
    1,
    0,
    Type.OTHER,
    1,
) {

    init {
        skipConfirmation = true
        defGUI = false

        castAmounts = 4
        castAmountsLeft = castAmounts
    }

    private var isFirstMove = false

    override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
        val playStage = playScreen.playStage

        val cSummoner = unitCaster.get(SummonerComponent::class.java) ?: let {
            Gdx.app.error(javaClass.simpleName, "Unit has no SummonerComponent")

            SummonerComponent()
        }

        with(cSummoner) {
            //check if unitCaster didn't reach max units
            var unitsOwned = 0
            for (unit in playStage.getUnitsForLeader(unitCaster.leaderID, true)) {
                units.firstOrNull { it == unit.selfName }?.let {
                    unitsOwned++
                }
            }
            if (unitsOwned >= maxUnits) return false

            //create unit
            val selectedUnitName = selectedUnit ?: return false
            val player = playScreen.turnManager.getPlayerById(unitCaster.playerId)
                ?: throw IllegalStateException("Player with such id is not found")
            val summonUnit = playScreen.unitsData.get(selectedUnitName, player)

            if (summonUnit.isFollower && unitCaster.leaderID != -1)
                summonUnit.followerID = unitCaster.leaderID

            summonUnit.allies.addAll(player.allies)

            summonUnit.actionPoints = 0
            summonUnit.setPosition(tileX, tileY)

            //if round is first, and unitCaster didn't use AP, give summoned unit AP
//        if (playScreen.turnManager.currentRound == 0 && unitCaster.actionPoints == Const.ACTION_POINTS)
//            giveAP = true

            if (unitCaster.actionPoints >= 2) {
                isFirstMove = true
            }

            if (giveAP && isFirstMove) {
                summonUnit.actionPoints = Const.ACTION_POINTS
                unitCaster.actionPoints = Const.ACTION_POINTS
            }

            if (castAmountsLeft == 1) {
                giveAP = false
                isFirstMove = false
            }

            //add unit to stage
            playScreen.playStage.addActor(summonUnit)

            //create effect
            val effect = CreateEffect.summon
            effect.setPosition(
                tileX.unTiled() + Const.TILE_SIZE / 2,
                tileY.unTiled() + Const.TILE_SIZE / 2
            )
            playStage.addActor(effect)

            castTime = effect.lifeTime
        }

        return true
    }
}