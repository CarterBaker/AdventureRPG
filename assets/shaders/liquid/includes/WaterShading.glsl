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
 * sparkles nor costs what near water does. Running fresh water is a cartoon river: its body takes the river's
 * own bright azure palette and is seen through as though deeper, so the bed only tints it; its top faces drift
 * their ripples downstream, stand in rows of V-shaped waves whose points lead downstream and whose crests now and
 * then break white, bumping the surface the light catches, and streak with long pale dashes racing the way the
 * column runs; the pool at a fall's foot churns with billowing puffs, whiter the further the water dropped onto it;
 * and a fall's face is a sheet of the same azure that all but hides what stands behind it, pouring with pale threads
 * and white water from a glassy lip, both carried at one pace through the time the water has been falling, so they
 * race faster and stretch longer the further down the face they are, and the white breaks through more as the drop
 * grows. White water joins the whitecap field and the pale streaks are a third field cut the same way, so each
 * takes the same hard cel edge every foam line has. The flow code is LiquidFlowUtility's: zero still, one to
 * WATER_FLOW_DIRECTIONS a way round from east toward south, and WATER_FLOW_CHURN churning; the fall is the height
 * of an upright face's lip, or the drop poured onto a churning top face.
 *
 * The engine prepends a program's includes to every one of its stages, so nothing here may touch what only a
 * fragment stage has. A fragment shader resolves its WaterSurface from its own gl_FragCoord, measures the foam
 * edges with fwidth itself, then shades:
 *
 *     WaterSurface surface;
 *     if (!resolveWaterSurface(fragment, gl_FragCoord.xyz, surface)) discard;
 *     vec3 foamEdges = max(fwidth(surface.foamValues), vec3(WATER_FOAM_EDGE_MIN));
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
const float WATER_FLOW_DIRECTIONS      = 16.0;
const float WATER_FLOW_CHURN           = 17.0;
const float WATER_FLOW_SPEED           = 2.4;
const float WATER_LINE_ACROSS_SCALE    = 2.4;
const float WATER_LINE_ALONG_SCALE     = 0.22;
const float WATER_LINE_THRESHOLD       = 0.6;
const float WATER_LINE_GAIN            = 5.0;
const float WATER_RAPID_BAND_RATE      = 0.9;
const float WATER_RAPID_LANE_RATE      = 0.45;
const float WATER_RAPID_SLANT          = 0.7;
const float WATER_RAPID_DRIFT          = 0.35;
const float WATER_RAPID_WARP           = 0.6;
const float WATER_RAPID_WARP_SCALE     = 0.35;
const float WATER_RAPID_CREST          = 0.3;
const float WATER_RAPID_BREAKUP_SCALE  = 0.8;
const float WATER_RAPID_BREAKUP        = 0.55;
const float WATER_RAPID_GAIN           = 3.5;
const float WATER_RAPID_HEIGHT         = 0.035;
const vec3  WATER_STREAK_COLOR         = vec3(0.80, 0.96, 1.00);
const float WATER_RIVER_DEPTH_GAIN     = 1.6;
const float WATER_RIVER_THICKNESS_GAIN = 3.0;
const float WATER_CHURN_SCALE          = 0.9;
const float WATER_CHURN_DETAIL_SCALE   = 2.3;
const float WATER_CHURN_SPEED          = 0.9;
const float WATER_CHURN_THRESHOLD      = 0.38;
const float WATER_CHURN_GAIN           = 3.0;
const float WATER_CHURN_FULL_DROP      = 12.0;
const float WATER_CHURN_DROP_SHARE     = 0.14;
const float WATER_FALL_GRAVITY         = 24.0;
const float WATER_FALL_LIP_START       = 0.08;
const float WATER_FALL_PACE            = 1.0;
const float WATER_FALL_ACROSS_SCALE    = 2.2;
const float WATER_FALL_ALONG_SCALE     = 2.4;
const float WATER_FALL_THRESHOLD       = 0.52;
const float WATER_FALL_GAIN            = 3.5;
const float WATER_FALL_WHITE_DROP      = 8.0;
const float WATER_FALL_WHITE_SHARE     = 0.22;
const float WATER_FALL_THREAD_ACROSS   = 4.5;
const float WATER_FALL_THREAD_ALONG    = 0.9;
const float WATER_FALL_THREAD_CUT      = 0.58;
const float WATER_FALL_THREAD_GAIN     = 6.0;
const float WATER_FALL_LIP_BLOCKS      = 0.16;
const float WATER_FALL_LIP_GAIN        = 8.0;
const float WATER_FALL_OPACITY         = 0.86;
const float WATER_FALL_BODY_DEPTH      = 3.0;
const float WATER_FLOW_NOISE_PERIOD    = 64.0;
const uint  WATER_FLOW_SEED            = 5153u;
const float WATER_TWO_PI               = 6.28318531;

struct WaterFragment {
    vec3  worldPos;
    vec3  faceNormal;
    float tidal;
    vec4  oceanState;
    float flow;
    float fall;
};

// Everything shading needs about one fragment, resolved before its foam edges, which only the fragment stage
// can measure. foamValues holds the whitecap and shore foam fields and the pale flow streaks the cel masks cut at
// one half.
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
    vec3          foamValues;
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

// The whitecap and shore foam fields, each cut into foam where it crosses one half
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

float resolveFoam(vec3 foamValues, vec3 foamEdges) {
    return max(resolveCelMask(foamValues.x, foamEdges.x), resolveCelMask(foamValues.y, foamEdges.y));
}

float resolveStreak(vec3 foamValues, vec3 foamEdges) {
    return resolveCelMask(foamValues.z, foamEdges.z);
}

// ── Flow ──────────────────────────────────────────────────────────────────

bool isRunningFlow(float flow) {
    return flow > 0.5 && flow < WATER_FLOW_DIRECTIONS + 0.5;
}

// Whether a fragment lies on a fall's face: an upright face off the tide
bool isFallingWater(WaterFragment fragment, vec3 faceNormal) {
    return fragment.tidal < 0.5 && abs(faceNormal.y) < 0.5;
}

// Whether a top face is fresh water on the move: a river running, or the pool churning at a fall's foot
bool isRiverWater(WaterFragment fragment) {
    return fragment.tidal < 0.5 && fragment.flow > 0.5;
}

// The seconds the water has been falling from its fall's lip when it reaches a point of the face, so a pattern
// carried through them at one pace pours ever faster down the face and stretches as it goes, as falling water does
float resolveFallTravel(WaterFragment fragment) {
    float drop = max(fragment.fall - fragment.worldPos.y, 0.0) + WATER_FALL_LIP_START;
    return sqrt(2.0 * drop / WATER_FALL_GRAVITY);
}

// The way a running flow code points across the world's x and z
vec2 resolveFlowDirection(float flow) {
    float angle = (floor(flow + 0.5) - 1.0) / WATER_FLOW_DIRECTIONS * WATER_TWO_PI;
    return vec2(cos(angle), sin(angle));
}

float sampleFlowNoise(vec2 lattice, uint seed) {
    return periodicGradientNoise2D(lattice, vec2(WATER_FLOW_NOISE_PERIOD), seed) * 0.5 + 0.5;
}

// Where a point of running water stands among the rapids' rows of V-shaped waves: the phase of the row it is in,
// whole at every crest, each row's middle leading its arms downstream and creeping slowly that way, warped so no two
// rows run true; and, through lane, which side of its V's middle it stands on
float resolveRapidPhase(vec2 pos, vec2 down, vec2 across, out float lane) {
    float time = u_oceanSurface.w;
    float warp = sampleFlowNoise(pos * WATER_RAPID_WARP_SCALE, WATER_FLOW_SEED + 4u) - 0.5;

    lane = fract(dot(pos, across) * WATER_RAPID_LANE_RATE) - 0.5;

    return dot(pos, down) * WATER_RAPID_BAND_RATE + abs(lane) * WATER_RAPID_SLANT - time * WATER_RAPID_DRIFT
        + warp * WATER_RAPID_WARP;
}

// The white caps breaking along the rapids' crests, crossing one half where they show, broken into lumps
float resolveRapidFoam(vec2 pos, vec2 down, vec2 across) {
    float lane;
    float phase   = resolveRapidPhase(pos, down, across, lane);
    float crest   = abs(fract(phase) - 0.5) * 2.0 - (1.0 - 2.0 * WATER_RAPID_CREST);
    float breakup = sampleFlowNoise(pos * WATER_RAPID_BREAKUP_SCALE - down * (u_oceanSurface.w * WATER_FLOW_SPEED),
        WATER_FLOW_SEED + 5u) - WATER_RAPID_BREAKUP;

    return 0.5 + min(crest, breakup) * WATER_RAPID_GAIN;
}

// The slope of the rapids' standing waves on running water's top faces, nothing elsewhere
vec2 resolveRapidSlope(WaterFragment fragment) {

    if (fragment.tidal > 0.5 || !isRunningFlow(fragment.flow))
    return vec2(0.0);

    vec2  down   = resolveFlowDirection(fragment.flow);
    vec2  across = vec2(-down.y, down.x);
    float lane;
    float phase  = resolveRapidPhase(fragment.worldPos.xz, down, across, lane);
    vec2  rise   = down * WATER_RAPID_BAND_RATE + across * (sign(lane) * WATER_RAPID_SLANT * WATER_RAPID_LANE_RATE);

    return rise * (cos(phase * WATER_TWO_PI) * WATER_TWO_PI * WATER_RAPID_HEIGHT);
}

// The white water running water adds, crossing one half where it shows: white breaking through a fall's face, more
// the further it has fallen; puffs churning at a fall's foot, more the further the water dropped onto it; and the
// rapids' breaking crests down a river; none on still or tidal water
float resolveFlowFoam(WaterFragment fragment, vec3 faceNormal) {
    float time = u_oceanSurface.w;
    vec3  pos  = fragment.worldPos;

    if (fragment.tidal > 0.5)
    return 0.0;

    if (isFallingWater(fragment, faceNormal)) {
        vec2  across = vec2(-faceNormal.z, faceNormal.x);
        vec2  fall   = vec2(dot(pos.xz, across) * WATER_FALL_ACROSS_SCALE,
            (resolveFallTravel(fragment) - time * WATER_FALL_PACE) * WATER_FALL_ALONG_SCALE);
        float white  = smoothstep(0.0, WATER_FALL_WHITE_DROP, fragment.fall - pos.y) * WATER_FALL_WHITE_SHARE;
        return 0.5 + (sampleFlowNoise(fall, WATER_FLOW_SEED) - WATER_FALL_THRESHOLD + white) * WATER_FALL_GAIN;
    }

    if (fragment.flow > WATER_FLOW_CHURN - 0.5) {
        float billow = sampleFlowNoise(pos.xz * WATER_CHURN_SCALE + vec2(0.0, time * WATER_CHURN_SPEED),
            WATER_FLOW_SEED + 1u);
        float detail = sampleFlowNoise(pos.xz * WATER_CHURN_DETAIL_SCALE - vec2(time * WATER_CHURN_SPEED),
            WATER_FLOW_SEED + 2u);
        float drop   = smoothstep(1.0, WATER_CHURN_FULL_DROP, fragment.fall) * WATER_CHURN_DROP_SHARE;
        return 0.5 + (billow * 0.65 + detail * 0.35 - WATER_CHURN_THRESHOLD + drop) * WATER_CHURN_GAIN;
    }

    if (!isRunningFlow(fragment.flow) || abs(faceNormal.y) < 0.5)
    return 0.0;

    vec2 down = resolveFlowDirection(fragment.flow);

    return resolveRapidFoam(pos.xz, down, vec2(-down.y, down.x));
}

// The pale streaks running water carries, crossing one half where they show: long dashes racing down a river the way
// its column runs, and thin threads pouring down a fall's face under the glassy band where it curls over its lip
float resolveFlowStreaks(WaterFragment fragment, vec3 faceNormal) {
    float time = u_oceanSurface.w;
    vec3  pos  = fragment.worldPos;

    if (isFallingWater(fragment, faceNormal)) {
        vec2  across = vec2(-faceNormal.z, faceNormal.x);
        vec2  thread = vec2(dot(pos.xz, across) * WATER_FALL_THREAD_ACROSS,
            (resolveFallTravel(fragment) - time * WATER_FALL_PACE) * WATER_FALL_THREAD_ALONG);
        float lip    = 0.5 + (WATER_FALL_LIP_BLOCKS - (fragment.fall - pos.y)) * WATER_FALL_LIP_GAIN;
        return max(0.5 + (sampleFlowNoise(thread, WATER_FLOW_SEED + 6u) - WATER_FALL_THREAD_CUT)
            * WATER_FALL_THREAD_GAIN, lip);
    }

    if (fragment.tidal > 0.5 || !isRunningFlow(fragment.flow) || faceNormal.y < 0.5)
    return 0.0;

    vec2 down   = resolveFlowDirection(fragment.flow);
    vec2 across = vec2(-down.y, down.x);
    vec2 line   = vec2(dot(pos.xz, across) * WATER_LINE_ACROSS_SCALE,
        (dot(pos.xz, down) - time * WATER_FLOW_SPEED) * WATER_LINE_ALONG_SCALE);

    return 0.5 + (sampleFlowNoise(line, WATER_FLOW_SEED + 3u) - WATER_LINE_THRESHOLD) * WATER_LINE_GAIN;
}

// Where a top face's ripples are read: carried downstream on running water, in place elsewhere
vec2 resolveRipplePosition(WaterFragment fragment) {
    return isRunningFlow(fragment.flow)
        ? fragment.worldPos.xz - resolveFlowDirection(fragment.flow) * (u_oceanSurface.w * WATER_FLOW_SPEED)
        : fragment.worldPos.xz;
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

// A fall's face: a sheet of river azure all but hiding what stands behind it, the sky caught on it by Fresnel, pale
// threads and white water pouring down it from its glassy lip
vec3 shadeFall(WaterSurface surface, vec3 foamEdges) {
    WaterFragment fragment = surface.fragment;
    vec3          normal   = surface.normal;
    vec3          viewDir  = surface.viewDir;

    vec3  light   = resolveWaterLight(normal);
    vec3  body    = resolveRiverBodyColor(WATER_FALL_BODY_DEPTH) * light;
    vec3  sheet   = mix(sampleWaterSceneColor(surface.screenUV), body, WATER_FALL_OPACITY);
    float facing  = max(dot(normal, viewDir), 0.0);
    float fresnel = (WATER_FRESNEL_BASE + (1.0 - WATER_FRESNEL_BASE) * pow(1.0 - facing, WATER_FRESNEL_POWER))
        * WATER_REFLECTION_STRENGTH;

    vec3 color = mix(sheet, sampleWaterSky(reflect(-viewDir, normal)), fresnel);

    color = mix(color, WATER_STREAK_COLOR * light, resolveStreak(surface.foamValues, foamEdges));
    color = mix(color, WATER_FOAM_COLOR * light, resolveFoam(surface.foamValues, foamEdges));
    color = applyAtmosphericFog(color, fragment.worldPos, -viewDir, resolveWaterLitAmount(normal));

    return compositeCloudScene(u_waterCloudColor, u_waterCloudDistance, surface.screenUV, color,
        surface.surfaceDistance);
}

vec3 shadeFromAbove(WaterSurface surface, vec3 foamEdges) {
    WaterFragment fragment        = surface.fragment;
    vec2          screenUV        = surface.screenUV;
    vec3          normal          = surface.normal;
    vec3          viewDir         = surface.viewDir;
    float         surfaceDistance = surface.surfaceDistance;
    float         detail          = surface.detail;
    float         shoreDepth      = surface.shoreDepth;

    if (isFallingWater(fragment, normalize(fragment.faceNormal)))
    return shadeFall(surface, foamEdges);

    float river        = isRiverWater(fragment) ? 1.0 : 0.0;
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

    vec3 body  = mix(resolveWaterBodyColor(floorDepth), resolveRiverBodyColor(floorDepth * WATER_RIVER_DEPTH_GAIN),
        river) * light;
    vec3 floorColor = seesNothing ? body : sampleWaterSceneColor(refractUV);

    if (!seesNothing && detail > 0.0)
    floorColor *= 1.0 + sampleWaterCaustics(resolveOceanLattice(floorPos.xz), floorDepth) * detail;

    vec3 transmittance = resolveWaterTransmittance(thickness * mix(1.0, WATER_RIVER_THICKNESS_GAIN, river));
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

    color = mix(color, WATER_STREAK_COLOR * light, resolveStreak(surface.foamValues, foamEdges));
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
            ? sampleOceanRippleSlope(resolveRipplePosition(fragment), fragment.oceanState.x) * surface.detail
            : vec2(0.0);

        if (fragment.tidal > 0.5)
        slope += sampleOceanSlope(fragment.worldPos.xz, surface.state)
            * mix(WATER_FAR_WAVE_SLOPE_SHARE, 1.0, surface.detail);

        slope += resolveRapidSlope(fragment) * mix(WATER_FAR_WAVE_SLOPE_SHARE, 1.0, surface.detail);

        surface.normal = slopeToNormal(slope);
    }

    surface.shoreDepth = 0.0;
    surface.foamValues = vec3(0.0);

    if (!surface.fromBelow) {
        vec3 straightPos = viewToWorld(reconstructWaterViewPos(surface.screenUV, surface.sceneDepth));

        surface.shoreDepth = surface.sceneDepth >= 1.0 || isFallingWater(fragment, faceNormal)
            ? WATER_UNSEEN_DEPTH_BLOCKS
            : max(fragment.worldPos.y - straightPos.y, 0.0);
        surface.foamValues = vec3(
            resolveFoamValues(surface.state, fragment.oceanState.w, surface.shoreDepth,
                sampleFoamNoise(fragment.worldPos.xz), fragment.tidal),
            resolveFlowStreaks(fragment, faceNormal));
        surface.foamValues.x = max(surface.foamValues.x, resolveFlowFoam(fragment, faceNormal));
    }

    return true;
}

vec3 shadeWaterSurface(WaterSurface surface, vec3 foamEdges) {
    return surface.fromBelow
        ? shadeFromBelow(surface.screenUV, surface.fragmentDepth, surface.normal, surface.viewDir,
            surface.surfaceDistance)
        : shadeFromAbove(surface, foamEdges);
}

#endif
