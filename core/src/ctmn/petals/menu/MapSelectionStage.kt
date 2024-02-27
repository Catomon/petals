package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.editor.MAPS_FOLDER_PATH
import ctmn.petals.editor.MAP_FILE_EXTENSION
import ctmn.petals.editor.isOutdatedVersion
import ctmn.petals.screens.MenuScreen
import ctmn.petals.map.MapConverted
import ctmn.petals.map.loadMap
import ctmn.petals.playscreen.GameMode
import ctmn.petals.widgets.*
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

        val folderLocal = Gdx.files.local(MAPS_FOLDER_PATH)
        val folderInternal = Gdx.files.internal(MAPS_FOLDER_PATH)
        for (file in folderLocal.list() + folderInternal.list()) {
            if (file.extension() == "map"
                || file.extension() == MAP_FILE_EXTENSION
            ) {

                var alreadyAdded = false
                val mapConverted =
                    maps.firstOrNull { it.mapId == file.nameWithoutExtension() }?.also { alreadyAdded = true }
                        ?: loadMap(file.nameWithoutExtension())

                if (gameModeSelectBox.selected != GameMode.ALL
                    && mapConverted.gameMode != gameModeSelectBox.selected.name.toLowerCase(Locale.ROOT)
                )
                    continue

                if (!alreadyAdded)
                    maps.add(mapConverted)

                mapsList.addButton(mapConverted.mapSave.name, mapConverted)
            }
        }
    }
}