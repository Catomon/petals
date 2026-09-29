package ctmn.petals.playscreen.commands

import ctmn.petals.Config
import ctmn.petals.playscreen.stageName
import ctmn.petals.unit.Ability
import ctmn.petals.unit.*
import ctmn.petals.playscreen.seqactions.WaitAction
import ctmn.petals.playstage.tiledDst
import ctmn.petals.sim.Battle
import ctmn.petals.unit.abilities.SummonAbility
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.component.InvisibilityComponent

class UseAbilityCommand(val unitCasterId: String, val  abilityName: String, val tileX: Int, val tileY: Int) : Command() {

    var param: String? = null

    /**
     * What the ability came to, for an ability whose outcome is random. The server fills this in
     * and it travels with the command, so a client applies the same result.
     */
    var decision: AbilityDecision? = null

    /** Some abilities decide their outcome with a roll, so the server has to run this one. */
    override val serverDecides: Boolean get() = true

    constructor(unitCaster: UnitActor, abilityName: String, tileX: Int, tileY: Int) : this(
        unitCaster,
        unitCaster.getAbility(abilityName) ?: throw IllegalArgumentException("No such ability: $abilityName"),
        tileX,
        tileY)

    constructor(unitCaster: UnitActor, ability: Ability, tileX: Int, tileY: Int) : this(unitCaster.stageName, ability.name, tileX, tileY) {
        if (ability is SummonAbility)
            param = unitCaster.summoner.selectedUnit
    }

    override fun canExecute(battle: Battle): Boolean {
        val unitCaster: UnitActor = battle.field.findUnit(unitCasterId) ?: return false
        val ability = unitCaster.getAbility(abilityName) ?: return false

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
        val targetActorsArray = ability.getTargets(battle.field, unitCaster, tileX, tileY)

        //return if unit has no targets
        if (targetActorsArray.isEmpty) {
            //Gdx.app.error(UseAbilityCommand::class.simpleName, "Canceled: no targets found")
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

    override fun execute(battle: Battle): Boolean {
        val unitCaster: UnitActor = battle.field.findUnit(unitCasterId) ?: return false
        val ability = unitCaster.getAbility(abilityName) ?: return false

        if (ability is SummonAbility) {
            if (param != null) unitCaster.summoner.selectedUnit = param
            if (unitCaster.summoner.selectedUnit == null) return false
        }

        // An ability that decides its own outcome is decided once, by the server, and the command
        // carries the decision. Everybody else applies it. Abilities with nothing random to decide
        // just run themselves.
        val isSuccess = run {
            val decided = decision
            if (decided != null) return@run ability.applyDecision(battle, unitCaster, tileX, tileY, decided)

            val fresh = ability.decide(battle, unitCaster, tileX, tileY)
            if (fresh != null) {
                decision = fresh
                return@run ability.applyDecision(battle, unitCaster, tileX, tileY, fresh)
            }

            ability.activate(battle, unitCaster, tileX, tileY)
        }

        if (!isSuccess) return false

        //ap
        if (ability.castAmounts == ability.castAmountsLeft)
            unitCaster.actionPoints -= Config.ACTION_POINTS_ABILITY

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
        battle.queueSeqAction(WaitAction(ability.castTime))

        //unit cast animation
        unitCaster.abilityCastAnimation?.let { unitCaster.setAnimation(it, 1f) }

        return true
    }
}
