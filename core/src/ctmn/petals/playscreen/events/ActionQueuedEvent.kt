package ctmn.petals.playscreen.events

import ctmn.petals.playscreen.seqactions.SeqAction
import com.badlogic.gdx.scenes.scene2d.Event

class ActionQueuedEvent(val action: SeqAction) : Event()