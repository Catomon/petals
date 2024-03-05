package ctmn.petals.unit.actors

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.playStage
import ctmn.petals.tile.isImpassable
import ctmn.petals.unit.*
import ctmn.petals.unit.abilities.JumpAbility
import ctmn.petals.unit.component.*
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.utils.getSurroundingTiles
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import kotlin.random.Random

class SlimeHuge : UnitActor(
    UnitComponent(
        "slime_huge",
        200,
        15,
        3,
        6
    )
) {

    init {
        add(
            AttackComponent(
                25,
                35,
                1
            )
        )
        add(AbilitiesComponent(
            SlimeJumpAbility()
        ))
        add(TerrainPropComponent(TerrainPropsPack.slime))
        add(TerrainPropComponent(TerrainPropsPack.slime))
        add(MatchUpBonusComponent())
        add(TraitComponent(fireVulnerability = 2f))
        add(LeaderComponent(Random.nextInt(100000, 999999), 2, 5, 5, 4, false))

        mana = 30
    }

    //todo slime component with slimeSpawnsLeft

    inner class SlimeJumpAbility : JumpAbility() {

        private var slimeSpawnsLeft = 1

        init {
            damage = 10
            pushUnits = true

            cooldown = 2
            cost = 10
        }

        override fun activate(playScreen: PlayScreen, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
            val done = super.activate(playScreen, unitCaster, tileX, tileY)

            if (done && slimeSpawnsLeft > 0) {
                for (action in actions) {
                    if (action is SequenceAction) {
                        action.addAction(OneAction {
                            val freeTiles = playStage.getSurroundingTiles(tileX, tileY)
                            freeTiles.removeAll { it.isImpassable() }
                            for (tile in freeTiles) {
                                val slime = SlimeLing()
                                slime.setPosition(tile.tiledX, tile.tiledY)
                                slime.playerId = playerId
                                slime.teamId = teamId
                                slime.followerOf(this@SlimeHuge, true)
                                slime.actionPoints = 0

                                playStage.addActor(slime)
                            }
                        })
                    }
                }

                slimeSpawnsLeft--
            }

            return done
        }
    }
}