package ctmn.petals.multiplayer

import ctmn.petals.ai.EasyAiDuelBot
import ctmn.petals.multiplayer.json.GameStateSnapshot
import ctmn.petals.playscreen.PlayScreen
import ctmn.petals.playscreen.events.NextTurnEvent
import ctmn.petals.tile.TileActor

fun applyGameStateToPlayScreen(
    snapshot: GameStateSnapshot,
    playScreen: PlayScreen = PlayScreen(),
    initView: Boolean = true,
    isMultiplayer: Boolean = false
): PlayScreen {

    playScreen.initView = initView
    playScreen.playStage.initView = initView

    playScreen.turnManager.players.clear()
    playScreen.aiManager.aiPlayers.clear()

    playScreen.actionManager.clear()
    playScreen.commandManager.clearQueue()

    playScreen.playStage.clearGameActors()

    playScreen.gameStateId = snapshot.gameStateId

    if (!isMultiplayer)
        playScreen.localPlayer = snapshot.localPlayer

    playScreen.turnManager.players.addAll(snapshot.players)
    playScreen.turnManager.turn = snapshot.turn
    playScreen.friendlyFire = snapshot.friendlyFire
    snapshot.aiPlayers.forEach { playScreen.aiManager.add(EasyAiDuelBot(playScreen.turnManager.getPlayerById(it) ?: return@forEach, playScreen)) }

    playScreen.playStage.idCounter = snapshot.idCounter

    playScreen.randomSeed = snapshot.randomSeed
    playScreen.randomCount = snapshot.randomCount

    with (playScreen) {
        for (tileJson in snapshot.tiles) {
            playStage.addActor(TileActor().apply { fromJsonObject(tileJson) })
        }

        for (unitJson in snapshot.units) {
            val unit = unitsData.get(unitJson["name"].asString).apply {
                fromJsonObject(unitJson)
            }
            playStage.addActor(unit)
        }

        playScreen.initGui()
        fireEvent(NextTurnEvent(turnManager.previousPlayer, turnManager.currentPlayer))
    }

    return playScreen
}

fun PlayScreen.createSnapshot(): GameStateSnapshot {
    return GameStateSnapshot(this)
}