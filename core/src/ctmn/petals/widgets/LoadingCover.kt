package ctmn.petals.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import ctmn.petals.assets
import ctmn.petals.utils.RegionAnimation
import ctmn.petals.utils.centerX
import ctmn.petals.utils.centerY
import ctmn.petals.utils.setPositionByCenter

class LoadingCover : Widget() {

    private val cover = Sprite(assets.textureAtlas.findRegion("gui/white"))
    //private val icon = Sprite(assets.textureAtlas.findRegion("gui/loading"))
    private val animation = RegionAnimation(0.25f, assets.textureAtlas.findRegions("gui/animated/loading"))
    private val loadingAni = Sprite(animation.currentFrame)

    private val coverAlpha = 0.7f
    private val fadeOutTime = 0.5f

    var fadeOutDelay = 0f

    init {
        cover.color = Color.BLACK
        cover.setAlpha(coverAlpha)

        setFillParent(true)
    }

    fun done() {
        addAction(Actions.sequence(Actions.delay(fadeOutDelay), Actions.fadeOut(fadeOutTime), Actions.removeActor()))
        animation.playMode = Animation.PlayMode.NORMAL
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        super.draw(batch, parentAlpha)

        if (color.a < coverAlpha)
            cover.setAlpha(color.a)

        //icon.setAlpha(color.a)
        loadingAni.setAlpha(color.a)
        
        if (stage != null)
            cover.setSize(width, height)

        cover.draw(batch)
        //icon.draw(batch)
        loadingAni.draw(batch)
    }

    override fun act(delta: Float) {
        var delta = delta
        if (delta > 0.03f) delta = 0.03f

        super.act(delta)



        //icon.setPositionByCenter(cover.centerX(), cover.centerY() + icon.height)
        //loadingAni.setPosition(icon.x, icon.y - loadingAni.height)

        loadingAni.setPositionByCenter(cover.centerX(), cover.centerY())

        animation.update(delta)
        loadingAni.setRegion(animation.currentFrame)
    }
}