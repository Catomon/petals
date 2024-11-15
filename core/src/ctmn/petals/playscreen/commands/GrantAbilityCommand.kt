package ctmn.petals.playscreen.commands

import ctmn.petals.effects.FloatingLabelAnimation
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.stageName
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.abilities
import ctmn.petals.unit.Ability
import ctmn.petals.unit.cAbilities
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY

class GrantAbilityCommand(val unitId: String, val ability: Ability) : Command() {

    constructor(unit: UnitActor, ability: Ability) : this(unit.stageName, ability)

    override fun canExecute(playScreen: PlayScreen): Boolean {
        val unit: UnitActor = playScreen.playStage.root.findActor(unitId)

        return unit.cAbilities != null
    }

    override fun execute(playScreen: PlayScreen): Boolean {
        val unit: UnitActor = playScreen.playStage.root.findActor(unitId)

        playScreen.playStage.addActor(
            FloatingLabelAnimation("New Ability: $ability", "default").position(unit.centerX, unit.centerY))

        unit.abilities.add(ability)
        return true
    }
}