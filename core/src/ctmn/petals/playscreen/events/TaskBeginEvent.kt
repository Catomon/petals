package ctmn.petals.playscreen.events

import ctmn.petals.playscreen.tasks.Task
import com.badlogic.gdx.scenes.scene2d.Event

class TaskBeginEvent(val task: Task) : Event()