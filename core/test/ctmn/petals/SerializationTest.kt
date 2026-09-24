package ctmn.petals

import com.badlogic.ashley.core.Component
import ctmn.petals.tile.components.BuildingComponent
import ctmn.petals.tile.components.PlayerIdComponent
import ctmn.petals.unit.TerrainPropsPack
import ctmn.petals.unit.actors.fairies.FairySword
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.component.ShopComponent
import ctmn.petals.unit.component.TerrainPropComponent
import ctmn.petals.unit.component.UnitComponent
import ctmn.petals.unit.cUnit
import ctmn.petals.unit.health
import ctmn.petals.unit.playerId
import ctmn.petals.utils.serialization.SyncedComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Guards the serialization refactor: components decide if they are synced via the
 * [SyncedComponent] marker, and a unit must survive a to/from JSON round trip.
 */
class SerializationTest {

    @Test
    fun syncedMarkerIsOptIn() {
        val unit: Component = UnitComponent("x", 1, 0, 1, 1)
        val playerId: Component = PlayerIdComponent(1)
        val building: Component = BuildingComponent("x", 1, 1)
        assertTrue(unit is SyncedComponent)
        assertTrue(playerId is SyncedComponent)
        assertTrue(building is SyncedComponent)

        // unit-class / terrain / shop data should never be persisted
        val shop: Component = ShopComponent(1)
        val terrainProps: Component = TerrainPropComponent(TerrainPropsPack.foot)
        assertFalse(shop is SyncedComponent)
        assertFalse(terrainProps is SyncedComponent)
    }

    @Test
    fun unitJsonRoundTripPreservesState() {
        val unit = FairySword()
        unit.playerId = 2
        unit.health = 77
        unit.actionPoints = 1
        unit.name = "test-unit"
        val jsonName = unit.cUnit.name

        val json = unit.toJsonObject()

        // Synced components are keyed by their fully qualified class name.
        assertTrue(
            json.has(UnitComponent::class.java.name),
            "expected UnitComponent to be serialized under its FQN"
        )

        val restored = FairySword()
        restored.fromJsonObject(json)

        assertEquals(jsonName, restored.cUnit.name)
        assertEquals("test-unit", restored.name)
        assertEquals(unit.health, restored.health)
        assertEquals(unit.playerId, restored.playerId)
        assertEquals(unit.actionPoints, restored.actionPoints)
    }
}
