#version 330 core

in vec2 vQuadUV;

uniform sampler2D u_tileTexture;
uniform sampler2D u_daylightTexture;
uniform sampler2D u_weatherTexture;
uniform vec4      u_uvRect;
uniform vec4      u_worldRect;
uniform vec2      u_weatherOffset;
uniform float     u_showDayNight;
uniform float     u_showWeather;
uniform vec4      u_nightColor;
uniform float     u_nightStrength;
uniform vec4      u_cloudColor;
uniform float     u_cloudStrength;
uniform vec4      u_rainColor;
uniform float     u_rainStrength;

out vec4 FragColor;

// Shows one part of a world map tile across its quad. The tile's rows run
// north to south while the quad runs bottom to top, so v is flipped; u_uvRect
// is (u0, v0, u1, v1) in the tile's own space, the whole tile or the quarter of
// a quarter a coarser tile stands in with. u_worldRect is the same quad as a
// share of the whole world, which the shared overlays are read across: the
// weather, held in noise space and slid by the weather flow, tints rain and
// lays cloud over the land, and day and night, one darkness per spot of the
// world, shades the night side.
void main() {
    vec2 quad  = vec2(vQuadUV.x, 1.0 - vQuadUV.y);
    vec2 uv    = mix(u_uvRect.xy, u_uvRect.zw, quad);
    vec2 world = mix(u_worldRect.xy, u_worldRect.zw, quad);
    vec3 color = texture(u_tileTexture, uv).rgb;

    if (u_showWeather > 0.5) {
        vec2 weather = texture(u_weatherTexture, world - u_weatherOffset).rg;
        color = mix(color, u_rainColor.rgb, weather.g * u_rainStrength);
        color = mix(color, u_cloudColor.rgb, weather.r * u_cloudStrength);
    }

    if (u_showDayNight > 0.5) {
        float night = texture(u_daylightTexture, world).r;
        color = mix(color, color * u_nightColor.rgb, night * u_nightStrength);
    }

    FragColor = vec4(color, 1.0);
}
