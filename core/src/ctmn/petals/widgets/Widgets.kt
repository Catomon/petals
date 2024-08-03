package ctmn.petals.widgets

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.Focusable
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.util.BorderOwner
import com.kotcrab.vis.ui.widget.*
import ctmn.petals.AudioManager
import ctmn.petals.utils.addClickListener

// widgets creation functions

fun newLabel(text: String = "text", style: String? = null): VisLabel {
    return if (style == null) VisLabel(text) else VisLabel(text, style)
}

/** A button that has drawable and styled button frame */
fun newImageButton(drawableName: String): Button {
    return VisImageButton(VisUI.getSkin().newDrawable(drawableName)).addClickSound().addFocusBorder()
}

/** A button that has drawable and styled button frame */
fun newImageTextButton(text: String, drawableName: String): Button {
    return VisImageTextButton(text, VisUI.getSkin().newDrawable(drawableName)).addClickSound().addFocusBorder()
}

fun newIconButton(styleName: String): VisImageButton {
    return VisImageButton(styleName).addClickSound().addFocusBorder()
}

fun newTextButton(text: String = "text", styleName: String? = null): VisTextButton {
    val button = if (styleName == null) VisTextButton(text) else VisTextButton(text, styleName)

    button.addClickSound()
    //button.label.setFontScale(0.3f)

    button.addFocusBorder()

    return button
}

fun VisImageTextButton.addFocusBorder(): VisImageTextButton {
    return addFocusBorderP()
}

fun VisImageButton.addFocusBorder(): VisImageButton {
    return addFocusBorderP()
}

fun VisTextButton.addFocusBorder(): VisTextButton {
    return addFocusBorderP()
}

private fun <T : BorderOwner> T.addFocusBorderP(): T {
    if (this !is Actor) throw IllegalArgumentException("This is not an Actor class")
    if (this !is Focusable) throw IllegalArgumentException("The actor class is not Focusable")

//    if (!Const.IS_MOBILE) {
    addListener(object : ClickListener() {
        override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
            focusGained()
        }

        override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
            focusLost()
        }
    })
//    } else {
//
//        isFocusBorderEnabled = false
//    }

    return this
}

fun Stage.addNotifyWindow(
    message: String,
    title: String = "Message",
    action: (() -> Unit)? = null,
    cancelButton: Boolean = false,
) {
    val window = newNotifyWindow(message, title, action, cancelButton)
    addActor(window)
    window.centerWindow()
    window.addListener(object : InputListener() {
        override fun keyDown(event: InputEvent, keycode: Int): Boolean {
            if (keycode == Input.Keys.ENTER
                || keycode == Input.Keys.ESCAPE
            ) {
                window.fadeOut()
                return true
            }
            return false
        }
    })
}

fun newNotifyWindow(
    message: String,
    title: String = "Message",
    action: (() -> Unit)? = null,
    cancelButton: Boolean = false,
): VisWindow {
    val label = newLabel(message, "font_5")
    label.wrap = true
    label.pack()
    label.setAlignment(Align.center)

    val win = VisWindow(title)
    win.closeOnEscape()
    win.add(label).expandX().prefWidth(400f).pad(10f)
    win.row().padTop(8f)
    win.add(VisTable().apply {
        add(newIconButton("confirm").addChangeListener {
            win.fadeOut()
            action?.invoke()
        }).padLeft(8f).align(Align.left)
        if (cancelButton) {
            add().expandX()
            add(newIconButton("cancel").addChangeListener {
                win.fadeOut()
            }).padRight(8f).align(Align.right)
        }
    }).expandX().fillX()
    win.pack()

    return win
}

fun <T : Actor> T.addChangeListener(listener: (actor: T) -> Unit): T {
    addListener(object : ChangeListener() {
        override fun changed(event: ChangeEvent?, actor: Actor?) {
            listener(actor as T)
        }
    })

    return this as T
}

fun <T : Actor> T.addClickSound(soundName: String = "click"): T {
    return addClickListener { AudioManager.sound(soundName) } as T
}