package ctmn.petals.playscreen.seqactions

import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.gui.floatingLabel
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.setPositionOrNear
import ctmn.petals.unit.tiledX
import ctmn.petals.unit.tiledY

class AddUnitAction(val unit: UnitActor, val x: Int = -1, val y: Int = -1) : SeqAction() {

    private var idleTime = 0.15f

    override fun update(deltaTime: Float) {
        //TODO add animation or something

        idleTime -= deltaTime

        if (idleTime < 0) {
            playScreen.playStage.addActor(unit)

            if (x > -1 || y > -1)
                unit.setPositionOrNear(x, y)
            else
                unit.setPositionOrNear(unit.tiledX, unit.tiledY)

            playScreen.guiStage.floatingLabel("Unit: ${unit.name}")

            isDone = true
        }
    }

    override fun onStart(playScreen: PlayScreen): Boolean {
        this.playScreen = playScreen

        return true
    }
}