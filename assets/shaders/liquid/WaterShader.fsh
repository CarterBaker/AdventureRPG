#version 400 core

in vec3  vWorldPos;
flat in vec3 vFaceNormal;
in float vTidal;
in vec4  vOceanState;

out vec4 FragColor;

#include "includes/CameraData.glsl"
#include "liquid/includes/OceanSurface.glsl"
#include "liquid/includes/WaterLight.glsl"
#include "liquid/includes/WaterSceneData.glsl"
#include "liquid/includes/WaterReflection.glsl"
#include "postprocessing/lighting/includes/AtmosphericFog.glsl"

/*
 * Forward water, drawn after deferred lighting over the lit scene. Fragments behind opaque geometry are
 * discarded by hand against the scene depth. From above, the scene below is refracted through the wave
 * normal and absorbed with the thickness of water it is seen through, so shallows stay clear over sand and
 * deepen through banded turquoise, teal and blue; the sky, clouds and shore reflect by Fresnel; the sun
 * leaves a hard cartoon glint; whitecaps break on storm crests and foam lines ring every shore. From below,
 * the sky shows through Snell's window and the rest of the surface mirrors the depths, under the same fog
 * the underwater pass lays over the scene.
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

// ── Foam ──────────────────────────────────────────────────────────────────

float sampleFoamNoise(vec2 pos) {
    return periodicGradientNoise2D(
        resolveOceanLattice(pos) * WATER_FOAM_LATTICE_SCALE,
        u_oceanNoisePeriod * WATER_FOAM_LATTICE_SCALE,
        WATER_FOAM_SEED) * 0.5 + 0.5;
}

float resolveCelMask(float value) {
    float edge = max(fwidth(value), WATER_FOAM_EDGE_MIN);
    return smoothstep(0.5 - edge, 0.5 + edge, value);
}

float resolveFoam(OceanState state, float displacement, float shoreDepth, float foamNoise) {
    float whitecap = vTidal > 0.5 ? resolveOceanWhitecap(state, displacement) : 0.0;
    float capMask  = resolveCelMask(whitecap * WATER_WHITECAP_GAIN - foamNoise + 0.5);

    float shoreReach = WATER_SHORE_FOAM_DEPTH * (1.0 + state.seaState * WATER_SHORE_STATE_GAIN);
    float shore      = 1.0 - smoothstep(0.0, shoreReach, shoreDepth);
    float ring       = 0.5 + 0.5 * sin(shoreDepth * WATER_SHORE_RING_FREQUENCY
        - u_oceanSurface.w * WATER_SHORE_RING_SPEED);
    float shoreMask  = resolveCelMask(shore * (1.0 - WATER_SHORE_RING_SHARE + WATER_SHORE_RING_SHARE * ring)
        + (foamNoise - 0.5) * WATER_FOAM_NOISE_SHARE);

    return max(capMask, shoreMask);
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

vec3 shadeFromAbove(vec2 screenUV, float sceneDepth, vec3 normal, vec3 viewDir, float surfaceDistance) {
    OceanState state;
    state.seaState = vOceanState.x;
    state.chop     = vOceanState.y;
    state.swell    = vOceanState.z;

    vec3  light       = resolveWaterLight(normal);
    vec3  straightPos = viewToWorld(reconstructWaterViewPos(screenUV, sceneDepth));
    float shoreDepth  = sceneDepth >= 1.0 ? WATER_UNSEEN_DEPTH_BLOCKS : max(vWorldPos.y - straightPos.y, 0.0);

    vec2  offset       = normal.xz * WATER_REFRACTION_STRENGTH * clamp(shoreDepth / WATER_REFRACTION_REACH, 0.0, 1.0);
    vec2  refractUV    = clamp(screenUV + offset, vec2(0.0), vec2(1.0));
    float refractDepth = sampleWaterSceneDepth(refractUV);

    if (refractDepth < gl_FragCoord.z) {
        refractUV    = screenUV;
        refractDepth = sceneDepth;
    }

    bool  seesNothing   = refractDepth >= 1.0;
    vec3  refractedView = reconstructWaterViewPos(refractUV, refractDepth);
    vec3  floorPos      = viewToWorld(refractedView);
    float floorDepth    = seesNothing ? WATER_UNSEEN_DEPTH_BLOCKS : max(vWorldPos.y - floorPos.y, 0.0);
    float thickness     = seesNothing ? WATER_UNSEEN_DEPTH_BLOCKS : max(length(refractedView) - surfaceDistance, 0.0);

    vec3 body  = resolveWaterBodyColor(floorDepth) * light;
    vec3 floorColor = seesNothing ? body : sampleWaterSceneColor(refractUV);

    if (!seesNothing)
    floorColor *= 1.0 + sampleWaterCaustics(resolveOceanLattice(floorPos.xz), floorDepth);

    vec3 transmittance = resolveWaterTransmittance(thickness);
    vec3 transmitted   = floorColor * transmittance + body * (1.0 - transmittance);

    vec3 reflectDir = reflect(-viewDir, normal);
    vec3 reflection = sampleWaterSky(reflectDir);
    vec4 traced     = traceWaterReflection((u_view * vec4(vWorldPos, 1.0)).xyz,
        normalize(mat3(u_view) * liftAboveHorizon(reflectDir)));

    reflection = mix(reflection, traced.rgb, traced.a);

    float facing  = max(dot(normal, viewDir), 0.0);
    float fresnel = (WATER_FRESNEL_BASE + (1.0 - WATER_FRESNEL_BASE) * pow(1.0 - facing, WATER_FRESNEL_POWER))
        * WATER_REFLECTION_STRENGTH;

    vec3  color = mix(transmitted, reflection, fresnel) + resolveGlints(normal, viewDir);
    float foam  = resolveFoam(state, vOceanState.w, shoreDepth, sampleFoamNoise(vWorldPos.xz));

    color = mix(color, WATER_FOAM_COLOR * light, foam);

    return applyAtmosphericFog(color, vWorldPos, -viewDir, resolveWaterLitAmount(normal));
}

// ── Below ─────────────────────────────────────────────────────────────────

vec3 shadeFromBelow(vec2 screenUV, vec3 normal, vec3 viewDir, float surfaceDistance) {
    vec3  depths    = resolveUnderwaterFogColor(u_oceanCamera.z);
    vec3  refracted = refract(-viewDir, -normal, WATER_SNELL_ETA);
    float window    = dot(refracted, refracted) > 0.0
        ? smoothstep(0.0, WATER_SNELL_EDGE, dot(refracted, normal))
        : 0.0;

    vec3 above = depths;

    if (window > 0.0) {
        vec2  aboveUV    = clamp(screenUV - normal.xz * WATER_REFRACTION_STRENGTH, vec2(0.0), vec2(1.0));
        float aboveDepth = sampleWaterSceneDepth(aboveUV);

        above = aboveDepth >= 1.0 || aboveDepth < gl_FragCoord.z
            ? sampleWaterSky(refracted)
            : sampleWaterSceneColor(aboveUV);
    }

    vec3 color = mix(depths, above, window);

    if (u_oceanCamera.x > 0.5)
    color = applyUnderwaterFog(color, surfaceDistance, u_oceanCamera.z);

    return color;
}

// ── Main ──────────────────────────────────────────────────────────────────

void main() {
    vec2  screenUV   = resolveWaterScreenUV(gl_FragCoord.xy);
    float sceneDepth = sampleWaterSceneDepth(screenUV);

    if (gl_FragCoord.z > sceneDepth)
    discard;

    vec3  faceNormal      = normalize(vFaceNormal);
    vec3  toCamera        = u_cameraPosition - vWorldPos;
    float surfaceDistance = max(length(toCamera), WATER_DISTANCE_EPSILON);
    vec3  viewDir         = toCamera / surfaceDistance;
    vec3  normal          = faceNormal;

    if (faceNormal.y > 0.5) {
        vec2 slope = sampleOceanRippleSlope(vWorldPos.xz, vOceanState.x);

        if (vTidal > 0.5) {
            OceanState state;
            state.seaState = vOceanState.x;
            state.chop     = vOceanState.y;
            state.swell    = vOceanState.z;
            slope += sampleOceanSlope(vWorldPos.xz, state);
        }

        normal = slopeToNormal(slope);
    }

    vec3 color = dot(faceNormal, toCamera) < 0.0
        ? shadeFromBelow(screenUV, normal, viewDir, surfaceDistance)
        : shadeFromAbove(screenUV, sceneDepth, normal, viewDir, surfaceDistance);

    FragColor = vec4(color, 1.0);
}
