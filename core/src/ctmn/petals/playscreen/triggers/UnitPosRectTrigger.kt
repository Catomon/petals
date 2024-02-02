package ctmn.petals.playscreen.triggers

import ctmn.petals.playstage.tiledHeight
import ctmn.petals.playstage.tiledWidth
import ctmn.petals.unit.UnitActor
import ctmn.petals.unit.tiledX
import ctmn.petals.utils.IntRectangle
import ctmn.petals.utils.RectRenderer
import ctmn.petals.utils.tiledY
import com.badlogic.gdx.utils.Array

class UnitPosRectTrigger(val unit: UnitActor? = null, x: Int = -1, y: Int = -1) : Trigger() {

    private val rectangle = IntRectangle(x, y, 1, 1)

    private var expandX = false
    private var expandY = false
    private var expandTop = false
    private var expandRight = false

    private var units: Array<UnitActor>? = null

    constructor(units: Array<UnitActor>, x: Int = -1, y: Int = -1) : this(null, x, y) {
        this.units = Array<UnitActor>().apply { addAll(units) }
    }

    override fun check(delta: Float): Boolean {
        check(rectangle.x > -1 && rectangle.y > -1)

        RectRenderer.add(rectangle.x * 16f, rectangle.y * 16f,
            rectangle.width * 16f, rectangle.height * 16f)

        units?.forEach { if (rectangle.contains(it.tiledX, it.tiledY)) return true }

        unit?.let { return rectangle.contains(it.tiledX, it.tiledY) }

        return false
    }

    fun pos(x: Int, y: Int) : UnitPosRectTrigger {
        rectangle.x = x
        rectangle.y = y

        return this
    }

    fun expand(radius: Int) : UnitPosRectTrigger {
        rectangle.width += radius * 2
        rectangle.x -= radius

        rectangle.height += radius * 2
        rectangle.y -= radius

        return this
    }

    fun expandTop() : UnitPosRectTrigger {
        expandTop = true

        return this
    }

    fun expandBottom() : UnitPosRectTrigger {
        rectangle.height += rectangle.y
        rectangle.y = 0

        return this
    }

    fun expandRight() : UnitPosRectTrigger {
        expandRight = true

        return this
    }

    fun expandLeft() : UnitPosRectTrigger {
        rectangle.width += rectangle.x
        rectangle.x = 0

        return this
    }

    fun expandX() : UnitPosRectTrigger {
        expandX = true

        return this
    }

    fun expandY() : UnitPosRectTrigger {
        expandY = true

        return this
    }

    override fun onAdded() {
        super.onAdded()

        if (expandX) {
            rectangle.x = 0
            rectangle.width = playScreen.playStage.tiledWidth()
        }

        if (expandY) {
            rectangle.y = 0
            rectangle.height = playScreen.playStage.tiledHeight()
        }

        if (expandTop) {
            rectangle.height = playScreen.playStage.tiledHeight() - rectangle.y
        }

        if (expandRight) {
            rectangle.width = playScreen.playStage.tiledWidth() - rectangle.x
        }
    }
}