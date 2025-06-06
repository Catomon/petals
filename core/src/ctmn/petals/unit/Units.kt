package ctmn.petals.unit

import ctmn.petals.player.Player
import ctmn.petals.playscreen.selfName
import ctmn.petals.unit.actors.*
import ctmn.petals.unit.actors.fairies.*
import ctmn.petals.unit.actors.goblins.*
import ctmn.petals.unit.actors.creatures.*
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
        add(Axeman())
        add(Bowman())
        add(Catapult())
        add(Cavalry())
        add(CentaurSpear())
        add(CentaurSword())
        add(Cherie())
        add(CherieSpearman())
        add(AngryOwl())
        add(BigLivingTree())
        add(Toad())
        add(Bulgy())
        add(LivingTree())
        add(PinkSlimeLing())
        add(OldLivingTree())
        add(Slime())
        add(SlimeBig())
        add(BunnySlimeHuge())
        add(BunnySlimeLing())
        add(BunnySlimeTiny())
        add(Crossbowman())
        add(Dummy())
        add(FairyArmorSword())
        add(FairyAxe())
        add(FairyBomber())
        add(FairyBow())
        add(FairyCucumber())
        add(FairyGlaive())
        add(FairyHammer())
        add(FairyHealer())
        add(FairyHunter())
        add(FairyMage())
        add(FairyPeas())
        add(FairyPike())
        add(FairyPixie())
        add(FairyScout())
        add(FairyShield())
        add(FairySower())
        add(FairySword())
        add(FairyWaterplant())
        add(FallenKnight())
        add(FallenSpearman())
        add(FallenSwordsman())
        add(FishGuy())
        add(Goblin())
        add(GoblinBoar())
        add(GoblinBomber())
        add(GoblinBow())
        add(GoblinCatapult())
        add(GoblinCrossbow())
        add(GoblinGalley())
        add(GoblinGiant())
        add(GoblinHealer())
        add(GoblinLeader())
        add(GoblinMace())
        add(GoblinMachete())
        add(GoblinPickaxe())
        add(GoblinPike())
        add(GoblinScout())
        add(GoblinShip())
        add(GoblinSword())
        add(GoblinWolf())
        add(GoblinWyvern())
        add(Horseman())
        add(Knight())
        add(KnightLeader())
        add(Necromancer())
        add(ObjBlob())
        add(ObjRock())
        add(ObjRoot())
        add(Spearman())
        add(Swordman())
        add(VillageSpearman())
        add(VillageSwordsman())
        add(GoblinBoarPike())
        add(GoblinRatKing())
    }

    fun add(unit: UnitActor) : UnitActor {
        val unitName = unit.selfName
        map[unitName] = unit::class
        names.add(unitName)
        return unit
    }

    fun find(name: String, player: Player? = null) : UnitActor? {
        if (map[name] == null) return null

        val unitActor = map[name]!!.java.constructors.first().newInstance() as UnitActor
        unitActor.playerId = player?.id ?: -1
        unitActor.teamId = player?.teamId ?: -1

        return unitActor
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
