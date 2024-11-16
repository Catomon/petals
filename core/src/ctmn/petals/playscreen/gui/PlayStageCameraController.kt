package ctmn.petals.playscreen.gui

import com.badlogic.gdx.graphics.OrthographicCamera
import ctmn.petals.Const
import ctmn.petals.player.Player
import ctmn.petals.playstage.*
import ctmn.petals.utils.*
import kotlin.math.max

class PlayStageCameraController(val playStage: PlayStage) {

    val camera = playStage.camera as OrthographicCamera

    private val movement = Movement().apply {
        deceleration.x = 1500f
        deceleration.y = 1500f
        maxVelocity.x = 1000f
        maxVelocity.y = 1000f
    }

    var up = false
    var down = false
    var left = false
    var right = false
    private val dAcceleration = 3000f

    var lock = false

    /** makes so u can't move camera further [maxDistFromUnit] from [playerId] units */
    var lockToPlayerUnitArea = false
    var playerId = Player.BLUE
    var maxDistFromUnit = 320f

    var maxZoomOut = 320f //if 0f map sized zoom out
    var maxZoomIn = 240f

    private val rumble = Rumble()

    fun update(delta: Float) {
        if (!lock) {
            movement.acceleration.setZero()
            when {
                up -> movement.acceleration.y = dAcceleration
                down -> movement.acceleration.y = -dAcceleration
            }
            when {
                left -> movement.acceleration.x = -dAcceleration
                right -> movement.acceleration.x = dAcceleration
            }

            movement.update(delta)
            camera.position.x += movement.dirVelocity.x
            camera.position.y += movement.dirVelocity.y
        }

        limitPosition()

        camera.setPosition(camera.position.x + rumble.pos.x, camera.position.y + rumble.pos.y)

        camera.update()
    }

    fun shake(power: Float = 3f, duration: Float = 0.5f) {
        rumble.rumble(power, duration)
    }

    fun limitPosition() {
        val camLastX = camera.position.x
        val camLastY = camera.position.y

        with(playStage) {
            val camera = camera as OrthographicCamera
            val camWidth = camera.viewportWidth * camera.zoom
            val camHeight = camera.viewportHeight * camera.zoom
            val offset = 4 * camera.zoom
            if (camWidth < mapWidth()) {
                if (camera.cornerX() < -offset)
                    camera.position.x = camWidth / 2 - offset // left
                else if (camera.cornerX() + camWidth > mapWidth() + offset)
                    camera.position.x = mapWidth() + offset - camWidth / 2 // right
            } else {
                camera.position.x = mapWidth() / 2 // center cam x if its width > map width
            }
            if (camHeight < mapHeight()) {
                if (camera.cornerY() < -offset)
                    camera.position.y = camHeight / 2 - offset // bot
                else if (camera.cornerY() + camHeight > mapHeight() + offset)
                    camera.position.y = mapHeight() - camHeight / 2 + offset //top
            } else {
                camera.position.y = mapHeight() / 2 // center cam x if its height > map height
            }
        }

        if (camLastX != camera.position.x)
            movement.velocity.x = 0f
        if (camLastY != camera.position.y)
            movement.velocity.y = 0f

        if (lockToPlayerUnitArea) {
            val closest = playStage.getUnitsOfPlayer(playerId)
                .minByOrNull { tiledDst(it.tiledX, it.tiledY, camera.position.x.tiled(), camera.position.y.tiled()) }

            if (closest != null) {
                if (camera.position.x - closest.centerX > maxDistFromUnit)
                    camera.position.x = closest.centerX + maxDistFromUnit
                else
                    if (camera.position.x - closest.centerX < -maxDistFromUnit)
                        camera.position.x = closest.centerX - maxDistFromUnit

                if (camera.position.y - closest.centerY > maxDistFromUnit)
                    camera.position.y = closest.centerY + maxDistFromUnit
                else
                    if (camera.position.y - closest.centerY < -maxDistFromUnit)
                        camera.position.y = closest.centerY - maxDistFromUnit
            }
        }
    }

    fun zoom(z: Float) {
        camera.zoom += z
        val offset = 12 * camera.zoom
        when {
            z > 0 -> {
                if (camera.viewportWidth * camera.zoom > playStage.mapWidth() + offset &&
                    camera.viewportHeight * camera.zoom > playStage.mapHeight() + offset
                ) {
                    camera.zoom -= z
                }
            }

            z < 0 -> {
                if (camera.viewportWidth * camera.zoom < 240f &&
                    camera.viewportHeight * camera.zoom < 240f
                ) {
                    camera.zoom -= z
                }
            }
        }
    }

    fun zoomUp(z: Float) {
        val maxZoomOut = if (Const.DEBUG_MODE) 999999f else this.maxZoomOut

        camera.zoom += z

        val offset = 12 * camera.zoom + 32

        fun zoomToMapSizeLimit() {
            if (camera.viewportWidth * camera.zoom > playStage.mapWidth() + offset &&
                camera.viewportHeight * camera.zoom > playStage.mapHeight() + offset
            ) {
                camera.zoom -= z
            }
        }

        when {
            z > 0 -> {
                if (maxZoomOut == 0f) { // if didn't set maxZoomOut, limit it to map size
                    zoomToMapSizeLimit()
                } else { // if did set, limit to it or map size if limit is larger than map size
                    if (camera.viewportWidth * camera.zoom > maxZoomOut + offset &&
                        camera.viewportHeight * camera.zoom > maxZoomOut + offset
                    ) {
                        camera.zoom -= z
                    }

//                    if (playStage.mapWidth() <= maxZoomOut || playStage.mapHeight() <= maxZoomOut) {
//                        zoomToMapSizeLimit()
//                    } else {
//                        if (camera.viewportWidth * camera.zoom > maxZoomOut + offset &&
//                            camera.viewportHeight * camera.zoom > maxZoomOut + offset
//                        ) {
//                            camera.zoom -= z
//                        }
//                    }
                }
            }

            z < 0 -> {
                if (camera.viewportWidth * camera.zoom < maxZoomIn &&
                    camera.viewportHeight * camera.zoom < maxZoomIn
                ) {
                    camera.zoom -= z
                }
            }
        }
    }
}
