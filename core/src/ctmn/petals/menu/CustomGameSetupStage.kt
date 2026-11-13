package ctmn.petals.menu

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.GamePref
import ctmn.petals.actors.actions.TimeAction
import ctmn.petals.assets
import ctmn.petals.bot.v2.BotFactory
import ctmn.petals.game
import ctmn.petals.map.MapConverted
import ctmn.petals.menu.GameSetupPane
import ctmn.petals.player.Player
import ctmn.petals.playscreen.CaptureBases
import ctmn.petals.playscreen.GameMode
import ctmn.petals.playscreen.GameType
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.screens.MenuScreen
import ctmn.petals.screens.PlayScreenTemplate
import ctmn.petals.strings
import ctmn.petals.widgets.LoadingCover
import ctmn.petals.widgets.addChangeListener
import ctmn.petals.widgets.addNotifyWindow
import ctmn.petals.widgets.newTextButton

/**
 * The same screen setup for a local match: pick a map, seat players and bots, then start.
 * Networked rooms use their own stages, so nothing here talks to a server.
 */
class CustomGameSetupStage(private val menuScreen: MenuScreen) :
    Stage(menuScreen.viewport, menuScreen.batch) {

    private val pane = GameSetupPane(this)

    private val localPlayer get() = pane.localPlayer
    private val playerSlots get() = pane.slots
    private val mapPreview get() = pane.mapPreview
    private val changeMapButton get() = pane.changeMapButton
    private val fogOfWarCheckbox get() = pane.fogOfWarCheckbox
    private val daytimeButton get() = pane.daytimeButton
    private val seasonButton get() = pane.seasonButton

    private val returnButton = newTextButton(strings.general.return_)
    private val confirmButton = newTextButton(strings.general.confirm).apply { isDisabled = true }

    private val windowTable = VisTable()

    private var gameMode: GameMode
        get() = pane.gameMode
        set(value) { pane.gameMode = value }

    init {
        pane.allowLocalPlayer = true
        pane.allowLocalSeatMove = false
        pane.onLobbyChanged = { refreshConfirmButton() }
        pane.onUnplayableMap = { message -> addNotifyWindow(message, "Custom Game") }

        windowTable.setFillParent(true)
        addActor(windowTable)

        // The footer goes in the pane's own 300px column, as a row after the vertical filler.
        with(pane.content) {
            row()
            add(VisTable().apply {
                width = 300f
                add(returnButton).left()
                add().expandX()
                add(confirmButton).right()
            }).width(300f)
        }

        changeMapButton.addChangeListener {
            menuScreen.stage = menuScreen.mapSelectionStage.also {
                it.onResult = { result ->
                    if (result != null) {
                        val map = result.map
                        val mode = result.gameMode

                        if (map != null) {
                            setLevel(map)

                            if (mode != null) {
                                this.gameMode = mode
                            }
                        }
                    }

                    menuScreen.stage = this
                }
            }
        }

        returnButton.addChangeListener {
            assets.tilesAtlas = assets.tilesSummerAtlas
            menuScreen.stage = menuScreen.menuStage
        }

        confirmButton.addChangeListener {
            addLoadingCoverAndStartGame()
        }

        localPlayer.clientId = GamePref.clientId
        setPlayerSlot(localPlayer, 0)
        pane.hostPlayer = localPlayer

        hostChanged()
    }

    private fun addLoadingCoverAndStartGame() {
        val loadingCover = LoadingCover()
        loadingCover.fadeOutDelay = 0.75f
        addActor(loadingCover)

        addAction(TimeAction {
            if (it < 0.1f) return@TimeAction false

            startGame()

            loadingCover.done()
            loadingCover.remove()

            if (game.screen is PlayScreen)
                (game.screen as PlayScreen).guiStage.addActor(loadingCover)

            true
        })
    }

    private fun startGame() {
        if (pane.map == null) return

        val players = Array<Player>()
        pane.players.forEach { players.add(it) }

        val playScreen = PlayScreen()
        playScreen.season = pane.season

        val ps = PlayScreenTemplate.pvp(
            menuScreen.game,
            pane.map!!,
            players,
            GameType.PVP_SAME_SCREEN,
            CaptureBases(),
            pane.gameMode,
            localPlayer,
            playScreen,
        ).apply {
            pane.slots.forEach {
                if (it.isAI)
                    botManager.add(BotFactory.create(it.player!!, this))
            }

            fogOfWarManager.drawFog = pane.fogOfWar
            playStage.timeOfDay = pane.daytime
        }
        ps.ready()

        if (ps.isReady)
            menuScreen.game.screen = ps
    }

    private fun setLevel(map: MapConverted?) {
        pane.setMap(map)
        refreshConfirmButton()
    }

    private fun setPlayerSlot(player: Player?, slotIndex: Int) = pane.setPlayerSlot(player, slotIndex)

    private fun refreshConfirmButton() {
        confirmButton.isDisabled = !pane.canStart
    }

    private fun hostChanged() {
        pane.refreshEnabled()
        refreshConfirmButton()
    }

    override fun addActor(actor: Actor?) {
        if (actor is VisWindow) {
            actor.isMovable = false
            windowTable.clear()
            windowTable.add(actor).expand().center().maxWidth(viewport.worldWidth - 10f)
            return
        }

        super.addActor(actor)
    }
}
