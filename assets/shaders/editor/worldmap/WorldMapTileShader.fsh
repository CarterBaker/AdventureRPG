#version 330 core

in vec2 vQuadUV;

uniform sampler2D u_tileTexture;
uniform vec4      u_uvRect;

out vec4 FragColor;

// Shows one part of a world map tile across its quad. The tile's rows run
// north to south while the quad runs bottom to top, so v is flipped; u_uvRect
// is (u0, v0, u1, v1) in the tile's own space, the whole tile or the quarter of
// a quarter a coarser tile stands in with.
void main() {
    vec2 uv = mix(u_uvRect.xy, u_uvRect.zw, vec2(vQuadUV.x, 1.0 - vQuadUV.y));

    FragColor = vec4(texture(u_tileTexture, uv).rgb, 1.0);
}
