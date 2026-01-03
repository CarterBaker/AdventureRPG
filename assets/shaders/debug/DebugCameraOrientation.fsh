#version 150
in vec3 v_dir;
in vec2 v_screenPos;
out vec4 fragColor;

#include "includes/CameraData.glsl"

void main() {
    vec3 dir = normalize(v_dir);

    // Correct aspect ratio for UI elements
    vec2 uv = v_screenPos;
    float aspectRatio = u_viewport.x / u_viewport.y;
    uv.x *= aspectRatio;  // Fix aspect ratio stretching

    float cornerOffset = 0.85;

    // CHECKERED BACKGROUND based on view direction
    // Use spherical coordinates for stable checker pattern
    float phi = atan(dir.z, dir.x);
    float theta = acos(clamp(dir.y, -1.0, 1.0));

    // Create checker pattern
    float checkerSize = 0.3;
    float checker1 = mod(floor(phi / checkerSize) + floor(theta / checkerSize), 2.0);

    // Color the checkers based on direction
    vec3 color1 = vec3(0.2);
    vec3 color2 = vec3(0.4);

    // Add directional tint
    color1.r += max(0.0, dir.x) * 0.3;
    color1.gb += max(0.0, -dir.x) * 0.3;
    color1.g += max(0.0, dir.y) * 0.3;
    color1.b += max(0.0, -dir.y) * 0.3;

    color2.r += max(0.0, dir.x) * 0.5;
    color2.gb += max(0.0, -dir.x) * 0.5;
    color2.g += max(0.0, dir.y) * 0.5;
    color2.b += max(0.0, -dir.y) * 0.5;

    vec3 bgColor = mix(color1, color2, checker1);

    // ROLL DETECTION - Extract from view matrix
    vec3 worldUp = vec3(0.0, 1.0, 0.0);

    // Extract camera's actual up vector from the view matrix
    // The view matrix's second column is the camera's up direction in world space
    vec3 cameraUpFromMatrix = normalize(vec3(u_view[0][1], u_view[1][1], u_view[2][1]));
    vec3 cameraRightFromMatrix = normalize(vec3(u_view[0][0], u_view[1][0], u_view[2][0]));

    // For HUD elements, compute based on view direction
    vec3 rightDir = normalize(cross(dir, worldUp));
    vec3 upDir = normalize(cross(rightDir, dir));
    float upAlignment = dot(upDir, worldUp);
    float rollAmount = 1.0 - abs(upAlignment);

    // Calculate actual roll angle using camera's real up vector
    // Project both vectors onto the plane perpendicular to view direction
    vec3 viewDir = normalize(dir);
    vec3 projectedCameraUp = normalize(cameraUpFromMatrix - dot(cameraUpFromMatrix, viewDir) * viewDir);
    vec3 projectedWorldUp = normalize(worldUp - dot(worldUp, viewDir) * viewDir);

    // Use atan2 to get the angle between them
    float rollAngle =
    atan(dot(cross(projectedWorldUp, projectedCameraUp), viewDir), dot(projectedWorldUp, projectedCameraUp));

    // CROSSHAIR - rotates based on camera roll
    float crosshairLength = 0.1;
    float crosshairThickness = 0.003;

    // Rotate UV coordinates by roll angle (already calculated above)
    float cosRoll = cos(-rollAngle);
    float sinRoll = sin(-rollAngle);
    vec2 rotatedUV = vec2(uv.x * cosRoll - uv.y * sinRoll, uv.x * sinRoll + uv.y * cosRoll);

    float horizontalLine = step(abs(rotatedUV.y), crosshairThickness) * step(abs(rotatedUV.x), crosshairLength);
    float verticalLine = step(abs(rotatedUV.x), crosshairThickness) * step(abs(rotatedUV.y), crosshairLength);
    float crosshair = max(horizontalLine, verticalLine);

    if (crosshair > 0.5) {
        if (horizontalLine > 0.5) {
            // Horizontal line = roll indicator (green Y-axis)
            bgColor = vec3(0.0, 1.0, 0.0);
        } else {
            // Vertical line = pitch indicator (blue Z-axis)
            bgColor = vec3(0.0, 0.0, 1.0);
        }
    }

    // CORNER CIRCLE OUTLINES
    vec2 corners[4];
    corners[0] = vec2(-cornerOffset * aspectRatio, -cornerOffset);  // Bottom-left - PITCH
    corners[1] = vec2(cornerOffset * aspectRatio, -cornerOffset);   // Bottom-right - ROLL
    corners[2] = vec2(-cornerOffset * aspectRatio, cornerOffset);   // Top-left - REFERENCE
    corners[3] = vec2(cornerOffset * aspectRatio, cornerOffset);    // Top-right - CAMERA

    float circleRadius = 0.08;
    float circleThickness = 0.01;

    for (int i = 0; i < 4; i++) {
        float dist = distance(uv, corners[i]);
        float circle = step(abs(dist - circleRadius), circleThickness);

        if (circle > 0.5) {
            // Color based on roll amount
            float rollIntensity = clamp(rollAmount * 5.0, 0.0, 1.0);
            if (rollAmount < 0.05) {
                bgColor = vec3(0.0, 1.0, 0.0);  // Green - no roll, camera is level
            } else {
                // Yellow to red gradient as roll increases
                bgColor = mix(vec3(1.0, 1.0, 0.0), vec3(1.0, 0.0, 0.0), rollIntensity);
            }
        }
    }

    // 3D AXIS VISUALIZERS (Unity-style)
    float axisScale = 0.06;  // Scale to fit in circle
    float axisThickness = 0.005;

    // TOP-LEFT: REFERENCE axes (default orientation - north, level, no rotation)
    vec2 topLeftCenter = vec2(-cornerOffset * aspectRatio, cornerOffset);
    vec2 topLeftUV = uv - topLeftCenter;

    // Draw reference X-axis (Red) - pointing right
    if (abs(topLeftUV.y) < axisThickness && topLeftUV.x > 0.0 && topLeftUV.x < axisScale) {
        bgColor = vec3(1.0, 0.0, 0.0);
    }
    // Draw reference Y-axis (Green) - pointing up
    if (abs(topLeftUV.x) < axisThickness && topLeftUV.y > 0.0 && topLeftUV.y < axisScale) {
        bgColor = vec3(0.0, 1.0, 0.0);
    }
    // Draw reference Z-axis (Blue) - pointing toward viewer (out of screen)
    // Show as a small circle since it's pointing at us
    if (length(topLeftUV) < 0.01 && length(topLeftUV) > 0.006) {
        bgColor = vec3(0.0, 0.0, 1.0);
    }

    // TOP-RIGHT: ACTUAL camera orientation axes (CONSTRAINED TO CIRCLE)
    vec2 topRightCenter = vec2(cornerOffset * aspectRatio, cornerOffset);
    vec2 topRightUV = uv - topRightCenter;

    // Only draw if within circle
    if (length(topRightUV) < circleRadius) {
        // Calculate camera's right, up, and forward vectors
        vec3 cameraRight = normalize(cross(dir, worldUp));
        vec3 cameraUp = normalize(cross(cameraRight, dir));
        vec3 cameraForward = dir;

        // Project camera axes to 2D (orthographic projection)
        vec2 camX2D = vec2(dot(cameraRight, vec3(1, 0, 0)), dot(cameraRight, vec3(0, 1, 0))) * axisScale;
        vec2 camY2D = vec2(dot(cameraUp, vec3(1, 0, 0)), dot(cameraUp, vec3(0, 1, 0))) * axisScale;
        vec2 camZ2D = vec2(dot(cameraForward, vec3(1, 0, 0)), dot(cameraForward, vec3(0, 1, 0))) * axisScale;

        // Draw camera X-axis (Red)
        float distToXAxis =
        length(camX2D) > 0.01 ? abs(topRightUV.y * camX2D.x - topRightUV.x * camX2D.y) / length(camX2D) : 999.0;
        if (distToXAxis < axisThickness && dot(topRightUV, camX2D) > 0.0 && dot(topRightUV, camX2D) < length(camX2D)) {
            bgColor = vec3(1.0, 0.0, 0.0);
        }

        // Draw camera Y-axis (Green)
        float distToYAxis =
        length(camY2D) > 0.01 ? abs(topRightUV.y * camY2D.x - topRightUV.x * camY2D.y) / length(camY2D) : 999.0;
        if (distToYAxis < axisThickness && dot(topRightUV, camY2D) > 0.0 && dot(topRightUV, camY2D) < length(camY2D)) {
            bgColor = vec3(0.0, 1.0, 0.0);
        }

        // Draw camera Z-axis (Blue)
        float distToZAxis =
        length(camZ2D) > 0.01 ? abs(topRightUV.y * camZ2D.x - topRightUV.x * camZ2D.y) / length(camZ2D) : 999.0;
        if (distToZAxis < axisThickness && dot(topRightUV, camZ2D) > 0.0 && dot(topRightUV, camZ2D) < length(camZ2D)) {
            bgColor = vec3(0.0, 0.0, 1.0);
        }
    }

    // BOTTOM-LEFT: PITCH indicator (X-axis rotation - red)
    vec2 bottomLeftCenter = vec2(-cornerOffset * aspectRatio, -cornerOffset);
    vec2 bottomLeftUV = uv - bottomLeftCenter;

    if (length(bottomLeftUV) < circleRadius) {
        // Pitch is the vertical angle
        float pitch = asin(clamp(dir.y, -1.0, 1.0));
        float pitchLineLength = circleRadius * 0.7;

        // Draw horizontal line rotated by pitch angle
        float cosPitch = cos(pitch);
        float sinPitch = sin(pitch);
        vec2 pitchLineEnd = vec2(cosPitch, sinPitch) * pitchLineLength;

        // Distance from point to line
        float distToPitchLine =
        abs(bottomLeftUV.y * pitchLineEnd.x - bottomLeftUV.x * pitchLineEnd.y) / length(pitchLineEnd);
        if (distToPitchLine < axisThickness && dot(bottomLeftUV, pitchLineEnd) > 0.0 &&
            dot(bottomLeftUV, pitchLineEnd) < length(pitchLineEnd)) {
            bgColor = vec3(1.0, 0.0, 0.0);  // Red for X-axis (pitch)
        }
    }

    // BOTTOM-RIGHT: ROLL indicator (Y-axis rotation - green)
    vec2 bottomRightCenter = vec2(cornerOffset * aspectRatio, -cornerOffset);
    vec2 bottomRightUV = uv - bottomRightCenter;

    if (length(bottomRightUV) < circleRadius) {
        // Roll is already calculated
        float rollLineLength = circleRadius * 0.7;

        // Draw line rotated by roll angle
        float cosRoll = cos(rollAngle);
        float sinRoll = sin(rollAngle);
        vec2 rollLineEnd = vec2(cosRoll, sinRoll) * rollLineLength;

        // Distance from point to line
        float distToRollLine =
        abs(bottomRightUV.y * rollLineEnd.x - bottomRightUV.x * rollLineEnd.y) / length(rollLineEnd);
        if (distToRollLine < axisThickness && dot(bottomRightUV, rollLineEnd) > 0.0 &&
            dot(bottomRightUV, rollLineEnd) < length(rollLineEnd)) {
            bgColor = vec3(0.0, 1.0, 0.0);  // Green for Y-axis (roll)
        }
    }

    // HORIZONTAL BAR (Top) - Shows yaw/horizontal rotation
    float barLength = 0.5;  // Shorter bar, more centered
    float barThickness = 0.03;

    // Use v_screenPos for vertical position, uv for horizontal (aspect-corrected)
    float topBarMask =
    step(0.85, v_screenPos.y) * step(v_screenPos.y, 0.85 + barThickness) * step(abs(uv.x), barLength);

    if (topBarMask > 0.5) {
        // Map screen X position to angle around Y axis
        float angle = atan(dir.z, dir.x);
        float normalizedAngle = (angle / 3.14159) * 0.5 + 0.5;  // 0 to 1

        // Sample the background color at different horizontal angles - match background better
        float sampleAngle = (uv.x / barLength) * 3.14159;
        vec3 sampleDir = normalize(vec3(sin(sampleAngle), 0.0, cos(sampleAngle)));

        // Use same logic as background for consistent colors
        vec3 barColor = vec3(0.3);  // Slightly brighter base
        barColor.r += max(0.0, sampleDir.x) * 0.7;
        barColor.gb += max(0.0, -sampleDir.x) * 0.7;

        bgColor = barColor;

        // Add current position indicator (black square)
        float indicatorPos = (normalizedAngle * 2.0 - 1.0) * barLength;
        float indicatorSize = 0.02;
        if (abs(uv.x - indicatorPos) < indicatorSize) {
            bgColor = vec3(0.0);
        }
    }

    // VERTICAL BAR (Left) - Shows pitch/vertical rotation
    float leftBarX = -0.85 * aspectRatio;
    float leftBarMask = step(uv.x, leftBarX + barThickness * 0.5) * step(leftBarX - barThickness * 0.5, uv.x) *
    step(abs(uv.y), barLength);

    if (leftBarMask > 0.5) {
        // Map screen Y position to pitch
        float normalizedPitch = dir.y * 0.5 + 0.5;  // 0 to 1

        // Sample colors at different vertical angles - match background better
        float samplePitch = uv.y / barLength;
        vec3 sampleDir = normalize(vec3(0.0, samplePitch, sqrt(1.0 - samplePitch * samplePitch)));

        // Use same logic as background for consistent colors
        vec3 barColor = vec3(0.3);  // Slightly brighter base
        barColor.g += max(0.0, sampleDir.y) * 0.7;
        barColor.b += max(0.0, -sampleDir.y) * 0.7;

        bgColor = barColor;

        // Add current position indicator (black square)
        float indicatorPos = (normalizedPitch * 2.0 - 1.0) * barLength;
        float indicatorSize = 0.02;
        if (abs(uv.y - indicatorPos) < indicatorSize) {
            bgColor = vec3(0.0);
        }
    }

    fragColor = vec4(bgColor, 1.0);
}