package ctmn.petals.widgets

import com.badlogic.gdx.utils.Array
import com.kotcrab.vis.ui.widget.VisScrollPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisTextButton
import ctmn.petals.utils.addClickListener

class ButtonsScrollPane(val buttonsGroup: VisTable = VisTable()) : VisScrollPane(buttonsGroup, ) {

    var onButtonClick: ((VisTextButton) -> Unit)? = null

    init {
        buttonsGroup.setSize(300f, 300f)
    }

    fun uncheckButtons() {
        for (button in buttonsGroup.children)
            if (button is VisTextButton) button.isChecked = false
    }

    fun addButton(name: String, userObject: Any? = null): VisTextButton {
        val button = newTextButton(name, "list_item")
        buttonsGroup.add(button).width(300f)
        buttonsGroup.row()

        if (onButtonClick != null)
            button.addClickListener {
                onButtonClick!!(button)

                // Only the buttons, since a list may hold a plain label between them.
                for (otherButton in buttonsGroup.children)
                    if (otherButton is VisTextButton) otherButton.isChecked = false

                button.isChecked = true
            }

        button.userObject = userObject
        return button
    }

    fun removeItems() = buttonsGroup.clear()

    fun setItems(items: Array<String>) {
        buttonsGroup.clear()

        for (item in items) {
            addButton(item)
        }
    }
}