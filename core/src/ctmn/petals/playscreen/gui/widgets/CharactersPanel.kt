package ctmn.petals.playscreen.gui.widgets

import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTable
import ctmn.petals.assets
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.utils.AnimatedSprite
import ctmn.petals.utils.RegionAnimation

class CharactersPanel(val guiStage: PlayGUIStage) : VisTable() {

    companion object {
        const val CHARACTER_HELPER_FAIRY = "fairy_helper"
    }

    init {
        setFillParent(true)
        left()

        add(VerticalGroup().apply {
            addActor(CharacterImage(CHARACTER_HELPER_FAIRY))
        })

        guiStage.addListener {
            if (it is StoryDialog.QuoteStartedEvent) {
                if (it.quote.source is CharacterImage) {
                    val src = it.quote.source as CharacterImage
                    src.setTalking()
                }

            } else {
                if (it is StoryDialog.QuoteEndedEvent) {
                    if (it.quote.source is CharacterImage) {
                        val src = it.quote.source as CharacterImage
                        src.setIdle()
                    }
                }
            }

            false
        }
    }

    class CharacterImage(val charName: String = "character_unknown") : VisImage() {
        val sprite = AnimatedSprite(RegionAnimation(0.75f, assets.guiAtlas.findRegions("portraits/$charName")))

        init {
//            sprite.animation.playMode = Animation.PlayMode.NORMAL
            drawable = SpriteDrawable(sprite)
            setSize(sprite.width, sprite.height)

            name = charName

            isVisible = false
        }

        override fun act(delta: Float) {
            super.act(delta)

            sprite.update(delta)
        }

        fun setTalking() {
            val regs = assets.guiAtlas.findRegions("portraits/" + charName + "_talking")
            if (regs.isEmpty) return
            sprite.animation.setFrames(regs)
            drawable = SpriteDrawable(sprite)

            isVisible = true
        }

        fun setIdle() {
            var regs = assets.guiAtlas.findRegions("portraits/$charName")
            if (regs.isEmpty) regs = assets.guiAtlas.findRegions("portraits/" + "character_unknown")
            sprite.animation.setFrames(regs)
            drawable = SpriteDrawable(sprite)

            isVisible = false
        }
    }
}