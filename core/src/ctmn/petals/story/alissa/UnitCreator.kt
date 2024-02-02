package ctmn.petals.story.alissa

import ctmn.petals.unit.*
import ctmn.petals.unit.actors.Alice

/** Making units specifically for story */
object CreateUnit {
    val alice get() = Alice().apply {
        summonAbility.cost = 5
        mana = 100

        summoner.units.add(UnitIds.DOLL_PIKE)

        cSummoner!!.giveAP = true
    }
}

