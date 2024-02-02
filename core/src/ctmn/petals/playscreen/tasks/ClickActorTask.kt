package ctmn.petals.playscreen.tasks

import ctmn.petals.playscreen.PlayScreen
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

class ClickActorTask(val actor: Actor) : Task() {

    private val clickListener = object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            super.clicked(event, x, y)

            isCompleted = true
        }
    }

    override fun onBegin(playScreen: PlayScreen) {
        super.onBegin(playScreen)



        playScreen.commandManager.stop = true
        actor.addListener(clickListener)
    }

    override fun onCompleted() {
        super.onCompleted()

        playScreen.commandManager.stop = false
        actor.removeListener(clickListener)
    }
}