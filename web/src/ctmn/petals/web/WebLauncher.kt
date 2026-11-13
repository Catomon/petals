@file:JvmName("WebLauncher")

package ctmn.petals.web

import com.github.xpenatan.gdx.backends.teavm.TeaApplication
import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration
import ctmn.petals.PetalsGame

fun main() {
    val config = TeaApplicationConfiguration("canvas").apply {
        // 0 for both means fill the available space.
        width = 0
        height = 0
    }

    TeaApplication(PetalsGame(), config)
}
