#version 150
in vec3 v_dir;
out vec4 fragColor;

#include "includes/NoiseUtility.glsl"
#include "includes/TimeData.glsl"

/* ---- CONSTANTS ---- */
const float c_tintStrength = 0.45;
const vec3 c_baseColor = vec3(0.42, 0.62, 1.0);
const vec3 c_dayLight = vec3(0.80, 0.85, 0.90);
const vec3 c_nightDark = vec3(0.01, 0.015, 0.04);
const float c_starSpeed = 0.0008;
const float PI = 3.14159265359;

void main() {
    vec3 dir = normalize(v_dir);

    /* ---- TIME ---- */
    float nightFactor = abs(1.0 - 2.0 * u_timeOfDay);
    float dayFactor = 1.0 - nightFactor;

    /* ---- ALTITUDE ---- */
    float altitude = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    altitude = smoothstep(0.0, 1.0, altitude);

    /* ---- BASE SKY ---- */
    vec3 daySky = mix(c_baseColor, c_dayLight, altitude);
    vec3 nightSky = mix(c_nightDark, vec3(0.03, 0.05, 0.12), altitude);
    vec3 sky = mix(nightSky, daySky, dayFactor);

    /* ---- TWILIGHT MASK ---- */
    float twilight = smoothstep(0.0, 0.25, dayFactor) * smoothstep(1.0, 0.75, dayFactor);

    /* ---- DAILY SUNSET VARIATION ---- */
    float daySeed = fract(u_randomNoiseFromDay);
    vec3 sunsetA = vec3(1.0, 0.55, 0.4);
    vec3 sunsetB = vec3(0.9, 0.4, 0.7);
    vec3 sunsetTint = mix(sunsetA, sunsetB, daySeed);
    float horizonMask = smoothstep(0.0, 0.5, 1.0 - altitude);
    sky = mix(sky, sky * sunsetTint, horizonMask * twilight * c_tintStrength);

    /* ---- CLOUDS (SEAMLESS) ---- */
    float phi = atan(dir.z, dir.x);
    float theta = acos(clamp(dir.y, -1.0, 1.0));

    vec2 uv = vec2(phi / (2.0 * PI), theta / PI);
    uv = uv * vec2(4.0, 2.4) + vec2(u_time * 0.015, u_time * 0.01);

    vec3 cloudPos = vec3(uv, u_randomNoiseFromDay * 10.0);
    float cloudNoise = fbmNoise3D(cloudPos);

    float cloudFade = smoothstep(0.05, 0.6, 1.0 - altitude);
    float cloudAmt = cloudNoise * 0.25 * cloudFade * mix(0.3, 1.0, dayFactor);
    vec3 cloudColor = mix(vec3(0.15), vec3(1.0), dayFactor);
    sky = mix(sky, cloudColor, cloudAmt);

    /* ---- STARS ---- */
    float angle = u_time * c_starSpeed;
    mat2 rot = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    vec3 starDir = normalize(vec3(rot * dir.xz, dir.y));

    vec3 starCell = floor(starDir * 120.0);
    float starSeed = hash31(starCell);
    float starOn = step(0.985, starSeed);
    float starBright = pow(fract(starSeed * 931.7), 18.0);
    float twinkle = 0.85 + 0.15 * sin(u_time * 0.8 + starSeed * 6.28);
    float starFade = smoothstep(0.1, 0.6, altitude) * nightFactor;
    sky += vec3(starOn * starBright * twinkle * starFade);

    fragColor = vec4(sky, 1.0);
}