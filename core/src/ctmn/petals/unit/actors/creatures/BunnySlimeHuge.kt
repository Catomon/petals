package ctmn.petals.unit.actors.creatures

import ctmn.petals.sim.Battle
import ctmn.petals.tile.isImpassable
import ctmn.petals.unit.*
import ctmn.petals.unit.abilities.JumpAbility
import ctmn.petals.unit.component.*
import ctmn.petals.utils.getSurroundingTiles
import kotlin.random.Random

class BunnySlimeHuge : UnitActor(
    UnitComponent(
        "slime_huge",
        300,
        20,
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
        add(MatchUpBonusComponent())
        add(TraitComponent(fireVulnerability = 2f))
        add(LeaderComponent(Random.nextInt(100000, 999999), 2, 5, 5, 4, false))

        mana = 100

        hitSounds = arrayOf("slime_hit.ogg")
    }

    override fun loadAnimations() {
        super.loadAnimations()

        airborneAnimation = createAnimation("slime_huge_airborne", 0.30f)
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

        override fun activate(battle: Battle, unitCaster: UnitActor, tileX: Int, tileY: Int): Boolean {
            val done = super.activate(battle, unitCaster, tileX, tileY)

            if (done && slimeSpawnsLeft > 0) {
                val freeTiles = battle.field.getSurroundingTiles(tileX, tileY)
                freeTiles.removeAll { it.isImpassable() }
                for (tile in freeTiles) {
                    val slime = BunnySlimeLing()
                    slime.setPosition(tile.tiledX, tile.tiledY)
                    slime.playerId = playerId
                    slime.teamId = teamId
                    slime.followerOf(this@BunnySlimeHuge, true)
                    slime.actionPoints = 0

                    battle.field.addGameActor(slime)
                    battle.onUnitSummonedView(slime)
                }

                slimeSpawnsLeft--
            }

            return done
        }
    }
}