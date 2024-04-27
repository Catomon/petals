package ctmn.petals.utils

import com.badlogic.gdx.Gdx

fun Any.log(msg: String) {
    Gdx.app.log(this::class.simpleName, msg)
}

fun Any.err(msg: String) {
    Gdx.app.error(this::class.simpleName, msg)
}