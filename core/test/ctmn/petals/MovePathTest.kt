package ctmn.petals

import ctmn.petals.playstage.findMovePath
import ctmn.petals.sim.SimField
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.actionPoints
import ctmn.petals.unit.actors.fairies.FairySword
import ctmn.petals.unit.canMove
import ctmn.petals.unit.movingRange
import ctmn.petals.unit.player
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the path search behind server verified moves: it must agree with the movement grid,
 * because any disagreement only decides optimistic versus verified, and a wrong agreement in
 * the optimistic direction would let a move through that the server refuses.
 */
class MovePathTest {

    private fun flatField(size: Int = 5): SimField {
        val field = SimField()
        for (x in 0 until size) {
            for (y in 0 until size) {
                field.addGameActor(TileActor("grass", "grass", 1, x, y))
            }
        }
        return field
    }

    private fun soldier(field: SimField, x: Int, y: Int, playerId: Int): FairySword {
        return FairySword().player(playerId).apply {
            actionPoints = 99
            setPosition(x, y)
            field.addGameActor(this)
        }
    }

    @Test
    fun pathIsAnOrthogonalWalkToTheDestination() {
        val field = flatField()
        val unit = soldier(field, 0, 0, 1)

        val path = field.findMovePath(unit, 2, 1)
        assertNotNull(path, "open ground within range must have a route")
        assertEquals(2 to 1, path.last())

        var at = 0 to 0
        for (step in path) {
            assertEquals(1, abs(step.first - at.first) + abs(step.second - at.second))
            at = step
        }
    }

    @Test
    fun stayingPutIsAnEmptyRouteButNotAMove() {
        val field = flatField()
        val unit = soldier(field, 0, 0, 1)

        // The unit's own tile answers two different questions: the route exists and is empty,
        // but stepping nowhere is not a move.
        assertEquals(emptyList(), field.findMovePath(unit, 0, 0))
        assertEquals(false, unit.canMove(0, 0))
    }

    @Test
    fun agreesWithTheMovementGridOnOpenGround() {
        val field = flatField()
        val unit = soldier(field, 0, 0, 1)

        for (x in 0 until 5) {
            for (y in 0 until 5) {
                if (x == 0 && y == 0) continue
                assertEquals(
                    unit.canMove(x, y), field.findMovePath(unit, x, y) != null,
                    "path search and the grid disagree at $x,$y (range ${unit.movingRange})"
                )
            }
        }
    }

    @Test
    fun agreesWithTheMovementGridNextToEnemies() {
        val field = flatField()
        val unit = soldier(field, 0, 0, 1)
        soldier(field, 1, 1, 2)
        soldier(field, 3, 3, 2)

        for (x in 0 until 5) {
            for (y in 0 until 5) {
                if (x == 0 && y == 0) continue
                assertEquals(
                    unit.canMove(x, y), field.findMovePath(unit, x, y) != null,
                    "path search and the grid disagree at $x,$y with enemies around"
                )
            }
        }
    }

    @Test
    fun enemyOnTheDestinationBlocks() {
        val field = flatField()
        val unit = soldier(field, 0, 0, 1)
        soldier(field, 2, 0, 2)

        assertNull(field.findMovePath(unit, 2, 0), "a held tile is not a destination")
        assertTrue(field.findMovePath(unit, 1, 0) != null || !unit.canMove(1, 0))
    }
}
