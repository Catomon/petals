package ctmn.petals.playscreen.tasks

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.gui.widgets.StoryDialog

class DialogTask(val storyDialog: StoryDialog) : Task() {

    override fun update(deltaTime: Float) {
        isCompleted = storyDialog.stage == null
    }

    override fun onBegin(playScreen: PlayScreen) {
        super.onBegin(playScreen)

        playScreen.guiStage.addActor(storyDialog)
    }
}