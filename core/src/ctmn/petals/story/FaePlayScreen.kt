package ctmn.petals.story

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.PetalsGame
import ctmn.petals.playscreen.GameEndCondition
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.utils.fadeOut
import ctmn.petals.widgets.StageCover
import ctmn.petals.Const.PLAY_CAMERA_ZOOM_STORY
import ctmn.petals.playscreen.listeners.CreditsForKillingSlimes
import ctmn.petals.story.faesandfoes.FaesScenario

class FaePlayScreen(
    game: PetalsGame,
) : PlayScreen(game) {

    private var progress = 0
    private var currentScenario: Scenario

    init {
        playStage.addListener(CreditsForKillingSlimes(this))

        //init scenario
        currentScenario = FaesScenario()

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
        playStageCameraController.maxZoomOut = 0f //PLAY_CAMERA_ZOOM_OUT_MAX_STORY
        playStageCameraController.lockToPlayerUnitArea = false
        guiStage.creditsLabel.isVisible = true

        guiStage.abilitiesPanel.rememberUnit = true

        // Step 3: make tasks, triggers etc.
        currentScenario.makeScenario(this)
        currentScenario.scenarioCreated(this)

        // fade out
        guiStage.fadeOut()

        ready()
    }

    private fun saveProgress() {
//        story.onScenarioOverSave()
//
//        val storySave = story.storySave
//
//        storySave.friendly_fire = friendlyFire
//
//        SavesManager.save(storySave)
    }

    override fun onGameOver() {
//        if (Const.IS_RELEASE && story.storySave.progress == story.scenarios.size) //like if endgame?
//            game.screen = MenuScreen(game)

        when (gameEndCondition.result) {
            GameEndCondition.Result.WIN -> {
                saveProgress()
            }

            GameEndCondition.Result.LOSE -> {

            }

            else -> {}
        }

        // fade in and change screen
        guiStage.addActor(StageCover().fadeInAndThen(OneAction {
            game.screen = FaePlayScreen(game)
        }))
    }
}
