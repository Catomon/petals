package ctmn.petals.utils

import com.badlogic.gdx.Gdx

private object LessPrint {
    const val idleTime = 1f
    var time = 0f
}

fun printLess(str: String) {
    if (LessPrint.time > LessPrint.idleTime) {
        println(str)
        LessPrint.time = 0f
    }
    LessPrint.time += Gdx.graphics.deltaTime
}