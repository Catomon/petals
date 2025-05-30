package ctmn.petals

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import kotlin.math.round

object Const {

    // App:
    const val APP_NAME = "Fae's Feisty"
    const val APP_VER_NAME = "0.5.2-alpha"
    const val IS_RELEASE = false
    const val UNLOCK_LEVELS = true
    const val USER_FOLDER = "Documents/Fae's Feisty/"
    const val LEGACY_USER_FOLDER = "Documents/Fae's Feisty Fray/"

    const val CONSOLE_ENABLED = !IS_RELEASE
    const val DEBUG_MODE = !IS_RELEASE
    const val SHOW_FPS = !IS_RELEASE

    /** discord */
    const val DISABLE_RICH = !IS_RELEASE

    // Multiplayer:
    const val SERVER_PORT = 7396

    //JmDNS
    const val SERVICE_TYPE = "_faesfeisty._upd.local."
    const val SERVICE_NAME = "Faes Feisty JmDNS"
    const val SERVICE_DESCRIPTION = "Ip scan for quick join in multiplayer."


    // GUI:
    val IS_MOBILE get() = Gdx.app.type == Application.ApplicationType.Android
    val IS_DESKTOP get() = Gdx.app.type == Application.ApplicationType.Desktop
    val IS_PORTRAIT get() = Gdx.graphics.width < Gdx.graphics.height

    private const val GUI_SCALE_MOBILE_LANDSCAPE = 1.25f
    private const val GUI_SCALE_MOBILE_PORTRAIT = 1.5f
    private val GUI_SCALE_PC get() = if (Gdx.graphics.width > 1600) 1.5f else 1f
    val GUI_SCALE
        get() =
            if (IS_MOBILE)
                if (Gdx.graphics.width < Gdx.graphics.height)
                    GUI_SCALE_MOBILE_PORTRAIT
                else GUI_SCALE_MOBILE_LANDSCAPE
            else GUI_SCALE_PC

    val PLAY_GUI_VIEWPORT_WIDTH get() = round(Gdx.app.graphics.width / GUI_SCALE)
    val PLAY_GUI_VIEWPORT_HEIGHT get() = round(Gdx.app.graphics.height / GUI_SCALE)

    private const val PC_PLAY_CAMERA_ZOOM = 0.9f
    private val MOBILE_PLAY_CAMERA_ZOOM get() = if (IS_PORTRAIT) 0.3f else 0.6f

    val PLAY_CAMERA_ZOOM
        get() =
            if (Gdx.app.type == Application.ApplicationType.Android) {
                MOBILE_PLAY_CAMERA_ZOOM
            } else {
                PC_PLAY_CAMERA_ZOOM
            }

    val PLAY_CAMERA_ZOOM_STORY
        get() =
            if (Gdx.app.type == Application.ApplicationType.Android)
                if (IS_PORTRAIT)
                    0.3f
                else
                    0.6f
            else
                0.9f
    const val PLAY_CAMERA_ZOOM_OUT_MAX_STORY = 320f

    // Gameplay:
    const val EXPERIMENTAL: Boolean = true
    const val TILE_SIZE: Float = 16f
    const val TILE_SIZE_X2: Float = TILE_SIZE * 2f
    const val TILE_SIZE_HALF: Float = TILE_SIZE / 2

    // Alice
    const val ALICE_LEADER_ID = 13404
    const val FAERIE_CAMPAIGN_ID: Int = 537351234
    const val TREASURED_PETALS_STORY_ID: Int = 182730192
    const val FAE_STORY_ID: Int = 286903792

    // UnitActor
    const val MAX_LVL = 10
    const val EXP_GAIN = 25 //10
    const val EXP_GAIN_LEADER = 25 //100
    const val EXP_TO_LEVEL_UP = 250
    const val NEED_MORE_EXP_PER_LVL = false

    const val ACTION_POINTS = 2
    const val ACTION_POINTS_MOVE = 1
    const val ACTION_POINTS_ATTACK = 2
    const val ACTION_POINTS_ABILITY = 2
    const val ACTION_POINTS_MOVE_MIN = 2
    const val ACTION_POINTS_ATTACK_MIN = 1

    // Effects
    const val UNIT_ANIMATION_FRAME_DURATION = 0.5f
    const val TALKING_ANIMATION_DURATION = 3f

    const val UNIT_MOVE_SPEED: Float = 150f
    const val UNIT_SHAKE_POWER = 3f
    const val UNIT_SHAKE_DURATION = 0.5f

    const val IS_ROUND_HEALTH_CHANGE_LABEL = false

    // Other logic
    const val BASE_BUILD_COST = 200
    const val BASE_RANGE_OF_VIEW = 2
    const val GOLD_PER_BASE = 100
    const val HEALING_AMOUNT_NEAR_LEADER = 10
    const val KILL_CREDITS_SLIME_HUGE = 1000
    const val KILL_CREDITS_SLIME_LING = 200
    const val CRYSTALS_CLUSTER = 2000
    const val PLAYER_CREDITS_RESERVE = 2000
    const val REMOVE_UNIT_AFTER_CAPTURE = false
    const val CAPTURE_TIME = 2
    const val BASE_BUILD_TIME = 3

    //combat
    const val MISSES = true
    const val CRITS = true
    const val ATTACK_BACK = false
}