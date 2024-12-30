package ctmn.petals.playscreen.triggers

import ctmn.petals.playscreen.events.TileCapturedEvent
import ctmn.petals.playstage.getCapturablesOf
import ctmn.petals.playstage.getTiles
import ctmn.petals.tile.TileActor
import ctmn.petals.tile.isCrystal
import ctmn.petals.utils.addOneTimeListener

class OnCaptureCrystalsTrigger(
    var amountNeeded: Int = -1,
) : Trigger() {

    lateinit var crystalsOnMap: List<TileActor>

    private var isDone = false

    override fun onAdded() {
        crystalsOnMap = playScreen.playStage.getTiles().filter { tile -> tile.isCrystal }
        amountNeeded = if (amountNeeded > 0) amountNeeded else crystalsOnMap.size
        playScreen.playStage.addOneTimeListener<TileCapturedEvent> {
            val playerCrystalsAmount =
                playScreen.playStage.getCapturablesOf(playScreen.localPlayer).filter { tile -> tile.isCrystal }.size
            isDone = playerCrystalsAmount == amountNeeded
            false
        }
    }

    override fun check(delta: Float): Boolean {
        return isDone
    }
}