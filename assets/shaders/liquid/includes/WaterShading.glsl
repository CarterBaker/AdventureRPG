#ifndef WATER_SHADING_GLSL
#define WATER_SHADING_GLSL

#include "includes/CameraData.glsl"
#include "liquid/includes/OceanSurface.glsl"
#include "liquid/includes/WaterLight.glsl"
#include "liquid/includes/WaterSceneData.glsl"
#include "liquid/includes/WaterReflection.glsl"
#include "postprocessing/lighting/includes/AtmosphericFog.glsl"
#include "weather/includes/CloudComposite.glsl"

/*
 * Forward water shading, shared by the voxel sea and the distant sea plane, drawn after deferred lighting over
 * the lit scene. Fragments behind opaque geometry are hidden by hand against the scene depth, and fragments
 * inside the dry hull of a vehicle are never drawn, so the sea stays out of its hold. From above, the scene
 * below is refracted through the wave normal and absorbed with the thickness of water it is seen through, so
 * shallows stay clear over sand and deepen through banded turquoise, teal and blue; the sky, clouds and shore
 * reflect by Fresnel; the sun leaves a hard cartoon glint; whitecaps break on storm crests and foam lines ring
 * every shore; and clouds standing between the camera and the surface, or the fog of a cloud the camera is in,
 * lie over it. From below, the sky shows through Snell's window and the rest of the surface mirrors the depths,
 * under the same fog the underwater pass lays over the scene. Detail falls away with distance: ripples fade
 * out, waves settle to a calmer slope, and the caustics and the reflection march stop, so far water neither
 * sparkles nor costs what near water does.
 *
 * The engine prepends a program's includes to every one of its stages, so nothing here may touch what only a
 * fragment stage has. A fragment shader resolves its WaterSurface from its own gl_FragCoord, measures the foam
 * edges with fwidth itself, then shades:
 *
 *     WaterSurface surface;
 *     if (!resolveWaterSurface(fragment, gl_FragCoord.xyz, surface)) discard;
 *     vec2 foamEdges = max(fwidth(surface.foamValues), vec2(WATER_FOAM_EDGE_MIN));
 *     FragColor = vec4(shadeWaterSurface(surface, foamEdges), 1.0);
 */

const float WATER_REFRACTION_STRENGTH  = 0.05;
const float WATER_REFRACTION_REACH     = 3.0;
const float WATER_UNSEEN_DEPTH_BLOCKS  = 64.0;
const float WATER_FRESNEL_BASE         = 0.02;
const float WATER_FRESNEL_POWER        = 5.0;
const float WATER_REFLECTION_STRENGTH  = 0.85;
const float WATER_GLINT_POWER          = 420.0;
const float WATER_GLINT_THRESHOLD      = 0.5;
const float WATER_GLINT_SOFTNESS       = 0.06;
const float WATER_GLINT_STRENGTH       = 1.4;
const float WATER_MOON_GLINT_STRENGTH  = 0.6;
const float WATER_FOAM_LATTICE_SCALE   = 48.0;
const float WATER_WHITECAP_GAIN        = 1.6;
const float WATER_SHORE_FOAM_DEPTH     = 0.8;
const float WATER_SHORE_STATE_GAIN     = 0.35;
const float WATER_SHORE_RING_FREQUENCY = 7.0;
const float WATER_SHORE_RING_SPEED     = 1.6;
const float WATER_SHORE_RING_SHARE     = 0.35;
const float WATER_FOAM_NOISE_SHARE     = 0.45;
const float WATER_FOAM_EDGE_MIN        = 0.02;
const float WATER_SNELL_ETA            = 1.333;
const float WATER_SNELL_EDGE           = 0.25;
const uint  WATER_FOAM_SEED            = 2731u;
const float WATER_DISTANCE_EPSILON     = 0.0001;
const float WATER_DETAIL_FADE_START    = 160.0;
const float WATER_DETAIL_FADE_END      = 640.0;
const float WATER_FAR_WAVE_SLOPE_SHARE = 0.35;

struct WaterFragment {
    vec3  worldPos;
    vec3  faceNormal;
    float tidal;
    vec4  oceanState;
};

// Everything shading needs about one fragment, resolved before its foam edges, which only the fragment stage
// can measure. foamValues holds the whitecap and shore foam fields the cel masks cut at one half.
struct WaterSurface {
    WaterFragment fragment;
    OceanState    state;
    vec2          screenUV;
    float         sceneDepth;
    float         fragmentDepth;
    vec3          normal;
    vec3          viewDir;
    float         surfaceDistance;
    float         detail;
    bool          fromBelow;
    float         shoreDepth;
    vec2          foamValues;
};

// ── Detail ────────────────────────────────────────────────────────────────

// 1 near the camera, easing to 0 far out, where ripples, caustics and the reflection march would only alias
// into sparkle and cost time no pixel shows.
float resolveWaterDetail(float surfaceDistance) {
    return 1.0 - smoothstep(WATER_DETAIL_FADE_START, WATER_DETAIL_FADE_END, surfaceDistance);
}

// ── Foam ──────────────────────────────────────────────────────────────────

float sampleFoamNoise(vec2 pos) {
    return periodicGradientNoise2D(
        resolveOceanLattice(pos) * WATER_FOAM_LATTICE_SCALE,
        u_oceanNoisePeriod * WATER_FOAM_LATTICE_SCALE,
        WATER_FOAM_SEED) * 0.5 + 0.5;
}

float resolveCelMask(float value, float edge) {
    return smoothstep(0.5 - edge, 0.5 + edge, value);
}

// The whitecap and shore foam fields, each cut into foam where it crosses one half.
vec2 resolveFoamValues(OceanState state, float displacement, float shoreDepth, float foamNoise, float tidal) {
    float whitecap = tidal > 0.5 ? resolveOceanWhitecap(state, displacement) : 0.0;
    float cap      = whitecap * WATER_WHITECAP_GAIN - foamNoise + 0.5;

    float shoreReach = WATER_SHORE_FOAM_DEPTH * (1.0 + state.seaState * WATER_SHORE_STATE_GAIN);
    float shore      = 1.0 - smoothstep(0.0, shoreReach, shoreDepth);
    float ring       = 0.5 + 0.5 * sin(shoreDepth * WATER_SHORE_RING_FREQUENCY
        - u_oceanSurface.w * WATER_SHORE_RING_SPEED);

    return vec2(cap, shore * (1.0 - WATER_SHORE_RING_SHARE + WATER_SHORE_RING_SHARE * ring)
        + (foamNoise - 0.5) * WATER_FOAM_NOISE_SHARE);
}

float resolveFoam(vec2 foamValues, vec2 foamEdges) {
    return max(resolveCelMask(foamValues.x, foamEdges.x), resolveCelMask(foamValues.y, foamEdges.y));
}

// ── Glint ─────────────────────────────────────────────────────────────────

float resolveGlint(vec3 normal, vec3 viewDir, vec3 lightDir) {
    float specular = pow(max(dot(normal, normalize(viewDir + lightDir)), 0.0), WATER_GLINT_POWER);
    return smoothstep(WATER_GLINT_THRESHOLD - WATER_GLINT_SOFTNESS, WATER_GLINT_THRESHOLD + WATER_GLINT_SOFTNESS,
        specular);
}

vec3 resolveGlints(vec3 normal, vec3 viewDir) {
    float clearSky = 1.0 - u_skyBlend.w;
    vec3  sun      = u_sunColor * (u_sunIntensity * resolveGlint(normal, viewDir, normalize(u_sunDirection)));
    vec3  moon     = u_moonColor * WATER_MOON_TINT
        * (min(u_moonIntensity, WATER_MOON_MAX_INTENSITY) * resolveGlint(normal, viewDir, normalize(u_moonDirection)));

    return (sun * WATER_GLINT_STRENGTH + moon * WATER_MOON_GLINT_STRENGTH) * clearSky;
}

// ── Above ─────────────────────────────────────────────────────────────────

vec3 shadeFromAbove(WaterSurface surface, vec2 foamEdges) {
    WaterFragment fragment        = surface.fragment;
    vec2          screenUV        = surface.screenUV;
    vec3          normal          = surface.normal;
    vec3          viewDir         = surface.viewDir;
    float         surfaceDistance = surface.surfaceDistance;
    float         detail          = surface.detail;
    float         shoreDepth      = surface.shoreDepth;

    vec3  light        = resolveWaterLight(normal);
    vec2  offset       = normal.xz * WATER_REFRACTION_STRENGTH * clamp(shoreDepth / WATER_REFRACTION_REACH, 0.0, 1.0);
    vec2  refractUV    = clamp(screenUV + offset, vec2(0.0), vec2(1.0));
    float refractDepth = sampleWaterSceneDepth(refractUV);

    if (refractDepth < surface.fragmentDepth) {
        refractUV    = screenUV;
        refractDepth = surface.sceneDepth;
    }

    bool  seesNothing   = refractDepth >= 1.0;
    vec3  refractedView = reconstructWaterViewPos(refractUV, refractDepth);
    vec3  floorPos      = viewToWorld(refractedView);
    float floorDepth    = seesNothing ? WATER_UNSEEN_DEPTH_BLOCKS : max(fragment.worldPos.y - floorPos.y, 0.0);
    float thickness     = seesNothing ? WATER_UNSEEN_DEPTH_BLOCKS : max(length(refractedView) - surfaceDistance, 0.0);

    vec3 body  = resolveWaterBodyColor(floorDepth) * light;
    vec3 floorColor = seesNothing ? body : sampleWaterSceneColor(refractUV);

    if (!seesNothing && detail > 0.0)
    floorColor *= 1.0 + sampleWaterCaustics(resolveOceanLattice(floorPos.xz), floorDepth) * detail;

    vec3 transmittance = resolveWaterTransmittance(thickness);
    vec3 transmitted   = floorColor * transmittance + body * (1.0 - transmittance);

    vec3 reflectDir = reflect(-viewDir, normal);
    vec3 reflection = sampleWaterSky(reflectDir);

    if (detail > 0.0) {
        vec4 traced = traceWaterReflection((u_view * vec4(fragment.worldPos, 1.0)).xyz,
            normalize(mat3(u_view) * liftAboveHorizon(reflectDir)));

        reflection = mix(reflection, traced.rgb, traced.a * detail);
    }

    float facing  = max(dot(normal, viewDir), 0.0);
    float fresnel = (WATER_FRESNEL_BASE + (1.0 - WATER_FRESNEL_BASE) * pow(1.0 - facing, WATER_FRESNEL_POWER))
        * WATER_REFLECTION_STRENGTH;

    vec3  color = mix(transmitted, reflection, fresnel) + resolveGlints(normal, viewDir);
    float foam  = resolveFoam(surface.foamValues, foamEdges);

    color = mix(color, WATER_FOAM_COLOR * light, foam);
    color = applyAtmosphericFog(color, fragment.worldPos, -viewDir, resolveWaterLitAmount(normal));

    return compositeCloudScene(u_waterCloudColor, u_waterCloudDistance, screenUV, color, surfaceDistance);
}

// ── Below ─────────────────────────────────────────────────────────────────

vec3 shadeFromBelow(vec2 screenUV, float fragmentDepth, vec3 normal, vec3 viewDir, float surfaceDistance) {
    vec3  depths    = resolveUnderwaterFogColor(u_oceanCamera.z);
    vec3  refracted = refract(-viewDir, -normal, WATER_SNELL_ETA);
    float window    = dot(refracted, refracted) > 0.0
        ? smoothstep(0.0, WATER_SNELL_EDGE, dot(refracted, normal))
        : 0.0;

    vec3 above = depths;

    if (window > 0.0) {
        vec2  aboveUV    = clamp(screenUV - normal.xz * WATER_REFRACTION_STRENGTH, vec2(0.0), vec2(1.0));
        float aboveDepth = sampleWaterSceneDepth(aboveUV);

        above = aboveDepth >= 1.0 || aboveDepth < fragmentDepth
            ? sampleWaterSky(refracted)
            : sampleWaterSceneColor(aboveUV);
    }

    vec3 color = mix(depths, above, window);

    if (u_oceanCamera.x > 0.5)
    color = applyUnderwaterFog(color, surfaceDistance, u_oceanCamera.z);

    return color;
}

// ── Surface ───────────────────────────────────────────────────────────────

// Resolves one water fragment from its window position and depth; false where it is hidden behind the scene
// or inside a hull, which the caller discards.
bool resolveWaterSurface(WaterFragment fragment, vec3 fragCoord, out WaterSurface surface) {
    surface.fragment      = fragment;
    surface.screenUV      = resolveWaterScreenUV(fragCoord.xy);
    surface.sceneDepth    = sampleWaterSceneDepth(surface.screenUV);
    surface.fragmentDepth = fragCoord.z;

    surface.state.seaState = fragment.oceanState.x;
    surface.state.chop     = fragment.oceanState.y;
    surface.state.swell    = fragment.oceanState.z;

    if (fragCoord.z > surface.sceneDepth || isInsideOceanHull(fragment.worldPos))
    return false;

    vec3 faceNormal = normalize(fragment.faceNormal);
    vec3 toCamera   = u_cameraPosition - fragment.worldPos;

    surface.surfaceDistance = max(length(toCamera), WATER_DISTANCE_EPSILON);
    surface.viewDir         = toCamera / surface.surfaceDistance;
    surface.detail          = resolveWaterDetail(surface.surfaceDistance);
    surface.fromBelow       = dot(faceNormal, toCamera) < 0.0;
    surface.normal          = faceNormal;

    if (faceNormal.y > 0.5) {
        vec2 slope = surface.detail > 0.0
            ? sampleOceanRippleSlope(fragment.worldPos.xz, fragment.oceanState.x) * surface.detail
            : vec2(0.0);

        if (fragment.tidal > 0.5)
        slope += sampleOceanSlope(fragment.worldPos.xz, surface.state)
            * mix(WATER_FAR_WAVE_SLOPE_SHARE, 1.0, surface.detail);

        surface.normal = slopeToNormal(slope);
    }

    surface.shoreDepth = 0.0;
    surface.foamValues = vec2(0.0);

    if (!surface.fromBelow) {
        vec3 straightPos = viewToWorld(reconstructWaterViewPos(surface.screenUV, surface.sceneDepth));

        surface.shoreDepth = surface.sceneDepth >= 1.0 ? WATER_UNSEEN_DEPTH_BLOCKS
            : max(fragment.worldPos.y - straightPos.y, 0.0);
        surface.foamValues = resolveFoamValues(surface.state, fragment.oceanState.w, surface.shoreDepth,
            sampleFoamNoise(fragment.worldPos.xz), fragment.tidal);
    }

    return true;
}

vec3 shadeWaterSurface(WaterSurface surface, vec2 foamEdges) {
    return surface.fromBelow
        ? shadeFromBelow(surface.screenUV, surface.fragmentDepth, surface.normal, surface.viewDir,
            surface.surfaceDistance)
        : shadeFromAbove(surface, foamEdges);
}

#endif
