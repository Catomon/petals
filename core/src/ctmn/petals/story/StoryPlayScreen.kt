package ctmn.petals.story

import ctmn.petals.Const.PLAY_CAMERA_ZOOM_OUT_MAX_STORY
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.PetalsGame
import ctmn.petals.playscreen.GameEndCondition
import ctmn.petals.utils.fadeOut
import ctmn.petals.Const.PLAY_CAMERA_ZOOM_STORY
import ctmn.petals.playscreen.gui.StoryGameOverMenu

class StoryPlayScreen(
    game: PetalsGame,
    val story: Story,
    scenario: Scenario? = null,
) : PlayScreen(game) {

    var currentScenario: Scenario

    init {
        // Set up story
        //init scenarios
        if (!story.scenariosAdded)
            story.addScenarios()

        if (story.isEmpty) throw IllegalStateException("Story does not contain any scenarios")

        friendlyFire = story.storySave.friendly_fire

        //
        currentScenario =
            scenario ?: (story.createNextUndoneScenario()
                ?: throw IllegalStateException("currentScenario is null cuz param 'scenario' is null and no unfinished scenarios found"))

        story.applySave(currentScenario)

        // Step 1: init map, add units and players, set up gameEndCondition
        currentScenario.createLevel(this)

        turnManager.players.addAll(currentScenario.players)
        turnManager.currentPlayer = currentScenario.player ?: currentScenario.players.first()

        gameEndCondition = currentScenario.gameEndCondition

        // Step 1.5: set localPlayer
        localPlayer = currentScenario.player ?: currentScenario.players.first()

        // Step 2: initGui
        initGui()
        guiStage.playStageCamera.zoom = PLAY_CAMERA_ZOOM_STORY
        playStageCameraController.maxZoomOut = PLAY_CAMERA_ZOOM_OUT_MAX_STORY
        playStageCameraController.lockToPlayerUnitArea = true

        guiStage.abilitiesPanel.rememberUnit = true

        // Step 3: make tasks, triggers etc.
        currentScenario.makeScenario(this)
        currentScenario.scenarioCreated(this)

        // fade out
        guiStage.fadeOut()

        ready()
    }

    override fun onGameOver() {
        fun onWin() {
            story.onScenarioOverSave(currentScenario)
            val storySave = story.storySave
            storySave.friendly_fire = friendlyFire
            SavesManager.save(storySave)
        }

        when (gameEndCondition.result) {
            GameEndCondition.Result.HAS_WINNER -> {
                if (gameEndCondition.winners.contains(localPlayer.id))
                    onWin()
            }

            GameEndCondition.Result.WIN -> {
                onWin()
            }

            GameEndCondition.Result.LOSE -> {

            }

            else -> {}
        }

        guiStage.addActor(StoryGameOverMenu(currentScenario.result, this))
    }
}
