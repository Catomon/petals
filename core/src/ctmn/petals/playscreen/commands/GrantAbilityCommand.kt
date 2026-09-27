package ctmn.petals.playscreen.commands

import ctmn.petals.playscreen.stageName
import ctmn.petals.sim.Battle
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.abilities
import ctmn.petals.unit.Ability
import ctmn.petals.unit.cAbilities
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class GrantAbilityCommand(val unitId: String, val ability: Ability) : Command() {

    constructor(unit: UnitActor, ability: Ability) : this(unit.stageName, ability)

    override fun canExecute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId) ?: return false

        return unit.cAbilities != null
    }

    override fun execute(battle: Battle): Boolean {
        val unit: UnitActor = battle.field.findUnit(unitId) ?: return false

        battle.showFloatingLabel("New Ability: $ability", null, unit.centerX, unit.centerY)

        unit.abilities.add(ability)
        return true
    }
}
