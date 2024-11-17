package ctmn.petals.menu

import com.badlogic.gdx.Screen
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.*
import ctmn.petals.*
import ctmn.petals.screens.LoadingScreen
import ctmn.petals.screens.MenuScreen
import ctmn.petals.utils.ScreenSizeChangedEvent
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.newTextButton

class SettingsStage(private val menuScreen: MenuScreen) : Stage(menuScreen.viewport, menuScreen.batch) {

    private val table = VisTable()

    private val returnButton = newTextButton(strings.general.return_)
    private val confirmButton = newTextButton(strings.general.confirm)

    private val fullscreenCB = VisCheckBox(strings.general.fullscreen)
    private val vSyncCB = VisCheckBox("vSync").addChangeListener {
        targetFpsSelectBox.isDisabled = it.isChecked
    }

    private val targetFpsSelectBox = VisSelectBox<Int>().apply {
        items = Array<Int>().apply {
            if (!Const.IS_RELEASE) add(0)
            add(240, 120, 60, 30)
        }
    }

    private val showAttachRangeCB = VisCheckBox(strings.general.show_attack_range_border)
    private val autoEndTurnCB = VisCheckBox(strings.general.auto_end_turn)
    private val showTerrainBonusCB = VisCheckBox(strings.general.show_terrain_bonus)

    private val languageSelectBox = LanguageSelectBox()
    private var langLast = GamePref.locale
    private val langChanged get() = langLast != languageSelectBox.selected.second

    private val musicSlider = VisSlider(0f, 1f, 0.1f, false)
    private val soundSlider = VisSlider(0f, 1f, 0.1f, false)

    var prevScreen: Screen? = null

    init {
        addEventListeners()
        setUpMainTable()

        returnButton.addChangeListener {
            if (prevScreen != null) {
                menuScreen.game.screen = prevScreen!!
                prevScreen = null
            } else {
                menuScreen.stage = menuScreen.menuStage
            }
        }
        confirmButton.addChangeListener {
            saveChanges()

            if (prevScreen != null) {
                menuScreen.game.screen = prevScreen!!
                prevScreen = null
            } else {
                if (langChanged)
                    menuScreen.game.screen = MenuScreen(menuScreen.game)
                else
                    menuScreen.stage = menuScreen.menuStage
            }
        }
    }

    private fun setUpMainTable() {
        with(table) {
            add(VisScrollPane(VisTable().apply {
                setFillParent(true)

                if (Const.IS_DESKTOP) {
                    add(fullscreenCB).left()
                    row()
                }
                add(vSyncCB).left()
                row()
                add(VisTable().apply {
                    add(VisLabel(strings.general.target_fps))
                    add().expandX()
                    add(targetFpsSelectBox).right()
                }).left().fillX()
                row()
                add(VisTable().apply {
                    add(VisLabel(strings.general.music))
                    add().expandX()
                    add(musicSlider)
                }).left().fillX()
                row()
                add(VisTable().apply {
                    add(VisLabel(strings.general.sound))
                    add().expandX()
                    add(soundSlider)
                }).left().fillX()
                row()
                add(showAttachRangeCB).left()
                row()
                add(autoEndTurnCB).left()
                row()
                add(showTerrainBonusCB).left()
                row()
            }).apply {
                setScrollingDisabled(true, false)
            }).expandY()

            row()

            add(VisTable().apply {
                add(VisLabel(strings.general.language))
                add().expandX()
                add(languageSelectBox).right()
            }).fillX().padLeft(15f).padRight(15f)

            row()

            if (Const.DEBUG_MODE) {
                add(VisTextButton("Reload Assets").addChangeListener {
                    game.runTexturePacker.run()
                    game.assets.clear()
                    game.screen = LoadingScreen(game)
                })

                row()
            }

            add(VisTable().apply {
                add(returnButton).left()
                add().expandX()
                add(confirmButton).right()
            }).width(300f).bottom()
        }
        addActor(table)
    }

    private fun addEventListeners() {
        addListener { event ->
            when (event) {
                is ResetStateEvent -> {
                    fullscreenCB.isChecked = GamePref.fullscreen
                    vSyncCB.isChecked = GamePref.vSync
                    targetFpsSelectBox.items.forEach {
                        if (it == GamePref.targetFps)
                            targetFpsSelectBox.selected = it
                    }

                    langLast = GamePref.locale

                    showAttachRangeCB.isChecked = GamePref.drawUnitAttackRange
                    autoEndTurnCB.isChecked = GamePref.autoEndTurn
                    showTerrainBonusCB.isChecked = GamePref.showTerrainBonus

                    languageSelectBox.items.forEach {
                        if (it.second == GamePref.locale)
                            languageSelectBox.selected = it
                    }

                    soundSlider.setValue(GamePref.soundVolume)
                    musicSlider.setValue(GamePref.musicVolume)
                }

                is ScreenSizeChangedEvent -> {
                    table.height = viewport.worldHeight
                    table.width = 300f
                    table.x = viewport.worldWidth / 2 - 300f / 2 - (table.width - 300f) / 2
                }
            }

            false
        }
    }

    private fun saveChanges() {
        GamePref.fullscreen = fullscreenCB.isChecked
        GamePref.vSync = vSyncCB.isChecked
        GamePref.targetFps = targetFpsSelectBox.selected
        GamePref.drawUnitAttackRange = showAttachRangeCB.isChecked
        GamePref.autoEndTurn = autoEndTurnCB.isChecked
        GamePref.showTerrainBonus = showTerrainBonusCB.isChecked
        GamePref.locale = languageSelectBox.selected.second
        GamePref.soundVolume = soundSlider.value
        GamePref.musicVolume = musicSlider.value
        GamePref.save()

        updateAppConfigToPrefs()
    }

    private inner class LanguageSelectBox : VisSelectBox<Pair<String, String>>() {
        init {
            items = Array<Pair<String, String>>().apply {
                add("English" to GameLocale.ENGLISH)
                add("Japanese" to GameLocale.JAPANESE)
                add("Spanish" to GameLocale.SPANISH)
                //add("Russian" to GameLocale.RUSSIAN)
            }
        }

        override fun toString(item: Pair<String, String>): String {
            return item.first
        }
    }
}