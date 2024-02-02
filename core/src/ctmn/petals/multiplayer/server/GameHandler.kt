package ctmn.petals.multiplayer.server

class GameHandler(val id: String, val masterClientId: String = "HOST") {

//    private val maxPlayers: Int = 8
//
//    val players = arrayOfNulls<NetPlayer>(maxPlayers)
//    private var numOfPlayers: Int = 0
//
//    //val engine: SeEngine = SeEngine()
//
//    fun startGame() {
//
//    }
//
//    fun endGame() {
//
//    }
//
//    fun endTurn(clientId: String) : Boolean {
////        if (engine.currentPlayer.id == getPlayer(clientId)?.playerId) {
////            engine.nextTurn()
//
//            println("Player[$clientId] has ended turn in Game[$id]")
//
//            return true
//        }
//
//        return false
//    }
//
//    fun addPlayer(clientId: String) {
//        players[numOfPlayers] = NetPlayer(clientId, numOfPlayers + 1)
//        numOfPlayers++
//
//        println("Player[$clientId] has joined the Game[$id]")
//    }
//
//    fun removePlayer(clientId: String) {
//        for (i in 0..numOfPlayers)
//            if (players[i]!!.clientId == clientId)
//        numOfPlayers--
//    }
//
//    fun getPlayer(clientId: String) : NetPlayer? {
//        for (i in 0..numOfPlayers)
//            if (players[i]!!.clientId == clientId) return players[i]!!
//        return null
//    }
//
//    //TODO TEMP
//    private fun getAbility(id: Int) : Ability {
//        return HealingAbility()
//    }
//
//    fun makeMove(clientId: String, mmg: MakeMoveGson) : Boolean {
//        //todo null check for engine.getUnit
//
//        val isEndTurn = mmg.end_turn
//        val orders = mmg.orders
//        for (orderGson in orders) {
//            val order: Order =
//            when (orderGson.id) {
//                //TODO
////                "move_unit" -> MoveUnitOrder(
////                        engine.getUnitEntity(orderGson.unit_x, orderGson.unit_y)!!,
////                        orderGson.target_x, orderGson.target_y)
////                "attack" -> AttackOrder(
////                        engine.getUnitEntity(orderGson.unit_x, orderGson.unit_y)!!,
////                        engine.getUnitEntity(orderGson.target_x, orderGson.target_y)!!)
////                "use_ability" -> UseAbilityOrder(
////                        engine.getUnitEntity(orderGson.unit_x, orderGson.unit_y)!!,
////                        getAbility(orderGson.extra),
////                        orderGson.target_x, orderGson.target_y)
//                else -> continue
//            }
//
//            engine.commandManager.execute(order)
//            //todo if not executed check
//        }
//
//        if (isEndTurn)
//            endTurn(clientId)
//
//        println("Player[$clientId] has made move in Game[$id]")
//
//        return true
//    }
//
//    fun getGameState() : GameStateGson {
//        return engine.getGameState()
//    }
//
//    fun updateGameState() {
//
//    }
}
