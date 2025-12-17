#version 150

in vec3 v_dir;
out vec4 fragColor;

#include "CameraData.glsl"
#include "NoiseUtility.glsl"
#include "TimeData.glsl"

uniform float u_overcast;

const float c_tintStrength = 0.45;
const vec3 c_baseColor = vec3(0.4, 0.6, 1.0);
const vec3 c_blendLight = vec3(0.8, 0.85, 0.9);
const vec3 c_blendDark = vec3(0.005, 0.015, 0.05);
const float c_horizon = 0.5;
const float c_starSpeed = 0.001;

void main() {
    // Time-based factor (0 = night, 1 = day)
    float invertedTimeFactor = abs(1.0 - 2.0 * u_timeOfDay);
    float timeFactor = 1.0 - invertedTimeFactor;
    float timeSeed = timeFactor * u_randomNoiseFromDay;

    // Vertical gradient factor
    float t = clamp(v_dir.y * 0.7 + 0.3, 0.001, 1.0);
    t = pow(t, 1.2);

    // Calculate and blend the clouds into the sky
    vec2 cloudDrift = vec2(u_time * 0.02, u_time * 0.01);
    float softness = mix(2.0, 6.0, timeFactor);
    vec3 noisePos = v_dir * softness + vec3(cloudDrift, 0.0) + timeSeed * 10.0;
    float cloudNoise = smoothNoise3D(noisePos);
    float cloudMask = clamp((c_horizon - v_dir.y) / c_horizon, 0.001, 1.0);
    vec3 cloudColor = mix(c_blendDark, vec3(1.0), timeFactor);
    float cloudStrength = cloudNoise * 0.25 * cloudMask;

    // Stars
    vec3 dir = normalize(v_dir);
    float angle = u_time * c_starSpeed;
    float cosA = cos(angle);
    float sinA = sin(angle);
    vec3 rotatedDir = vec3(dir.x * cosA - dir.z * sinA, dir.y, dir.x * sinA + dir.z * cosA);

    vec3 starBasePos = floor(rotatedDir * 100.0);
    float starSeed = hash31(starBasePos);
    float hasStar = step(0.97, starSeed);
    float starBrightness = pow(fract(starSeed * 1234.5), 20.0);
    float starSpeed = 0.03 + 0.02 * hash31(starBasePos + vec3(2.0));
    float twinkleSeed = hash31(starBasePos + vec3(1.0, 0.0, 0.0));
    float twinklePhase = u_time * starSpeed + twinkleSeed * 6.28;
    float twinkle = 0.85 + 0.15 * sin(twinklePhase) * sin(twinklePhase);
    vec3 stars = vec3(hasStar * starBrightness * twinkle);
    stars *= invertedTimeFactor;

    // Overcast factor
    float overcastBlend = u_overcast * timeFactor;

    // Calculate the sky color based on time of day
    vec3 tint = mix(vec3(0.4, 0.6, 1.0), vec3(1.0, 0.5, 0.7), fract(u_randomNoiseFromDay));
    tint = tint * invertedTimeFactor;
    vec3 tintedBase = mix(c_baseColor, tint, c_tintStrength);
    vec3 dayNight = mix(c_blendDark, c_blendLight, timeFactor);
    vec3 sky = mix(dayNight, tintedBase, t * timeFactor);
    sky = mix(sky, cloudColor, cloudStrength);
    sky = mix(sky, stars, invertedTimeFactor);
    sky = mix(sky, c_blendDark, overcastBlend);

    fragColor = vec4(sky, 1.0);
}