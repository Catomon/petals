package ctmn.petals.editor

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import kotlin.math.round

const val EDITOR_VERSION = "0.1"

const val MAP_MIN_VERSION = "0.1"

const val MAP_FILE_EXTENSION = "ptmap"
const val MAPS_FOLDER_PATH = "maps/custom"

val IS_MOBILE get() = Gdx.app.type == Application.ApplicationType.Android
val IS_DESKTOP get() = Gdx.app.type == Application.ApplicationType.Desktop
val IS_PORTRAIT get() = Gdx.graphics.width < Gdx.graphics.height

private const val GUI_SCALE_MOBILE_LANDSCAPE = 1.25f
private const val GUI_SCALE_MOBILE_PORTRAIT = 1.5f
private const val GUI_SCALE_PC = 0.75f

val GUI_SCALE
    get() = if (IS_MOBILE)
        if (IS_PORTRAIT)
            GUI_SCALE_MOBILE_PORTRAIT
        else GUI_SCALE_MOBILE_LANDSCAPE
    else GUI_SCALE_PC

val GUI_VIEWPORT_WIDTH get() = round(Gdx.app.graphics.width / GUI_SCALE)
val GUI_VIEWPORT_HEIGHT get() = round(Gdx.app.graphics.height / GUI_SCALE)