#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_intensity;
uniform vec3 u_tint;

void main() {
  vec4 color = texture2D(u_texture, v_texCoords);

  vec3 finalColor = mix(color.rgb, color.rgb * u_tint, u_intensity);

  gl_FragColor = vec4(finalColor, color.a);
}