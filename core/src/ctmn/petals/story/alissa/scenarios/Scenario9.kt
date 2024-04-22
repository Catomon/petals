package ctmn.petals.story.alissa.scenarios

import com.badlogic.gdx.utils.Array
import ctmn.petals.ai.SimpleBot
import ctmn.petals.player.Player
import ctmn.petals.player.Team
import ctmn.petals.playscreen.*
import ctmn.petals.playscreen.gui.widgets.StoryDialog
import ctmn.petals.playscreen.triggers.UnitsDiedTrigger
import ctmn.petals.playstage.PlayStage
import ctmn.petals.playstage.getAllTiles
import ctmn.petals.playstage.getLabels
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.story.addAliceDiedGameOverTrigger
import ctmn.petals.story.alissa.AlissaScenario
import ctmn.petals.story.gameOverSuccess
import ctmn.petals.story.playScreen
import ctmn.petals.tile.TerrainNames
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.Tiles
import ctmn.petals.unit.*
import ctmn.petals.unit.actors.*
import ctmn.petals.utils.addUnit
import ctmn.petals.utils.getSurroundingTiles

class Scenario9 : AlissaScenario("Swamp", "level_9.map") {

    private val allyPlayer = Player("Ally", Player.GREEN, Team.GREEN).apply {
        allies.add(alicePlayer.teamId)
    }

    private val enemyPlayer = Player("Enemy", Player.RED, Team.RED)

    private val cherie = Cherie()

    private val allyGoblin = Goblin().player(allyPlayer)

    init {
        alicePlayer.allies.add(allyPlayer.teamId)
        enemyPlayer.allies.add(allyPlayer.teamId)
        player = alicePlayer
        gameEndCondition = NoEnd()

        players.add(player, enemyPlayer, allyPlayer)

        unitsToSaveProgressOf.add(cherie)
    }

    override fun makeScenario(playScreen: PlayScreen) {
        super.makeScenario(playScreen)

        playScreen.fogOfWarManager.drawFog = true
        playScreen.fogOfWarManager.drawDiscoverableFog = true

        // ai
        playScreen.aiManager.add(SimpleBot(enemyPlayer, playScreen).apply {
            simpleAI.permaAgro = false
            simpleAI.roamingIfNoAgro = true
            simpleAI.agroRange = 5
        })
        playScreen.aiManager.add(SimpleBot(allyPlayer, playScreen))

        //daytime
        playStage.timeOfDay = PlayStage.DayTime.DAY

        // labels
        playStage.getLabels().forEach { label ->
            when (label.labelName) {
                "alice" -> label.addUnit(alice)

                "ally" -> {
                    label.addUnit(cherie.player(alicePlayer).leader(2, 2, true))
                    for (i in 0..3) {
                        label.addUnit(CherieSpearman().player(alicePlayer).followerOf(2))
                    }
                }
                "ally_1" -> {
                    label.addUnit(allyGoblin)
                }

                "enemy_6" -> {
                    label.addUnit(Slime().player(enemyPlayer).leader(3, 2, true).position(label))
                    for (i in 0..3) {
                        label.addUnit(SlimeLing().player(enemyPlayer).followerOf(3).position(label))
                    }
                }

                "root_0" -> {
                    label.addUnit(RootTree().player(enemyPlayer))
                }
                "root_1" -> {
                    label.addUnit(RootTree().player(enemyPlayer))
                }

                "pink_0" -> {
                    label.addUnit(PinkSlimeLing().player(enemyPlayer))
                }
                "pink_1" -> {
                    label.addUnit(PinkSlimeLing().player(enemyPlayer))
                }

                "enemy_0" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
                "enemy_1" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
                "enemy_2" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
                "enemy_3" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
                "enemy_4" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
                "enemy_5" -> {
                    label.addUnit(BigToad().player(enemyPlayer))
                }
            }
        }

        playScreen {
            addAliceDiedGameOverTrigger()

            //add tree root tile at layer 2 around goblin
            for (tile in playStage.getSurroundingTiles(allyGoblin.tiledX, allyGoblin.tiledY)) {
                tile.tileComponent.terrain = TerrainNames.unwalkable

                playStage.getTile(tile.tiledX, tile.tiledY, 2)?.remove()

                val tileData = tilesData.get(Tiles.ROOT) ?: throw IllegalStateException("Tile with name '${Tiles.ROOT}' not existing")
                val rootTile = TileActor(tileData.name, tileData.terrain)
                rootTile.setPosition(tile.tiledX, tile.tiledY)
                rootTile.layer = 2

                playStage.addActor(rootTile)
            }

            ///

            queueDialogAction(
                StoryDialog.Quote("help", allyGoblin),
                StoryDialog.Quote("ok", cherie)
            )

            //todo walk cost
            //ai roam range

            addTrigger(UnitsDiedTrigger(Array<UnitActor>().also { rootTrees ->
                playStage.getUnitsOfPlayer(enemyPlayer).filter { it.selfName == UnitIds.ROOT_TREE }.forEach { rootTrees.add(it) }
            })).trigger {
                queueAction {
                    playStage.getAllTiles().forEach {
                        if (it.selfName == Tiles.ROOT) {
                            it.remove()
                            val groundTile = playStage.getTile(it.tiledX, it.tiledY)
                            groundTile?.tileComponent?.terrain = tilesData.get(groundTile!!.selfName)!!.terrain
                        }
                    }
                }

                queueDialogAction(
                    StoryDialog.Quote("Thanks lol", allyGoblin),
                )

                queueAction {
                    gameOverSuccess()
                }
            }
        }
    }
}