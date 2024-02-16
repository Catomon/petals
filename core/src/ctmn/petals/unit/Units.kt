package ctmn.petals.unit

import ctmn.petals.player.Player
import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.actors.*
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

/**
 * This class is auto-generated.
 * [ctmn.petals.utils.ClassGen.generateUnitsClass]
 */

object Units {
    private val map = HashMap<String, KClass<out UnitActor>>()
     val names = ArrayList<String>()

    init {
        add(Alice())
        add(AngryOwl())
        add(Axeman())
        add(BigEvilTree())
        add(BigToad())
        add(Bowman())
        add(Catapult())
        add(Cavalry())
        add(CentaurSpear())
        add(CentaurSword())
        add(Cherie())
        add(CherieSpearman())
        add(Crossbowman())
        add(Dummy())
        add(EvilTree())
        add(FairyAxe())
        add(FairyBow())
        add(FairyCucumber())
        add(FairyHunter())
        add(FairyPike())
        add(FairySword())
        add(FallenKnight())
        add(FallenSpearman())
        add(FallenSwordsman())
        add(FishGuy())
        add(Goblin())
        add(GoblinBoar())
        add(GoblinBow())
        add(GoblinCatapult())
        add(GoblinGiant())
        add(GoblinLeader())
        add(GoblinPike())
        add(GoblinSword())
        add(Horseman())
        add(Knight())
        add(KnightLeader())
        add(Necromancer())
        add(PinkSlimeLing())
        add(PixieUnit())
        add(RootTree())
        add(Slime())
        add(SlimeBig())
        add(SlimeHuge())
        add(SlimeLing())
        add(Spearman())
        add(Swordman())
        add(VillageSpearman())
        add(VillageSwordsman())
    }

    fun add(unit: UnitActor) : UnitActor {
        val unitName = unit.selfName
        map[unitName] = unit::class
        names.add(unitName)
        return unit
    }

    fun get(name: String, player: Player? = null) : UnitActor {
        if (map[name] == null)
            throw IllegalArgumentException("Unit with name '$name' is not found.")

        val unitActor = map[name]!!.java.constructors.first().newInstance() as UnitActor
        unitActor.playerId = player?.id ?: -1
        unitActor.teamId = player?.teamId ?: -1

        return unitActor
    }
}
