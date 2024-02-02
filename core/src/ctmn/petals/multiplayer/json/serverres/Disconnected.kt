package ctmn.petals.multiplayer.json.serverres

class Disconnected(val reason: String = "shutdown") {
    val id = "disconnected"
}