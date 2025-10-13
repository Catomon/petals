package ctmn.petals.bot.mediocre

import ctmn.petals.player.Species

sealed class Task(
    val priority: Int = 50
) {
    class BuildBaseTask : Task(85)

    class StartBuildingTask(val building: Species.Building) : Task(75)

    class CaptureCrystalTask(val crystalTileId: String) : Task(80)
}