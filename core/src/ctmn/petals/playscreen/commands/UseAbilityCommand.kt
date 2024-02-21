package ctmn.petals.playscreen.commands

import ctmn.petals.Const
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.*
import ctmn.petals.unit.Ability
import ctmn.petals.unit.*
import com.badlogic.gdx.Gdx
import ctmn.petals.playscreen.seqactions.WaitAction
import ctmn.petals.playstage.tiledDst
import ctmn.petals.unit.abilities.SummonAbility
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.InvisibilityComponent

class UseAbilityCommand(val unitCasterId: String, val  abilityName: String, val tileX: Int, val tileY: Int) : Command() {

    var param: String? = null

    constructor(unitCaster: UnitActor, abilityName: String, tileX: Int, tileY: Int) : this(
        unitCaster,
        unitCaster.getAbility(abilityName) ?: throw IllegalArgumentException("No such ability: $abilityName"),
        tileX,
        tileY)

    constructor(unitCaster: UnitActor, ability: Ability, tileX: Int, tileY: Int) : this(unitCaster.stageName, ability.name, tileX, tileY) {
        if (ability is SummonAbility)
            param = unitCaster.summoner.selectedUnit
    }

    override fun canExecute(playScreen: PlayScreen): Boolean {
        val unitCaster: UnitActor = playScreen.playStage.root.findActor(unitCasterId) ?: return false
        val ability = unitCaster.getAbility(abilityName) ?: throw IllegalArgumentException("No such ability: $abilityName")

        val abilities = unitCaster.cAbilities?.abilities ?: return false

        //return if no AP, Mana, or Ability found
        if (unitCaster.mana < ability.cost || !abilities.contains(ability))
            return false

        if (unitCaster.actionPoints <= 0 && ability.castAmountsLeft == ability.castAmounts)
            return false

        //return if ability is on cooldown
        if (ability.currentCooldown > 0)
            return false

        //return if ability used too far from ordered unit
        if (tiledDst(unitCaster.tiledX, unitCaster.tiledY, tileX, tileY) > ability.range)
            return false

        //find targets
        val targetActorsArray = ability.getTargets(playScreen.playStage, unitCaster, tileX, tileY)

        //return if unit has no targets
        if (targetActorsArray.isEmpty) {
            Gdx.app.error(UseAbilityCommand::class.simpleName, "Canceled: no targets found")
            return false
        }

        //summon ability mana cost for unit + ability
        if (ability is SummonAbility) {
            if (param != null) unitCaster.summoner.selectedUnit = param
            if (unitCaster.summoner.selectedUnit == null) return false

            val unitSummonCost = Units.get(unitCaster.summoner.selectedUnit!!).cSummonable?.cost ?: 0

            if (ability.castAmounts == ability.castAmountsLeft) {
                if (unitCaster.mana < unitSummonCost + ability.cost)
                    return false
            } else
                if (unitCaster.mana < unitSummonCost)
                    return false
        }

        return true
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val unitCaster: UnitActor = playScreen.playStage.root.findActor(unitCasterId) ?: return false
        val ability = unitCaster.getAbility(abilityName) ?: throw IllegalArgumentException("No such ability: $abilityName")

        if (ability is SummonAbility) {
            if (param != null) unitCaster.summoner.selectedUnit = param
            if (unitCaster.summoner.selectedUnit == null) return false
        }

        //try to activate, return on fail
        val isSuccess =
            ability.activate(playScreen, unitCaster, tileX, tileY)

        if (!isSuccess) return false

        //ap
        if (ability.castAmounts == ability.castAmountsLeft)
            unitCaster.actionPoints -= Const.ACTION_POINTS_ABILITY

        //reveal if invisible
        unitCaster.del(InvisibilityComponent::class.java)

        //mana
        if(ability.castAmounts == ability.castAmountsLeft)
            unitCaster.mana -= ability.cost

        //cd
        ability.castAmountsLeft--

        if (ability.castAmountsLeft < 1)
            ability.currentCooldown = ability.cooldown

        //summon ability mana for unit
        if (ability is SummonAbility) {
            val unitSummonCost = Units.get(unitCaster.summoner.selectedUnit!!).cSummonable?.cost ?: 0
            unitCaster.mana -= unitSummonCost
        }

        //command lifeTime
        playScreen.queueAction(WaitAction(ability.castTime))

        //unit cast animation
        unitCaster.abilityCastAnimation?.let { unitCaster.setAnimation(it, 1f) }

        return true
    }
}
