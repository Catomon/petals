package ctmn.petals.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector3

class TintShader(
    var tint: Vector3 = Vector3(1.0f, 0.5f, 0.0f),
    var intensity: Float = .5f
) {

    val shader = ShaderProgram(Gdx.files.internal("shaders/shader.vert"), Gdx.files.internal("shaders/tint.frag"))

    init {
        if (!shader.isCompiled) Gdx.app.log("Shader", shader.log);
    }

    fun setUniformfs() {
        shader.setUniformf("u_intensity", intensity)
        shader.setUniformf("u_tint", tint)
    }
}