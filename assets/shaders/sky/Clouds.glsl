#ifndef CLOUDS_GLSL
#define CLOUDS_GLSL

vec3 calculateClouds(vec3 dir, float dailySeed) {
    // -------------------------------
    // Horizon band only (distant clouds)
    // -------------------------------
    float altitude = dir.y;  // 0 = horizon
    float bandHalf = 0.05;   // very thin horizontal band
    if (abs(altitude) > bandHalf) return vec3(0.0);

    // -------------------------------
    // Horizontal angle around camera
    // -------------------------------
    float angle = atan(dir.z, dir.x);  // -pi..pi
    float u = angle / 3.14159265;      // normalize -1..1

    // -------------------------------
    // Drift across the sky (0 to 180 deg)
    // -------------------------------
    vec3 cloudSample = vec3(u * 2.0, 0.0, dailySeed);

    // TODO: replace 0.01 with uniform for cloud speed if desired
    cloudSample.x += u_time * 0.01;  // horizontal movement across horizon

    // -------------------------------
    // Cloud existence (gaps, smooth long-term pattern)
    // -------------------------------
    float existence = smoothNoise3D(cloudSample * 0.3);
    existence = smoothstep(0.4, 0.7, existence);
    if (existence < 0.1) return vec3(0.0);

    // -------------------------------
    // Simple stretched circles
    // -------------------------------
    float cloudShape = cellNoise(cloudSample * 0.6);
    cloudShape = smoothstep(0.35, 0.65, cloudShape);

    // Slight vertical fade at band edges (optional)
    cloudShape *= 1.0 - 0.2 * abs(altitude / bandHalf);

    // Apply existence
    cloudShape *= existence;

    // -------------------------------
    // Return white distant clouds
    // -------------------------------
    return vec3(cloudShape);
}

#endif
