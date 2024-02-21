package ctmn.petals.utils

import ctmn.petals.Const

class WaveAnimation(
    var frameDuration: Float = 1f,
    var stepX: Float = 1f,
    var stepY: Float = 1f,
) {

    var frameSize = Const.TILE_SIZE.toFloat()
    var size = Const.TILE_SIZE * 2f

    var timeState = 0f

    var curStepX: Float = if (stepX > 0) 0f else if (stepX < 0) frameSize else 0f
    var curStepY: Float = if (stepY > 0) 0f else if (stepY < 0) frameSize else 0f

    fun update(delta: Float) {
        timeState += delta
        if (timeState >= frameDuration) {
            timeState -= frameDuration
            curStepX += stepX
            curStepY += stepY

            if (stepX > 0) {
                if (curStepX > frameSize)
                    curStepX = 0f
            } else if (stepX < 0){
                if (curStepX < 0)
                    curStepX = 16f
            }
            if (stepY > 0) {
                if (curStepY > frameSize)
                    curStepY = 0f
            } else if (stepY < 0){
                if (curStepY < 0)
                    curStepY = 16f
            }

            println("$curStepX : $curStepY")
        }
    }
}