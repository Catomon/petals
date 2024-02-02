package ctmn.petals.playscreen.seqactions

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.gui.widgets.StoryDialog

class DialogAction(val storyDialog: StoryDialog) : SeqAction() {

    override fun update(deltaTime: Float) {
        isDone = storyDialog.stage == null
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        playScreen.guiStage.addActor(storyDialog)

        return true
    }
}