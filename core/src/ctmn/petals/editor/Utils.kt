package ctmn.petals.editor

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.kotcrab.vis.ui.widget.VisTextButton
import ctmn.petals.widgets.addClickSound
import ctmn.petals.widgets.addFocusBorder

fun makeValidFileName(name: String): String {
    return name.replace("\\s+".toRegex(), "_").replace("[^A-Za-z0-9_]".toRegex(), "").take(60)
}

fun newTextButton(text: String, styleName: String = "default"): VisTextButton {
    return VisTextButton(text, styleName).addClickSound().addFocusBorder()
}

fun ShapeRenderer.rect(rectangle: Rectangle) {
    rect(rectangle.x, rectangle.y, rectangle.width, rectangle.height)
}