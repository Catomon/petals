package ctmn.petals.playscreen.gui.widgets

import ctmn.petals.Const.TILE_SIZE
import ctmn.petals.playstage.getUnitOrTile
import ctmn.petals.playscreen.gui.PlayGUIStage
import ctmn.petals.playstage.isInRange
import ctmn.petals.tile.TileActor
import ctmn.petals.unit.*
import ctmn.petals.utils.*
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import ctmn.petals.newPlaySprite
import ctmn.petals.tile.Terrain
import ctmn.petals.unit.UnitActor

class TileSelectionDrawer(private val guiStage: PlayGUIStage) : Actor() {


    private val selectedSprite = Sprite(guiStage.assets.textureAtlas.findRegion("gui/selected_tile"))
    private val selectTexture = guiStage.assets.textureAtlas.findRegion("gui/hovering_select")
    private val attackUnitTexture = guiStage.assets.textureAtlas.findRegion("gui/hovering_attack")
    private val moveUnitTexture = guiStage.assets.textureAtlas.findRegion("gui/hovering_move")
    private val abilityTexture = guiStage.assets.textureAtlas.findRegion("gui/hovering_magic")
    private val summonDollTexture = guiStage.assets.textureAtlas.findRegion("gui/hovering_magic")
    private val cancelTexture = guiStage.assets.textureAtlas.findRegion("gui/hovering_cancel")

    private val moveUnitIconTexture = guiStage.assets.textureAtlas.findRegion("gui/hovering_move_icon")

    val hoveringSprite: Sprite = newPlaySprite(selectTexture)

    val hoveringIconSprite = newPlaySprite(moveUnitIconTexture)

    var hoveringActor: Actor? = null
        private set

    // locks hoveringSprite in place for one frame
    var confirmLock = false

    init {
        selectedSprite.setSize(TILE_SIZE * 2f, TILE_SIZE * 2f)
        hoveringSprite.setSize(TILE_SIZE * 2f, TILE_SIZE * 2f)
        hoveringIconSprite.setSize(TILE_SIZE * 2f, TILE_SIZE * 2f)

        hoveringIconSprite.setOriginCenter()

        guiStage.playStage.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                setPosition(x, y)

                return false
            }

            override fun mouseMoved(event: InputEvent?, x: Float, y: Float): Boolean {
                setPosition(x, y)

                if (confirmLock) {
                    if (hoveringSprite.centerX().tiled() != x.tiled() || hoveringSprite.centerY().tiled() != y.tiled()) {
                        hoveringSprite.setAlpha(0.5f)
                        hoveringIconSprite.setAlpha(0.5f)
                    } else {
                        hoveringSprite.setAlpha(1f)
                        hoveringIconSprite.setAlpha(1f)
                    }
                }

                return false
            }
        })
    }

    override fun act(delta: Float) {
        super.act(delta)
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        hoveringActor = guiStage.playStage.getUnitOrTile(hoveringSprite.centerX().tiled(), hoveringSprite.centerY().tiled())

        var drawHoveringSprite = false

        var confirmLockChanged = false
        if (confirmLock) {
            confirmLock = false
            confirmLockChanged = true
        }

        if (guiStage.isUnitSelected) {
            selectedSprite.setPosition(
                guiStage.selectedUnit!!.tiledX * TILE_SIZE - TILE_SIZE / 2f,
                guiStage.selectedUnit!!.tiledY * TILE_SIZE - TILE_SIZE / 2f)
            selectedSprite.draw(batch)
        }

        when (guiStage.clickStrategy) {
            guiStage.selectUnitCs -> {
                val hoveringUnit = guiStage.playStage.getUnit(hoveringSprite.centerX().tiled(), hoveringSprite.centerY().tiled())
                if (hoveringUnit != null && hoveringUnit.isVisible) {
                    hoveringSprite.setRegion(selectTexture)
                    drawHoveringSprite = true
                }
            }

            guiStage.unitSelectedCs -> {
                when (hoveringActor) {
                    is UnitActor -> {
                        if (hoveringActor != guiStage.selectedUnit
                            && !guiStage.selectedUnit!!.isAlly((hoveringActor as UnitActor))
                            && guiStage.selectedUnit!!.canAttack(hoveringActor as UnitActor)) {
                            hoveringSprite.setRegion(attackUnitTexture)
                        } else {
                            if (hoveringActor == guiStage.selectedUnit)
                                hoveringSprite.setRegion(cancelTexture)
                            else
                                hoveringSprite.setRegion(selectTexture)
                        }

                        if ((hoveringActor as UnitActor).isVisible)
                            drawHoveringSprite = true
                    }
                    is TileActor -> {
                        //todo update only after action or something
                        if (guiStage.selectedUnit?.canMove(hoveringSprite.centerX().tiled(), hoveringSprite.centerY().tiled()) == true) {
                            hoveringSprite.setRegion(moveUnitTexture)
                            var degrees = (degrees(
                                guiStage.selectedUnit!!.tiledX.unTiled() + TILE_SIZE / 2,
                                guiStage.selectedUnit!!.tiledY.unTiled() + TILE_SIZE / 2,
                                hoveringIconSprite.centerX(),
                                hoveringIconSprite.centerY()))

                            degrees = when {
                                degrees > 135f -> 180f
                                degrees > 45f -> 90f
                                degrees > -45f -> 0f
                                degrees > -135f -> -90f
                                else -> 180f
                            }
                            hoveringIconSprite.rotation = degrees
                            hoveringIconSprite.draw(batch)
                            drawHoveringSprite = true
                        } else {
                            hoveringSprite.setRegion(cancelTexture)
                        }
                    }
                    null -> {
                        hoveringSprite.setRegion(cancelTexture)
                    }
                }
            }

            guiStage.useAbilityCs -> {
                if (guiStage.selectedUnit!!.isInRange(hoveringSprite.centerX().tiled(), hoveringSprite.centerY().tiled(), guiStage.abilitiesPanel.selectedAbility!!.range)) {
                    hoveringSprite.setRegion(abilityTexture)
                } else {
                    hoveringSprite.setRegion(cancelTexture)
                }
                drawHoveringSprite = true
            }

            guiStage.confirmAbilityCs -> {
                confirmLock = true

                if (guiStage.abilitiesPanel.selectedAbility?.range == 0) {
                    hoveringActor = guiStage.selectedUnit!!
                    hoveringSprite.setPositionByCenter(hoveringActor!!.centerX, hoveringActor!!.centerY)
                }

                if (isInRange(
                        guiStage.abilityActivationRangeBorder.x.tiled(), guiStage.abilityActivationRangeBorder.y.tiled(),
                        guiStage.abilitiesPanel.selectedAbility!!.range,
                        hoveringSprite.centerX().tiled(), hoveringSprite.centerY().tiled())
                    && hoveringSprite.centerX().tiled() == guiStage.abilityActivationRangeBorder.x.tiled()
                    && hoveringSprite.centerY().tiled() == guiStage.abilityActivationRangeBorder.y.tiled()) {
                    hoveringSprite.setRegion(abilityTexture)
                } else {
                    hoveringSprite.setRegion(cancelTexture)

                }
                drawHoveringSprite = true
            }

//            guiStage.summonDollCs -> {
//                if (guiStage.selectedUnit?.isInRange(
//                        hoveringSprite.tiledX, hoveringSprite.tiledY, guiStage.selectedUnit!!.cAlice!!.summonRange) == true
//                    && (hoveringSprite.tiledX != guiStage.selectedUnit!!.tiledX
//                    || hoveringSprite.tiledY != guiStage.selectedUnit!!.tiledY)) {
//                    hoveringSprite.setRegion(summonDollTexture)
//                } else {
//                    hoveringSprite.setRegion(cancelTexture)
//                }
//
//                hoveringSprite.draw(batch)
//            }
        }

        val tile = guiStage.playStage.getTile(hoveringSprite.tiledX, hoveringSprite.tiledY)
        if (drawHoveringSprite || tile?.terrain == Terrain.base) {
            if (tile?.terrain == Terrain.base)
                hoveringSprite.setRegion(selectTexture)
            hoveringSprite.draw(batch)
        }

        if (confirmLockChanged) {
            if (!confirmLock) {
                hoveringSprite.setPosition(x, y)
                hoveringIconSprite.setPosition(x, y)
                hoveringSprite.setAlpha(1f)
                hoveringIconSprite.setAlpha(1f)
            }
        }
    }

    val Sprite.tiledX get() = centerX().tiled()

    val Sprite.tiledY get() = centerY().tiled()

    override fun setPosition(x: Float, y: Float) {
        var tileFrameX = (x / TILE_SIZE).toInt() * TILE_SIZE.toFloat() - TILE_SIZE / 2
        if (x < 0)
            tileFrameX -= TILE_SIZE
        var tileFrameY = (y / TILE_SIZE).toInt() * TILE_SIZE.toFloat() - TILE_SIZE / 2
        if (y < 0)
            tileFrameY -= TILE_SIZE

        super.setPosition(tileFrameX, tileFrameY)
    }

    override fun positionChanged() {
        super.positionChanged()

        selectedSprite.setPosition(x, y)

        if (!confirmLock) {
            hoveringSprite.setPosition(x, y)
            hoveringIconSprite.setPosition(x , y)
        }
    }
}
