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
    const val MOVE_SEAT = "move_seat"
    const val KICK = "kick"
    const val KICKED = "kicked"
    const val READY = "ready"
    const val START = "start"
    const val LOBBY_STATE = "lobby_state"

    const val QUEUE_JOIN = "queue_join"
    const val QUEUE_LEAVE = "queue_leave"
    const val QUEUE_STATUS = "queue_status"

    const val MATCH_FOUND = "match_found"
    const val MATCH_COMMAND = "match_command"
    const val MATCH_COMMAND_BROADCAST = "match_command_broadcast"
    const val LEAVE_MATCH = "leave_match"
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

    /** Maps this server can host, for the lobby creator to pick from. */
    var mapIds: MutableList<String> = mutableListOf()
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

    /** 0 based lobby position. -1 when unknown. */
    var slot: Int = -1

    /** A bot seat, which no client is connected to. */
    var isAi: Boolean = false

    /** Bot difficulty name, see [ctmn.petals.bot.v2.Difficulty]. */
    var botDifficulty: String = "normal"

    /** Species id, so a seat keeps the portrait the host picked instead of rerolling. */
    var species: String = ""

    /** True when this seat is past the map's player places and only watches. */
    var isSpectator = false
}

/**
 * A player asking for a different seat.
 *
 * The host arranges anybody anywhere. Anybody else may take a seat that is free or holds a bot,
 * so a bot swaps to where they were. A seat another player is sitting in is not theirs to take,
 * and the server refuses it.
 */
class MoveSeat {
    /** The seat the player wants, 0 based. */
    var slot: Int = -1
}

/**
 * The host removing somebody from their lobby. A ban also keeps them from rejoining.
 *
 * The host's own seat cannot be named: the client never offers it and the server refuses it.
 */
class Kick {
    /** The member to remove, by client id. */
    var clientId: String = ""

    /** True to also keep them out of this lobby, not just out of their seat. */
    var ban: Boolean = false
}

/** Told to the player the host just removed, so their screen does not sit on a lobby they left. */
class KickedMsg {
    var reason: String = ""
    var banned: Boolean = false
}

class LobbyInfo {
    var id: String = ""
    var name: String = ""
    var mapId: String = ""

    /** The map's display name, which is what a player recognises. */
    var mapName: String = ""
    var hostName: String = ""
    var players: Int = 0
    var maxPlayers: Int = 2
    var state: String = "WAITING"

    /** True when the host has published this lobby, so it belongs in the browser. */
    var listed: Boolean = false
}

class LobbyList {
    var lobbies: MutableList<LobbyInfo> = mutableListOf()
}

/**
 * One seat as the host arranged it.
 *
 * The slot is the position and the player id is the identity, and they are deliberately
 * independent. Colour follows the id, so a player keeps their colour when they move to another
 * slot, and a bot takes whatever id is still free.
 */
class LobbySeat {
    /** 0 based position in the lobby. */
    var slot: Int = 0

    /** The lobby player id, which is also the colour. */
    var playerId: Int = -1

    var name: String = ""
    var isAi: Boolean = false
    var botDifficulty: String = "normal"

    /** Species id, so the server creates the seat with the species the host picked. */
    var species: String = ""

    /**
     * Which connection this seat belongs to, for a human seat. It is how the server tells the
     * host's own seat from a joiner's, so the host's arrangement of people is not thrown away.
     */
    var clientId: String = ""
}

class CreateLobby {
    var name: String = ""
    var mapId: String = ""

    /**
     * The map's display name. A map id can be a file name or a UUID, so the id is not something
     * to show in the lobby browser, and the name has to travel with it.
     */
    var mapName: String = ""
    var maxPlayers: Int = 2
    var fogOfWar: Boolean = true
    var gameMode: String = "CRYSTALS"
    var season: String = "SUMMER"
    var daytime: String = "DAY"

    /**
     * Whether the lobby shows up in the browser. The host's lobby exists as soon as they open the
     * setup screen, so the arrangement can be built before anyone else sees it; publishing it is a
     * separate, deliberate press.
     */
    var listed: Boolean = false

    /**
     * The host's arrangement: which player id sits in which slot. The host includes their own
     * seat, so the server can keep their id and colour.
     */
    var seats: MutableList<LobbySeat> = mutableListOf()

    /**
     * The map itself, when the host picked one that only exists on their machine. The server
     * cannot load it by id, so it uses this instead and passes it on to every client in
     * `match_found`.
     */
    var mapJson: String = ""
}

class JoinLobby {
    var lobbyId: String = ""

    /**
     * The joining player's own species. The server has no other way to know it, and without it the
     * lobby shows that player with a freshly rolled species on every state update.
     */
    var species: String = ""
}

class Ready {
    var ready: Boolean = true
}

class LobbyStateMsg {
    var id: String = ""
    var name: String = ""
    var hostClientId: String = ""
    var mapId: String = ""

    /** The map's display name, for anything that names the map to a player. */
    var mapName: String = ""
    var gameMode: String = "CRYSTALS"
    var season: String = "SUMMER"
    var daytime: String = "DAY"
    var fogOfWar: Boolean = true
    var maxPlayers: Int = 2
    var state: String = "WAITING"
    var members: MutableList<LobbyMember> = mutableListOf()

    /**
     * The map itself, when the host picked one that only exists on their machine. Everyone in the
     * lobby needs it to show the right preview, not just the players who end up in the match.
     */
    var mapJson: String = ""
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

    /**
     * Which player owns which map place, keyed by the lobby slot they sit in. The client must
     * use this instead of deriving ownership from the map's own labels, otherwise it hands the
     * first place to player one and invents a base for somebody who is only spectating.
     */
    val mapOwnerBySlot = HashMap<Int, Int>()
}

class MatchCommand {
    var requestId: String = ""
    var expectedStateId: Int = 0
    var commandName: String = ""
    var commandJson: String = ""
}

class MatchCommandBroadcast {
    /**
     * Which match this belongs to. A client can be a member of a new match while the server is
     * still finishing an old one, and without this the two streams arrive interleaved with nothing
     * to tell them apart.
     */
    var matchId: String = ""
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
    var matchId: String = ""
    var stateId: Int = 0
    var snapshotJson: String = ""
}

class MatchEnded {
    var matchId: String = ""
    var winners: MutableList<Int> = mutableListOf()
}

class Ping {
    var time: Long = 0
}

class Pong {
    var time: Long = 0
}
