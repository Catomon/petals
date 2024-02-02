package ctmn.petals.playscreen.events

import ctmn.petals.playscreen.tasks.Task
import com.badlogic.gdx.scenes.scene2d.Event

class TaskUpdatedEvent(val task: Task) : Event()