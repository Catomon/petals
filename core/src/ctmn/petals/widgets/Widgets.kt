package ctmn.petals.widgets

import com.badlogic.gdx.Input
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.Focusable
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.util.BorderOwner
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisWindow
import ctmn.petals.Const
import ctmn.petals.assets
import ctmn.petals.utils.addClickListener

// widgets creation functions

fun newLabel(text: String = "text", style: String? = null): VisLabel {
    return if (style == null) VisLabel(text) else VisLabel(text, style)
}

fun newButton(styleName: String): Button {
    return Button(VisUI.getSkin().get(styleName, ButtonStyle::class.java)).addClickSound()
}

// todo i used this button wrong. could use just Button instead
fun newImageButton(styleName: String): VisImageButton {
    return VisImageButton(styleName).addClickSound().addFocusBorder()
}

fun newTextButton(text: String = "text", styleName: String? = null): VisTextButton {
    val button = if (styleName == null) VisTextButton(text) else VisTextButton(text, styleName)

    button.addClickSound()
    //button.label.setFontScale(0.3f)

    button.addFocusBorder()

    return button
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

    if (!Const.IS_MOBILE) {
        addListener(object : ClickListener() {
            override fun enter(event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?) {
                focusGained()
            }

            override fun exit(event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?) {
                focusLost()
            }
        })
    } else {

        isFocusBorderEnabled = false
    }

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
        add(newImageButton("confirm").addChangeListener {
            win.fadeOut()
            action?.invoke()
        }).padLeft(8f).align(Align.left)
        if (cancelButton) {
            add().expandX()
            add(newImageButton("cancel").addChangeListener {
                win.fadeOut()
            }).padRight(8f).align(Align.right)
        }
    }).expandX().fillX()
    win.pack()

    return win
}

fun <T : Actor> T.addChangeListener(listener: () -> Unit): T {
    addListener(object : ChangeListener() {
        override fun changed(event: ChangeEvent?, actor: Actor?) {
            listener()
        }
    })

    return this as T
}

fun <T : Actor> T.addClickSound(sound: Sound = assets.getSound("click.ogg")): T {
    return addClickListener { sound.play() } as T
}