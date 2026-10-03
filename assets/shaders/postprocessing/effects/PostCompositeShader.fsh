#version 330 core

in  vec2 v_texCoord;
out vec4 fragColor;

#include "includes/PostProcessData.glsl"
#include "postprocessing/effects/includes/PostFocus.glsl"
#include "postprocessing/effects/includes/PostBlurRadius.glsl"
#include "postprocessing/effects/includes/PostAntiAliasing.glsl"

/*
 * Finishes the composited scene at full resolution. The sharp scene is
 * anti-aliased, its red and blue split apart toward the screen's edges like
 * a lens's fringe, and blended into the gathered depth of field by this
 * pixel's own circle of confusion at full resolution, or by the blurred
 * foreground covering it, whichever is greater, so the blur always meets
 * sharp edges cleanly. Bloom is added over it, then the brightness, contrast
 * and saturation are graded, the corners darkened and film grain laid over
 * the whole, changing like film frames rather than every frame.
 */

// The composited scene and the blur pass's depth of field and bloom.
uniform sampler2D u_sceneColor;
uniform sampler2D u_blurDepthOfField;
uniform sampler2D u_blurBloom;

// Seconds of grain, wrapped by the post effects system.
uniform float u_grainTime;

const vec3  LUMA_WEIGHTS           = vec3(0.2126, 0.7152, 0.0722);
const float DOF_BLEND_START_PIXELS = 0.5;
const float DOF_BLEND_END_PIXELS   = 2.0;
const float BLOOM_STRENGTH         = 1.5;
const float GRADE_PIVOT            = 0.5;
const float FRINGE_MAX_OFFSET      = 0.012;
const float FRINGE_FADE_PIXELS     = 1.0;
const float VIGNETTE_START         = 0.35;
const float VIGNETTE_END           = 1.0;
const float GRAIN_STRENGTH         = 0.12;
const float GRAIN_FRAME_RATE       = 24.0;
const vec2  GRAIN_FRAME_SHIFT      = vec2(37.0, 17.0);

// ── Lens Fringe ────────────────────────────────────────────────────────────

// How far red and blue split at this pixel, growing from none at the centre of the screen.
vec2 resolveFringeOffset(vec2 uv) {
    return (uv - 0.5) * u_chromaticAberration * FRINGE_MAX_OFFSET;
}

// Splits red and blue out along the fringe, eased in over its first pixel so the anti-aliased
// centre of the screen is never replaced by unfiltered taps.
vec3 applyFringe(sampler2D source, vec3 color, vec2 uv, vec2 fringe, vec2 size) {
    float split = clamp(length(fringe * size) / FRINGE_FADE_PIXELS, 0.0, 1.0);

    if (split <= 0.0)
    return color;

    color.r = mix(color.r, texture(source, uv + fringe).r, split);
    color.b = mix(color.b, texture(source, uv - fringe).b, split);

    return color;
}

// ── Depth of Field ─────────────────────────────────────────────────────────

// The gathered depth of field, softened by four half-texel taps into a tent over the half-resolution target.
vec4 sampleDepthOfField(vec2 uv) {
    vec2 halfTexel = 0.5 / vec2(textureSize(u_blurDepthOfField, 0));

    return 0.25 * (
        texture(u_blurDepthOfField, uv + vec2(-halfTexel.x, -halfTexel.y)) +
        texture(u_blurDepthOfField, uv + vec2( halfTexel.x, -halfTexel.y)) +
        texture(u_blurDepthOfField, uv + vec2(-halfTexel.x,  halfTexel.y)) +
        texture(u_blurDepthOfField, uv + vec2( halfTexel.x,  halfTexel.y)));
}

vec3 applyDepthOfField(vec3 color, vec2 uv, vec2 fringe, vec2 size) {
    vec4  blurred   = sampleDepthOfField(uv);
    float cocPixels = abs(resolveCocAt(uv)) * resolveDofRadius(size.y);
    float blend     = max(smoothstep(DOF_BLEND_START_PIXELS, DOF_BLEND_END_PIXELS, cocPixels), blurred.a);

    blurred.rgb = applyFringe(u_blurDepthOfField, blurred.rgb, uv, fringe, size);

    return mix(color, blurred.rgb, blend);
}

// ── Grading ────────────────────────────────────────────────────────────────

vec3 applyColorGrading(vec3 color) {
    color *= u_brightness;
    color  = (color - GRADE_PIVOT) * u_contrast + GRADE_PIVOT;
    color  = mix(vec3(dot(color, LUMA_WEIGHTS)), color, u_saturation);

    return max(color, vec3(0.0));
}

// Darkens toward the corners; reach runs from 0 at the centre to 1 at a corner on any aspect.
vec3 applyVignette(vec3 color, vec2 uv, vec2 size) {
    vec2  aspect = vec2(size.x / size.y, 1.0);
    float reach  = length((uv - 0.5) * aspect) / length(aspect * 0.5);

    return color * (1.0 - u_vignette * smoothstep(VIGNETTE_START, VIGNETTE_END, reach));
}

// ── Grain ──────────────────────────────────────────────────────────────────

float hashGrain(vec2 position) {
    vec3 p = fract(vec3(position.xyx) * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

vec3 applyFilmGrain(vec3 color, vec2 fragCoord) {
    if (u_filmGrain <= 0.0)
    return color;

    vec2  frame = floor(u_grainTime * GRAIN_FRAME_RATE) * GRAIN_FRAME_SHIFT;
    float grain = hashGrain(fragCoord + frame) - 0.5;

    return color + vec3(grain * u_filmGrain * GRAIN_STRENGTH);
}

void main() {
    vec2 size   = vec2(textureSize(u_sceneColor, 0));
    vec2 texel  = 1.0 / size;
    vec2 fringe = resolveFringeOffset(v_texCoord);

    vec3 color = u_antiAliasing
    ? applyAntiAliasing(u_sceneColor, v_texCoord, texel)
    : texture(u_sceneColor, v_texCoord).rgb;

    color = applyFringe(u_sceneColor, color, v_texCoord, fringe, size);

    if (u_dofEnabled)
    color = applyDepthOfField(color, v_texCoord, fringe, size);

    if (u_bloomEnabled)
    color += texture(u_blurBloom, v_texCoord).rgb * u_bloomIntensity * BLOOM_STRENGTH;

    color = applyColorGrading(color);
    color = applyVignette(color, v_texCoord, size);
    color = applyFilmGrain(color, gl_FragCoord.xy);

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
