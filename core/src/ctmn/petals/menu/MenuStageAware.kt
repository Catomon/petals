package ctmn.petals.menu

/**
 * A menu stage that wants to know when the menu switches to it.
 *
 * `MenuScreen.stage` is a plain setter, and a scene2d `Stage` has no `show` hook of its own, so
 * this is how a stage gets told it became the visible one. Useful for anything that was borrowed
 * while another stage was open, such as a network listener.
 */
interface MenuStageAware {
    fun onMenuShown() {}
}
