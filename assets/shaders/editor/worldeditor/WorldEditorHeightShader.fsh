#version 330 core

in vec2 vQuadUV;

uniform sampler2D u_tileTexture;
uniform vec4      u_uvRect;
uniform float     u_overlayAlpha;

out vec4 FragColor;

// Lays the height layer over the world image, texel for texel, as the black
// and white heightmap it is: black the lowest ground, white the highest. It
// stays partly see-through so the biomes beneath line up with every level.
void main() {
    vec2  quad  = vec2(vQuadUV.x, 1.0 - vQuadUV.y);
    vec2  uv    = mix(u_uvRect.xy, u_uvRect.zw, quad);
    float level = texture(u_tileTexture, uv).r;

    FragColor = vec4(vec3(level), u_overlayAlpha);
}
