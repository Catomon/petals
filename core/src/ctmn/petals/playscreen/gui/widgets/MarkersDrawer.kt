package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.Const
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.Stage
import ctmn.petals.assets
import ctmn.petals.newPlayPuiSprite
import ctmn.petals.newPlaySprite
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.commands.BuildBaseCommand
import ctmn.petals.playscreen.events.TaskBeginEvent
import ctmn.petals.playscreen.events.TaskCompletedEvent
import ctmn.petals.playscreen.tasks.*
import ctmn.petals.playstage.getUnitsOfPlayer
import ctmn.petals.tile.cPlayerId
import ctmn.petals.unit.component.WaypointComponent
import ctmn.petals.unit.isWorker
import ctmn.petals.unit.playerIdByColor
import ctmn.petals.utils.*

class MarkersDrawer(val playScreen: PlayScreen) : Actor() {

    private val taskManager = playScreen.taskManager

    private val waypointMark = assets.atlases.getRegion("gui/waypoint_mark")
    private val waypointAnimation = RegionAnimation(0.1f, assets.atlases.findRegions("gui/animated/waypoint_mark"))
    private val sprite = newPlayPuiSprite(waypointMark)

    override fun act(delta: Float) {
        super.act(delta)

        waypointAnimation.update(delta)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        for (task in taskManager.getTasks()) {
            when (task) {
                is MoveUnitTask -> {
                    if (playScreen.guiStage.selectedUnit == task.unit) {
                        setPosition(task.x.unTiled() + Const.TILE_SIZE / 2, task.y.unTiled() + Const.TILE_SIZE / 2)
                    } else {
                        setPosition(task.unit.centerX, task.unit.centerY)
                    }

                    drawMarker(batch)
                }

                is MoveAnyUnitTask -> {
                    setPosition(task.x.unTiled() + Const.TILE_SIZE / 2, task.y.unTiled() + Const.TILE_SIZE / 2)
                    drawMarker(batch)
                }

                is CaptureCrystalsTask -> {
                    for (tile in task.crystalsOnMap) {
                        if (tile.cPlayerId?.playerId != playScreen.localPlayer.id) {
                            setPosition(tile.centerX, tile.centerY)
                            drawMarker(batch)
                        }
                    }
                }

                is ExecuteCommandTask -> {
                    if (task.commandClass == BuildBaseCommand::class) {
                        for (unit in playScreen.playStage.getUnitsOfPlayer(playScreen.localPlayer)) {
                            if (unit.isWorker) {
                                setPosition(unit.centerX, unit.centerY)
                                drawMarker(batch)
                            }
                        }
                    }
                }
            }
        }

        playScreen.guiStage.selectedUnit?.let { unit ->
            unit.get(WaypointComponent::class.java)?.let { waypoint ->
                setPosition(waypoint.tileX.unTiled() + Const.TILE_SIZE_HALF, waypoint.tileY.unTiled() + Const.TILE_SIZE_HALF)
                drawMarker(batch)
            }
        }

        if (playScreen.guiStage.isSettingWaypoint) {
            val hover = playScreen.guiStage.tileSelectionDrawer.hoveringSprite
            setPosition(hover.centerX(), hover.centerY())
            drawMarker(batch)
        }
    }

    override fun positionChanged() {
        super.positionChanged()

        sprite.setPositionByCenter(x, y)
    }

    fun drawMarker(batch: Batch) {
        sprite.setRegion(waypointMark)
        sprite.draw(batch)
        sprite.setRegion(waypointAnimation.currentFrame)
        sprite.draw(batch)
    }
}