package ctmn.petals.menu

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.editor.*
import ctmn.petals.screens.MenuScreen
import ctmn.petals.map.MapConverted
import ctmn.petals.playscreen.GameMode
import ctmn.petals.widgets.*
import ctmn.petals.widgets.newTextButton
import java.util.*

class MapSelectionStage(private val menuScreen: MenuScreen, var onResult: (map: MapConverted?) -> Unit) :
    Stage(menuScreen.viewport, menuScreen.batch) {

    private val table = VisTable()

    private val mapPreview = MapPreview()
    private val mapsList = ButtonsScrollPane()

    private val gameModeSelectBox = VisSelectBox<GameMode>().also { it.setItems(Array(GameMode.values())) }

    private val returnButton = newTextButton("Return")
    private val confirmButton = newTextButton("Confirm")

    private val maps = Array<MapConverted>()

    // private val gameModeButton = VisSelectBox<GameMode>().also {
    //        it.setItems(GameMode.ALL, GameMode.ALICE_VS_ALICE, GameMode.CASTLES)
    //    }

    private val size = 300f

    init {
        confirmButton.isDisabled = true

        mapsList.setScrollingDisabled(true, false)
        mapsList.setSize(size, size)

        mapsList.onButtonClick = {
            val mapConverted = (it.userObject as MapConverted)

            if (mapConverted.mapSave.isOutdatedVersion) {
                addNotifyWindow("Outdated map, unplayable", "Map Selection")
                confirmButton.isDisabled = true
            } else {
                mapPreview.setPreview(mapConverted)

                confirmButton.isDisabled = false
            }
        }

        returnButton.addChangeListener {
            menuScreen.stage = menuScreen.menuStage

            onResult(null)
        }

        confirmButton.addChangeListener {
            val mapConverted = mapPreview.map ?: return@addChangeListener
            onResult(mapConverted)
        }

        gameModeSelectBox.addListener {
            if (it is ChangeEvent) {
                updateList()
            }

            false
        }

        //setup table
        table.setFillParent(true)
        //table.debug = true

        with(table) {
            add(mapPreview).size(size).padTop(6f)
            row()
            add(mapsList).width(size).expandY()
            row()
            add(gameModeSelectBox).center().width(size)
            row()
            add(VisTable().apply {
                //debug = true
                add(returnButton).left()
                add().expandX()
                add(confirmButton).right()
            }).width(size)
        }

        addActor(table)

        addListener {
            if (it is ResetStateEvent) {
                maps.clear()
                updateList()
            }

            false
        }
    }

    private fun updateList() {
        //levels.clear()
        mapsList.removeItems()

        val mapItems = collectMaps()
        for (mapItem in mapItems) {
            val mapConverted = MapConverted(mapItem.mapSave)

            if (gameModeSelectBox.selected != GameMode.ALL
                && mapConverted.gameMode != gameModeSelectBox.selected.name.toLowerCase(Locale.ROOT)
            ) continue

            val sameName = mapItems.firstOrNull {
                it.mapSave.name == mapConverted.mapSave.name && it.mapSave.id != mapConverted.mapId
            }
            val suffix = when {
                sameName != null && sameName.type == MapItem.Type.DEFAULT && mapItem.type == MapItem.Type.CUSTOM -> " (2)"
                else -> ""
            }

            mapsList.addButton(mapConverted.mapSave.name + suffix, mapConverted)
        }
    }
}