package ctmn.petals.bot.v2.util

import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.Ability
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.cAbilities
import ctmn.petals.unit.cAttack
import ctmn.petals.unit.cShop
import ctmn.petals.unit.component.MoveAfterAttackComponent
import ctmn.petals.unit.isAir
import ctmn.petals.unit.isWater
import ctmn.petals.unit.isWorker
import ctmn.petals.unit.movingRange

/** The job a unit fills in the bot's army, independent of its movement type. */
enum class UnitRole {
    WORKER, SCOUT, HEALER, ARTILLERY, RANGED, SKIRMISHER, AIR, WATER, MELEE
}

/**
 * Classifies units by battlefield job instead of raw stats.
 *
 * Movement type and anti-air capability stay separate from the role, because in this game
 * they are independent: GoblinWyvern is a LAND unit with flier movement that can attack air,
 * while FairyBomber is an AIR unit that can only attack the ground.
 */
object UnitRoles {

    private val healingAbilityNames =
        setOf("healing", "healing_touch", "health_potion", "personal_barrier")

    /** Cheap, fast, weak flyers meant for vision, not for fighting. */
    private val scoutUnitIds = setOf(UnitIds.PIXIE, UnitIds.GOBLIN_SCOUT)

    /**
     * Land units that fly for movement or hit and run but are not UNIT_TYPE_AIR. They are
     * mobile skirmishers, distinct from units that are actually airborne.
     */
    private val skirmisherIds = setOf(
        UnitIds.FAIRY_SCOUT,
        UnitIds.GOBLIN_WOLF,
        UnitIds.GOBLIN_WYVERN,
    )

    fun isHealerUnit(unit: UnitActor): Boolean {
        val abilities = unit.cAbilities?.abilities ?: return false
        return abilities.any { it.type == Ability.Type.HEALING || it.name in healingAbilityNames }
    }

    fun isScoutUnit(unit: UnitActor): Boolean {
        if (unit.selfName in scoutUnitIds) return true
        val price = unit.cShop?.price ?: return false
        return price <= 60 && unit.movingRange >= 3 && (unit.cAttack?.attackRange ?: 1) <= 1
    }

    fun isSkirmisherUnit(unit: UnitActor): Boolean {
        if (unit.selfName in skirmisherIds) return true
        return unit.get(MoveAfterAttackComponent::class.java) != null
    }

    fun of(unit: UnitActor): UnitRole {
        if (unit.isWorker) return UnitRole.WORKER
        if (isScoutUnit(unit)) return UnitRole.SCOUT
        if (isHealerUnit(unit)) return UnitRole.HEALER
        // True artillery has a dead zone: it cannot hit adjacent tiles.
        if ((unit.cAttack?.attackRangeMin ?: 0) > 0) return UnitRole.ARTILLERY
        if (unit.isWater) return UnitRole.WATER
        if (unit.isAir) return UnitRole.AIR
        if (isSkirmisherUnit(unit)) return UnitRole.SKIRMISHER
        if ((unit.cAttack?.attackRange ?: 0) >= 2) return UnitRole.RANGED
        return UnitRole.MELEE
    }
}
