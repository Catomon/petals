package ctmn.petals.menu

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisSelectBox
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.screens.MenuScreen
import ctmn.petals.level.JsonLevel
import ctmn.petals.level.Level
import ctmn.petals.playscreen.GameMode
import ctmn.petals.widgets.ButtonsScrollPane
import ctmn.petals.widgets.MapPreview
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newTextButton
import java.util.*

class MapSelectionStage(private val menuScreen: MenuScreen, var onResult: (level: Level?) -> Unit) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val table = VisTable()

    private val mapPreview = MapPreview()
    private val mapsList = ButtonsScrollPane()

    private val gameModeSelectBox = VisSelectBox<GameMode>().also { it.setItems(Array(GameMode.values())) }

    private val returnButton = newTextButton("Return")
    private val confirmButton = newTextButton("Confirm")

    private val levels = Array<JsonLevel>()

    // private val gameModeButton = VisSelectBox<GameMode>().also {
    //        it.setItems(GameMode.ALL, GameMode.ALICE_VS_ALICE, GameMode.CASTLES)
    //    }

    private val size = 300f

    init {
        confirmButton.isDisabled = true

        mapsList.setSize(size, size)

        mapsList.onButtonClick = {
            val level = it.userObject as JsonLevel
            if (!level.actorsInitialized)
                level.initActors(menuScreen.game.assets)

            mapPreview.setPreview(level)

            confirmButton.isDisabled = false
        }

        returnButton.addChangeListener {
            menuScreen.stage = menuScreen.menuStage

            onResult(null)
        }

        confirmButton.addChangeListener {
            val level = mapPreview.level ?: return@addChangeListener
            onResult(level)

            //prepare and start screen
            //if (selectedScenarioName != null)
            //game.screen = StoryPlayScreen(game, ScenariosManager.byName(selectedScenarioName!!), ScenariosManager.storySave)
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

        with (table) {
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
                levels.clear()
                updateList()
            }

            false
        }
    }

    private fun updateList() {
        //levels.clear()
        mapsList.removeItems()

        val folder = Gdx.files.internal("maps/custom")
        for (file in folder.list()) {
            if (file.extension() == "map") {

                var alreadyAdded = false
                val level = levels.firstOrNull { it.fileName == file.nameWithoutExtension() }?.also { alreadyAdded = true } ?: JsonLevel.fromFile(file.nameWithoutExtension())

                if (gameModeSelectBox.selected != GameMode.ALL
                    && level.gameMode != gameModeSelectBox.selected.name.toLowerCase(Locale.ROOT))
                    continue

                if (!alreadyAdded)
                    levels.add(level)

                mapsList.addButton(level.name, level)
            }
        }
    }
}