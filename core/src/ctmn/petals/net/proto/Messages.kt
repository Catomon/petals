package ctmn.petals.net.proto

/**
 * Wire messages for the dedicated server.
 *
 * Every frame is a JSON [Envelope]: a `type`, an optional `requestId` used to correlate a
 * reply or a rejection, and a `payload` holding the JSON of the concrete message below.
 */
class Envelope {
    var type: String = ""
    var requestId: String = ""
    var payload: String = ""
}

object Msg {
    const val HELLO = "hello"
    const val WELCOME = "welcome"
    const val ERROR = "error"

    const val LIST_LOBBIES = "list_lobbies"
    const val LOBBY_LIST = "lobby_list"
    const val CREATE_LOBBY = "create_lobby"
    const val JOIN_LOBBY = "join_lobby"
    const val LEAVE_LOBBY = "leave_lobby"
    const val READY = "ready"
    const val START = "start"
    const val LOBBY_STATE = "lobby_state"

    const val QUEUE_JOIN = "queue_join"
    const val QUEUE_LEAVE = "queue_leave"
    const val QUEUE_STATUS = "queue_status"

    const val MATCH_FOUND = "match_found"
    const val MATCH_COMMAND = "match_command"
    const val MATCH_COMMAND_BROADCAST = "match_command_broadcast"
    const val COMMAND_REJECTED = "command_rejected"
    const val SNAPSHOT_REQUEST = "snapshot_request"
    const val MATCH_SNAPSHOT = "match_snapshot"
    const val MATCH_ENDED = "match_ended"

    const val PING = "ping"
    const val PONG = "pong"
}

class Hello {
    var clientId: String = ""
    var name: String = ""
    var protocol: Int = 1
}

class Welcome {
    var clientId: String = ""
    var name: String = ""
    var serverName: String = ""
    var protocol: Int = 1
}

class ErrorMsg {
    var requestId: String = ""
    var message: String = ""
}

class LobbyMember {
    var clientId: String = ""
    var name: String = ""
    var playerId: Int = -1
    var ready: Boolean = false
    var host: Boolean = false
    var connected: Boolean = true
}

class LobbyInfo {
    var id: String = ""
    var name: String = ""
    var mapId: String = ""
    var hostName: String = ""
    var players: Int = 0
    var maxPlayers: Int = 2
    var state: String = "WAITING"
}

class LobbyList {
    var lobbies: MutableList<LobbyInfo> = mutableListOf()
}

class CreateLobby {
    var name: String = ""
    var mapId: String = ""
    var maxPlayers: Int = 2
    var fogOfWar: Boolean = true
    var gameMode: String = "CRYSTALS"
}

class JoinLobby {
    var lobbyId: String = ""
}

class Ready {
    var ready: Boolean = true
}

class LobbyStateMsg {
    var id: String = ""
    var name: String = ""
    var hostClientId: String = ""
    var mapId: String = ""
    var gameMode: String = "CRYSTALS"
    var season: String = "SUMMER"
    var daytime: String = "DAY"
    var fogOfWar: Boolean = true
    var maxPlayers: Int = 2
    var state: String = "WAITING"
    var members: MutableList<LobbyMember> = mutableListOf()
}

class QueueStatus {
    var position: Int = 0
    var size: Int = 0
}

class MatchFound {
    var matchId: String = ""
    var you: Int = -1
    var mapId: String = ""
    var mapJson: String = ""
    var gameMode: String = "CRYSTALS"
    var season: String = "SUMMER"
    var daytime: String = "DAY"
    var fogOfWar: Boolean = true
    var turnTimeLimitSeconds: Int = 0
    var players: MutableList<LobbyMember> = mutableListOf()
}

class MatchCommand {
    var requestId: String = ""
    var expectedStateId: Int = 0
    var commandName: String = ""
    var commandJson: String = ""
}

class MatchCommandBroadcast {
    var stateId: Int = 0
    var playerId: Int = -1
    var commandName: String = ""
    var commandJson: String = ""
}

class CommandRejected {
    var requestId: String = ""
    var reason: String = ""
}

class MatchSnapshot {
    var stateId: Int = 0
    var snapshotJson: String = ""
}

class MatchEnded {
    var winners: MutableList<Int> = mutableListOf()
}

class Ping {
    var time: Long = 0
}

class Pong {
    var time: Long = 0
}
