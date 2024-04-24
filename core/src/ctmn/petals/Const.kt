package ctmn.petals

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import kotlin.math.round

object Const {

    // App:
    const val APP_NAME = "Petals: F&GCC"
    const val APP_VER_NAME = "\nDemo"
    const val IS_RELEASE = false

    const val CONSOLE_ENABLED = !IS_RELEASE
    const val DEBUG_MODE = !IS_RELEASE

    /** discord */
    const val DISABLE_RICH = !IS_RELEASE

    // Multiplayer:
    const val SERVICE_TYPE = "_alissagame._upd.local."
    const val SERVICE_NAME = "AlissaGameService"
    const val SERVICE_DESCRIPTION = "Alissa Game Service"
    const val SERVER_PORT = 7396

    // GUI:
    val IS_MOBILE get() = Gdx.app.type == Application.ApplicationType.Android
    val IS_DESKTOP get() = Gdx.app.type == Application.ApplicationType.Desktop
    val IS_PORTRAIT get() = Gdx.graphics.width < Gdx.graphics.height

    private const val GUI_SCALE_MOBILE_LANDSCAPE = 1.25f
    private const val GUI_SCALE_MOBILE_PORTRAIT = 1.5f
    private const val GUI_SCALE_PC = 1f
    val GUI_SCALE get() =
        if (IS_MOBILE)
            if (Gdx.graphics.width < Gdx.graphics.height)
                GUI_SCALE_MOBILE_PORTRAIT
            else GUI_SCALE_MOBILE_LANDSCAPE
        else GUI_SCALE_PC

    val PLAY_GUI_VIEWPORT_WIDTH get() = round(Gdx.app.graphics.width / GUI_SCALE)
    val PLAY_GUI_VIEWPORT_HEIGHT get() = round(Gdx.app.graphics.height / GUI_SCALE)

    private const val PC_PLAY_CAMERA_ZOOM = 0.9f
    private const val MOBILE_PLAY_CAMERA_ZOOM = 0.6f

    val PLAY_CAMERA_ZOOM get() =
        if (Gdx.app.type == Application.ApplicationType.Android) {
            MOBILE_PLAY_CAMERA_ZOOM
        } else {
            PC_PLAY_CAMERA_ZOOM
        }

    val PLAY_CAMERA_ZOOM_STORY get() = if (Gdx.app.type == Application.ApplicationType.Android) 0.6f else 0.9f
    const val PLAY_CAMERA_ZOOM_OUT_MAX_STORY = 320f

    // Gameplay:
    const val TILE_SIZE: Int = 16

    // Alice
    const val ALICE_LEADER_ID = 13404
    const val TREASURED_PETALS_STORY_ID: Int = 182730192
    const val FAE_STORY_ID: Int = 286903792

    // UnitActor
    const val MAX_LVL = 10
    const val EXP_GAIN = 10
    const val EXP_GAIN_LEADER = 25
    const val EXP_MOD_LEVEL_UP = 250

    const val ACTION_POINTS = 2
    const val ACTION_POINTS_MOVE = 1
    const val ACTION_POINTS_ATTACK = 2
    const val ACTION_POINTS_ABILITY = 2
    const val ACTION_POINTS_MOVE_MIN = 2
    const val ACTION_POINTS_ATTACK_MIN = 1

    // Effects
    const val UNIT_ANIMATION_FRAME_DURATION = 0.5f
    const val TALKING_ANIMATION_DURATION = 1f

    const val UNIT_MOVE_SPEED: Float = 200f
    const val UNIT_SHAKE_POWER = 3f
    const val UNIT_SHAKE_DURATION = 0.5f

    const val IS_ROUND_HEALTH_CHANGE_LABEL = false

    // Other logic
    const val BASE_RANGE_OF_VIEW = 2
    const val GOLD_PER_BASE = 100
    const val HEALING_AMOUNT_NEAR_LEADER = 10
    const val KILL_CREDITS_SLIME_HUGE = 1000
    const val KILL_CREDITS_SLIME_LING = 200
}