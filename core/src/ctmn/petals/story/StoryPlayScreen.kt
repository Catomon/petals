package ctmn.petals.story

import ctmn.petals.Const
import ctmn.petals.Const.PLAY_CAMERA_ZOOM_OUT_MAX_STORY
import ctmn.petals.screens.MenuScreen
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.PetalsGame
import ctmn.petals.playscreen.GameEndCondition
import ctmn.petals.actors.actions.OneAction
import ctmn.petals.utils.fadeOut
import ctmn.petals.widgets.StageCover
import ctmn.petals.Const.PLAY_CAMERA_ZOOM_STORY

class StoryPlayScreen(
    game: PetalsGame,
    val story: Story,
    var currentScenario: Scenario? = null
) : PlayScreen(game) {

    init {
        // Set up story
        //init scenarios
        if (!story.areScenariosInitialized)
            story.initScenarios()

        if (story.scenarios.isEmpty) throw IllegalStateException("Story does not contain scenarios")

        friendlyFire = story.storySave.friendly_fire

        story.applySave()

        //
        if (currentScenario == null)
            currentScenario = story.currentScenario

        val currentScenario = currentScenario!!

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
        super.onGameOver()
        //todo show continue / end buttons

//        if (Const.IS_RELEASE && story.storySave.progress == story.scenarios.size) {
//            game.screen = MenuScreen(game)
//        }

        fun onWin() {
            story.onScenarioOverSave()

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

        // fade in and change screen
        guiStage.addActor(StageCover().fadeInAndThen(OneAction {
//            story.initScenarios()
//            game.screen = StoryPlayScreen(game, story)

            game.screen = MenuScreen().apply {
                stage = levelsStage
                stage.fadeOut()
            }
        }))
    }
}
