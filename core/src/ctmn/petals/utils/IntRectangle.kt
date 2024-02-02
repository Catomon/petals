package ctmn.petals.utils

class IntRectangle(var x: Int, var y: Int, var width: Int, var height: Int) {
    fun contains(x: Int, y: Int): Boolean {
        return this.x <= x && x < this.x + width && this.y <= y && y < this.y + height
    }
}