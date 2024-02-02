package ctmn.petals.playscreen.events

import ctmn.petals.playscreen.commands.Command
import com.badlogic.gdx.scenes.scene2d.Event

class CommandAddedEvent(val command: Command) : Event()