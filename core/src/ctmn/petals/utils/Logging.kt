package ctmn.petals.utils

import com.badlogic.gdx.Gdx

fun echoMsg(msg: String) {
    Gdx.app.log("INFO", msg)
}

fun echoErr(msg: String) {
    Gdx.app.error("ERROR", msg)
}

fun Any.logMsg(msg: String) {
    Gdx.app.log(this::class.simpleName, msg)
}

fun Any.logErr(msg: String) {
    Gdx.app.error(this::class.simpleName, msg)
}