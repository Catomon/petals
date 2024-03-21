package ctmn.petals.story.alissa

import ctmn.petals.unit.*
import ctmn.petals.unit.abilities.*
import ctmn.petals.unit.actors.Alice

/** Making units specifically for story */
object CreateUnit {
    val alice get() = Alice().apply {
        mana = 100

        abilities.add(MeteoriteAbility())
        abilities.add(HealingTouchAbility().apply { cost = 10 })
        abilities.add(PersonalBarrierAbility())
        abilities.add(LightningAbility())
        abilities.add(FlameAbility().apply { activationRange = 2; cost = 10 })

//        SummonAbility().apply {
//            cost = 0
//        }
//            50,
//            HealingAbility().apply {
//                activationRange = 1
//                value = 25 },
//            FlameAbility().apply {
//                activationRange = 1
//                value = 25 }
//        summonAbility.cost = 5
//        summoner.units.add(UnitIds.DOLL_PIKE)
//        cSummoner!!.giveAP = true
    }
}

