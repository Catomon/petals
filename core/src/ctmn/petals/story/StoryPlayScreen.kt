package ctmn.petals.story

import ctmn.petals.GameConst
import ctmn.petals.GameConst.PLAY_CAMERA_ZOOM_OUT_MAX_STORY
import ctmn.petals.MenuScreen
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.PetalsGame
import ctmn.petals.playscreen.GameEndCondition
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.utils.fadeOut
import ctmn.petals.widgets.StageCover
import ctmn.petals.GameConst.PLAY_CAMERA_ZOOM_STORY

class StoryPlayScreen(
    game: PetalsGame,
    val story: Story,
) : PlayScreen(game) {

    private var currentScenario: Scenario

    init {
        // Set up story
        //init scenarios
        if (!story.areScenariosInitialized)
            story.initScenarios()

        if (story.scenarios.isEmpty) throw IllegalStateException("Story does not contain scenarios")

        friendlyFire = story.storySave.friendly_fire

        story.applySave()

        //
        currentScenario = story.currentScenario

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
        if (GameConst.IS_RELEASE && story.storySave.progress == story.scenarios.size)
            game.screen = MenuScreen(game)

        when (gameEndCondition.result) {
            GameEndCondition.Result.WIN -> {
                story.onScenarioOverSave()

                val storySave = story.storySave

                storySave.friendly_fire = friendlyFire

                SavesManager.save(storySave)
            }

            GameEndCondition.Result.LOSE -> {

            }

            else -> {}
        }

        // fade in and change screen
        guiStage.addActor(StageCover().fadeInAndThen(OneAction {
            story.initScenarios()
            game.screen = StoryPlayScreen(game, story)
        }))
    }
}
