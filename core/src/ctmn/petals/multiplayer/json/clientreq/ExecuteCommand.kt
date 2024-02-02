package ctmn.petals.multiplayer.json.clientreq

class ExecuteCommand(
    val commandName: String,
    val command: String,
    val gameStateId: Int
) : ClientRequest {

    val id: String = "execute_command"
}