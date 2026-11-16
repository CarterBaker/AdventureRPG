#version 330 core

in vec2 vUV;

uniform sampler2D u_imageTexture;
uniform int       u_hasImage;
uniform vec2      u_imageExtent;
uniform vec4      u_backgroundColor;
uniform vec4      u_checkerColor;
uniform vec4      u_borderColor;
uniform float     u_checkerCells;
uniform float     u_borderWidth;

out vec4 FragColor;

// The image shown, its layers laid flat or one of its maps, over the rectangle of view space its width and height
// span (u_imageExtent, the longer side one unit), a checker behind it where it is clear as a painting tool shows
// transparency, its rows top first, with the image's border measured in screen pixels and the backdrop beyond it.
void main() {
    vec2  pixel = fwidth(vUV);
    vec2  local = vUV / u_imageExtent;
    vec4  color = u_backgroundColor;

    if (all(greaterThanEqual(local, vec2(0.0))) && all(lessThanEqual(local, vec2(1.0)))) {
        vec2  cell    = floor(vUV * u_checkerCells);
        float checker = mod(cell.x + cell.y, 2.0);

        color = mix(u_backgroundColor, u_checkerColor, checker);

        if (u_hasImage != 0) {
            vec4 texel = texture(u_imageTexture, vec2(local.x, 1.0 - local.y));
            color.rgb  = mix(color.rgb, texel.rgb, texel.a);
        }
    }

    vec2  reach  = pixel * u_borderWidth / u_imageExtent;
    vec2  within = step(-reach, local) * step(local, vec2(1.0) + reach);
    vec2  line   = step(min(abs(local), abs(local - vec2(1.0))), reach);
    float onEdge = max(line.x * within.y, line.y * within.x);

    color.rgb = mix(color.rgb, u_borderColor.rgb, onEdge * u_borderColor.a);
    FragColor = vec4(color.rgb, 1.0);
}
