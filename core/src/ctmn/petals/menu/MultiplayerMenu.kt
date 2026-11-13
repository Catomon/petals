package ctmn.petals.menu

import ctmn.petals.PlatformHooks
import ctmn.petals.screens.MenuScreen
import ctmn.petals.screens.multiplayerMenuFactory

/**
 * The screens that need the networking stack, kept together so the menu itself can stay offline.
 * Only a platform that has networking installs them, see [enableMultiplayerMenu].
 */
class MultiplayerMenu(private val menuScreen: MenuScreen) {
    val lobbiesStage = LobbiesStage(menuScreen, this)
    val serverConnectStage = ServerConnectStage(menuScreen, this)
    val mpLobbyVariantsStage = MpLobbyVariantsStage(menuScreen, this)
}

/** Called by the desktop and Android launchers to turn on everything that needs networking. */
fun enableNetworking() {
    multiplayerMenuFactory = { MultiplayerMenu(it).mpLobbyVariantsStage }
    PlatformHooks.onDispose = { ctmn.petals.net.LanHost.stop() }
    PlatformHooks.startOnlineAutoJoin = { game, host, port ->
        ctmn.petals.multiplayer.OnlineAutoJoin.start(game, host, port)
    }
}
