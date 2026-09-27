package ctmn.petals.net

import ctmn.petals.net.proto.LobbyMember

/**
 * Prints who sits where, in one format both the server and the clients use, so a lobby and the
 * match it produced can be compared line by line.
 *
 * Slot is the position and decides which map place a player owns. Player id is the identity and
 * decides the colour. They are independent, which is exactly the thing worth seeing in a log.
 */
object SeatingLog {

    /**
     * @param members seats, in any order, sorted here.
     * @param mapPlaces map place index (0 based) to the player id owning it.
     */
    fun describe(
        where: String,
        members: List<LobbyMember>,
        mapPlaces: Map<Int, Int> = emptyMap(),
    ): String {
        if (members.isEmpty()) return "$where: no seats"

        val sorted = members.sortedWith(compareBy({ it.slot }, { it.playerId }))

        val seats = sorted.joinToString(" ") { member ->
            val id = if (member.playerId > 0) "p${member.playerId}" else "empty"
            val who = when {
                member.isAi -> "AI/${member.botDifficulty}"
                member.name.isEmpty() -> "?"
                else -> member.name
            }
            "s${member.slot}=$id:$who"
        }

        val places = if (mapPlaces.isEmpty()) "" else {
            val text = mapPlaces.entries.sortedBy { it.key }
                .joinToString(" ") { "place${it.key + 1}=>p${it.value}" }
            " | $text"
        }

        return "$where: $seats$places"
    }

    /** Convenience for building a row on the fly. */
    fun row(
        slot: Int,
        playerId: Int,
        name: String,
        isAi: Boolean = false,
        difficulty: String = "normal",
    ): LobbyMember = LobbyMember().also {
        it.slot = slot
        it.playerId = playerId
        it.name = name
        it.isAi = isAi
        it.botDifficulty = difficulty
    }
}
