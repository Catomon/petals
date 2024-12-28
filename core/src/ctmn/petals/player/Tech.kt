package ctmn.petals.player

import com.badlogic.gdx.scenes.scene2d.Actor
import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.abilities
import ctmn.petals.unit.abilities.PlagueAbility
import ctmn.petals.unit.cAbilities
import ctmn.petals.unit.component.AbilitiesComponent

val TECH_FAIRY_HAMMER_ABILITY = "tech:fairy_hammer_ability"
val TECH_FAIRY_HEALER_ABILITY = "tech:fairy_healer_ability"
val TECH_LEADER = "tech:fairy_healer_ability"
val TECH_GOBLIN_PLAGUE_ABILITY = "tech_goblin_plague_ability"

open class Tech(
    val name: String,
    val species: String,
    val targetType: TargetType,
    val targetId: String,
    val time: Int,
    val buildingsNeeded: List<String>,
    protected val applyTech: (Actor?) -> Unit,
) {
    enum class TargetType {
        BoughtUnit
    }

    fun applyTechToUnit(unitActor: UnitActor) {
        if (targetId.isEmpty() || targetId == unitActor.selfName) {
            applyTech(unitActor)
        }
    }
}

object Techs {
    val map = mapOf<String, Tech>(
        TECH_GOBLIN_PLAGUE_ABILITY to RatPlagueTech()
    )
}

class RatPlagueTech : Tech(
    TECH_GOBLIN_PLAGUE_ABILITY,
    goblin,
    TargetType.BoughtUnit,
    UnitIds.GOBLIN_RAT_KING,
    time = 3,
    listOf(GOBLIN_SORCERY_TOWER),
    { target ->
        target as UnitActor
        if (target.cAbilities == null) target.add(AbilitiesComponent())
        if (target.abilities.none { it is PlagueAbility })
            target.abilities.add(PlagueAbility())
    }
)