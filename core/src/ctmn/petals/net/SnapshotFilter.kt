package ctmn.petals.net

import ctmn.petals.multiplayer.json.GameStateSnapshot
import ctmn.petals.sim.FogOfWar
import kotlin.Array as KArray

/**
 * Applies fog of war to a snapshot before it leaves the server.
 *
 * Enemy units outside the viewer's visibility are dropped entirely. Tiles keep their terrain
 * so the client can still render the map and draw fog, but the components that reveal state
 * (ownership, health, crystal amounts, build progress) are removed for unseen cells.
 */
object SnapshotFilter {

    private const val UNIT_COMPONENT = "ctmn.petals.unit.component.UnitComponent"
    private const val TILE_COMPONENT = "ctmn.petals.tile.components.TileComponent"

    private val HIDDEN_TILE_COMPONENTS = listOf(
        "ctmn.petals.tile.components.PlayerIdComponent",
        "ctmn.petals.tile.components.HealthComponent",
        "ctmn.petals.tile.components.CrystalsComponent",
        "ctmn.petals.tile.components.BuildingComponent",
        "ctmn.petals.tile.components.BaseBuildingComponent",
        "ctmn.petals.tile.components.CapturingComponent",
        "ctmn.petals.tile.components.DestroyingComponent",
    )

    fun apply(snapshot: GameStateSnapshot, visibility: KArray<IntArray>?, viewerTeamId: Int) {
        if (visibility == null) return

        // Enemy units the player cannot see are not sent at all.
        snapshot.units.removeIf { unit ->
            val component = unit.getAsJsonObject(UNIT_COMPONENT) ?: return@removeIf false

            val team = component.get("teamID")?.asInt ?: return@removeIf false
            val x = component.get("tiledX")?.asInt ?: return@removeIf false
            val y = component.get("tiledY")?.asInt ?: return@removeIf false

            team != viewerTeamId && !FogOfWar.isVisible(visibility, x, y)
        }

        // Tiles stay for rendering, but unseen ones stop carrying state.
        for (tile in snapshot.tiles) {
            val component = tile.getAsJsonObject(TILE_COMPONENT) ?: continue

            val x = component.get("tiledX")?.asInt ?: continue
            val y = component.get("tiledY")?.asInt ?: continue

            if (FogOfWar.isVisible(visibility, x, y)) continue

            for (key in HIDDEN_TILE_COMPONENTS) tile.remove(key)
        }
    }
}
