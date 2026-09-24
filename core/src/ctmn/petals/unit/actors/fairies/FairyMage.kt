package ctmn.petals.unit.actors.fairies

import ctmn.petals.tile.TerrainNames
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.UnitIds
import ctmn.petals.unit.UnitIds.FAIRY_MAGE
import ctmn.petals.unit.component.*

/**
 * Appearance: a fairy mage in a pointed hat.
 * Role: caster - WORK IN PROGRESS.
 * Notes: gated behind Config.WIP_CONTENT; its abilities are not implemented yet.
 */
class FairyMage : UnitActor(
    UnitComponent(
        FAIRY_MAGE,
        100,
        0,
        4,
        6
    )
) {

    init {
        add(AbilitiesComponent(25, abilities = mutableSetOf(

        )))
        add(FollowerComponent())
        add(
            AttackComponent(
                20,
                35,
                2,
                attackType = ATTACK_TYPE_ALL,
                environmentDmg = 15,
            )
        )
        add(ReloadingComponent(1))
        add(TerrainPropComponent(TerrainPropsPack.foot.copy().also {
            it[TerrainNames.hills].ad(15, 0)
            it[TerrainNames.mountains].ad(15, 5)
            it[TerrainNames.tower].ad(15, 10)
        }))
        add(MatchUpBonusComponent().apply {
            bonuses[UnitIds.ANGRY_OWL] = 25 to 0
            bonuses[UnitIds.FAIRY_BOMBER] = 25 to 0
            bonuses[UnitIds.GOBLIN_WYVERN] = 25 to 0
            bonuses[UnitIds.FAIRY_SCOUT] = 25 to 0
            bonuses[UnitIds.PIXIE] = 25 to 0
        })

        hitSounds = arrayOf("bow.ogg")
    }
}
