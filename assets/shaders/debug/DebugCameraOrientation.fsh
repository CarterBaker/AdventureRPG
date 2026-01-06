#version 150
in vec3 v_dir;
in vec2 v_screenPos;
out vec4 fragColor;

#include "includes/CameraData.glsl"

void main() {
    vec3 dir = normalize(v_dir);

    vec2 uv = v_screenPos;
    float aspectRatio = u_viewport.x / u_viewport.y;
    uv.x *= aspectRatio;

    float cornerOffset = 0.85;

    // Background checkerboard using spherical coordinates
    float phi = atan(dir.z, dir.x);
    float theta = acos(clamp(dir.y, -1.0, 1.0));
    float checkerSize = 0.3;
    float checker1 = mod(floor(phi / checkerSize) + floor(theta / checkerSize), 2.0);

    // Background colors based on direction
    vec3 color1 = vec3(0.2);
    vec3 color2 = vec3(0.4);

    // Color based on world axes
    color1.r += max(0.0, dir.x) * 0.3;    // +X = red
    color1.gb += max(0.0, -dir.x) * 0.3;  // -X = cyan
    color1.g += max(0.0, dir.y) * 0.3;    // +Y = green
    color1.rb += max(0.0, -dir.y) * 0.3;  // -Y = magenta

    color2.r += max(0.0, dir.x) * 0.5;
    color2.gb += max(0.0, -dir.x) * 0.5;
    color2.g += max(0.0, dir.y) * 0.5;
    color2.rb += max(0.0, -dir.y) * 0.5;

    vec3 bgColor = mix(color1, color2, checker1);

    float lineWidth = 0.005;

    // FIXED: Great circles for each axis plane
    // XY plane (Z=0) - Blue circle
    float zDist = abs(dir.z);
    float zCircle = step(zDist, lineWidth);
    if (zCircle > 0.5) {
        bgColor = mix(bgColor, vec3(0.0, 0.0, 1.0), 0.8);
    }

    // XZ plane (Y=0) - Green circle
    float yDist = abs(dir.y);
    float yCircle = step(yDist, lineWidth);
    if (yCircle > 0.5) {
        bgColor = mix(bgColor, vec3(0.0, 1.0, 0.0), 0.8);
    }

    // YZ plane (X=0) - Red circle
    float xDist = abs(dir.x);
    float xCircle = step(xDist, lineWidth);
    if (xCircle > 0.5) {
        bgColor = mix(bgColor, vec3(1.0, 0.0, 0.0), 0.8);
    }

    // FIXED: Calculate roll using proper camera basis vectors
    vec3 worldUp = vec3(0.0, 1.0, 0.0);
    vec3 cameraUpFromMatrix = normalize(vec3(u_view[0][1], u_view[1][1], u_view[2][1]));
    vec3 cameraRightFromMatrix = normalize(vec3(u_view[0][0], u_view[1][0], u_view[2][0]));

    // Calculate expected up direction based on view direction
    vec3 viewDir = normalize(dir);
    vec3 expectedRight = normalize(cross(worldUp, viewDir));
    vec3 expectedUp = normalize(cross(viewDir, expectedRight));

    // Calculate roll angle by comparing camera up to expected up
    vec3 projectedCameraUp = normalize(cameraUpFromMatrix - dot(cameraUpFromMatrix, viewDir) * viewDir);
    vec3 projectedExpectedUp = normalize(expectedUp - dot(expectedUp, viewDir) * viewDir);

    float rollAngle =
    atan(dot(cross(projectedExpectedUp, projectedCameraUp), viewDir), dot(projectedExpectedUp, projectedCameraUp));

    // Calculate roll amount for coloring
    float rollAmount = abs(rollAngle) / 3.14159;

    // FIXED: Crosshair with proper roll rotation
    float crosshairLength = 0.1;
    float crosshairThickness = 0.003;
    float cosRoll = cos(-rollAngle);
    float sinRoll = sin(-rollAngle);
    vec2 rotatedUV = vec2(uv.x * cosRoll - uv.y * sinRoll, uv.x * sinRoll + uv.y * cosRoll);

    float horizontalLine = step(abs(rotatedUV.y), crosshairThickness) * step(abs(rotatedUV.x), crosshairLength);
    float verticalLine = step(abs(rotatedUV.x), crosshairThickness) * step(abs(rotatedUV.y), crosshairLength);
    float crosshair = max(horizontalLine, verticalLine);

    if (crosshair > 0.5) {
        if (horizontalLine > 0.5) {
            bgColor = vec3(0.0, 1.0, 0.0);  // Horizontal = green (pitch)
        } else {
            bgColor = vec3(0.0, 0.0, 1.0);  // Vertical = blue (yaw)
        }
    }

    // Corner roll indicators
    vec2 corners[4];
    corners[0] = vec2(-cornerOffset * aspectRatio, -cornerOffset);
    corners[1] = vec2(cornerOffset * aspectRatio, -cornerOffset);
    corners[2] = vec2(-cornerOffset * aspectRatio, cornerOffset);
    corners[3] = vec2(cornerOffset * aspectRatio, cornerOffset);

    float circleRadius = 0.08;
    float circleThickness = 0.01;

    for (int i = 0; i < 4; i++) {
        float dist = distance(uv, corners[i]);
        float circle = step(abs(dist - circleRadius), circleThickness);

        if (circle > 0.5) {
            if (rollAmount < 0.05) {
                bgColor = vec3(0.0, 1.0, 0.0);  // Green when level
            } else {
                float rollIntensity = clamp(rollAmount * 2.0, 0.0, 1.0);
                bgColor = mix(vec3(1.0, 1.0, 0.0), vec3(1.0, 0.0, 0.0), rollIntensity);
            }
        }
    }

    float axisScale = 0.06;
    float axisThickness = 0.005;

    // Top-left: World axis reference (fixed)
    vec2 topLeftCenter = vec2(-cornerOffset * aspectRatio, cornerOffset);
    vec2 topLeftUV = uv - topLeftCenter;

    if (abs(topLeftUV.y) < axisThickness && topLeftUV.x > 0.0 && topLeftUV.x < axisScale) {
        bgColor = vec3(1.0, 0.0, 0.0);  // X axis
    }
    if (abs(topLeftUV.x) < axisThickness && topLeftUV.y > 0.0 && topLeftUV.y < axisScale) {
        bgColor = vec3(0.0, 1.0, 0.0);  // Y axis
    }
    if (length(topLeftUV) < 0.01 && length(topLeftUV) > 0.006) {
        bgColor = vec3(0.0, 0.0, 1.0);  // Z axis (center)
    }

    // FIXED: Top-right camera orientation gizmo
    vec2 topRightCenter = vec2(cornerOffset * aspectRatio, cornerOffset);
    vec2 topRightUV = uv - topRightCenter;

    if (length(topRightUV) < circleRadius) {
        // Extract camera basis vectors from view matrix
        vec3 cameraRight = normalize(vec3(u_view[0][0], u_view[1][0], u_view[2][0]));
        vec3 cameraUp = normalize(vec3(u_view[0][1], u_view[1][1], u_view[2][1]));
        vec3 cameraForward = -normalize(vec3(u_view[0][2], u_view[1][2], u_view[2][2]));

        // Project 3D axes onto 2D screen space
        vec2 camX2D = vec2(dot(cameraRight, vec3(1, 0, 0)), dot(cameraUp, vec3(1, 0, 0))) * axisScale;
        vec2 camY2D = vec2(dot(cameraRight, vec3(0, 1, 0)), dot(cameraUp, vec3(0, 1, 0))) * axisScale;
        vec2 camZ2D = vec2(dot(cameraRight, vec3(0, 0, 1)), dot(cameraUp, vec3(0, 0, 1))) * axisScale;

        // Draw X axis (red)
        float distToXAxis =
        length(camX2D) > 0.01 ? abs(topRightUV.y * camX2D.x - topRightUV.x * camX2D.y) / length(camX2D) : 999.0;
        if (distToXAxis < axisThickness && dot(topRightUV, camX2D) > 0.0 && dot(topRightUV, camX2D) < length(camX2D)) {
            bgColor = vec3(1.0, 0.0, 0.0);
        }

        // Draw Y axis (green)
        float distToYAxis =
        length(camY2D) > 0.01 ? abs(topRightUV.y * camY2D.x - topRightUV.x * camY2D.y) / length(camY2D) : 999.0;
        if (distToYAxis < axisThickness && dot(topRightUV, camY2D) > 0.0 && dot(topRightUV, camY2D) < length(camY2D)) {
            bgColor = vec3(0.0, 1.0, 0.0);
        }

        // Draw Z axis (blue)
        float distToZAxis =
        length(camZ2D) > 0.01 ? abs(topRightUV.y * camZ2D.x - topRightUV.x * camZ2D.y) / length(camZ2D) : 999.0;
        if (distToZAxis < axisThickness && dot(topRightUV, camZ2D) > 0.0 && dot(topRightUV, camZ2D) < length(camZ2D)) {
            bgColor = vec3(0.0, 0.0, 1.0);
        }
    }

    // Bottom-left: Pitch indicator
    vec2 bottomLeftCenter = vec2(-cornerOffset * aspectRatio, -cornerOffset);
    vec2 bottomLeftUV = uv - bottomLeftCenter;

    if (length(bottomLeftUV) < circleRadius) {
        float pitch = asin(clamp(dir.y, -1.0, 1.0));
        float pitchLineLength = circleRadius * 0.7;

        float cosPitch = cos(pitch);
        float sinPitch = sin(pitch);
        vec2 pitchLineEnd = vec2(cosPitch, sinPitch) * pitchLineLength;

        float distToPitchLine =
        abs(bottomLeftUV.y * pitchLineEnd.x - bottomLeftUV.x * pitchLineEnd.y) / length(pitchLineEnd);
        if (distToPitchLine < axisThickness && dot(bottomLeftUV, pitchLineEnd) > 0.0 &&
            dot(bottomLeftUV, pitchLineEnd) < length(pitchLineEnd)) {
            bgColor = vec3(1.0, 0.0, 0.0);
        }
    }

    // Pitch up indicator triangle
    float upOffset = 0.02;
    float triHeight = 0.025;
    float triHalfWidth = 0.015;
    vec2 triBaseCenter = vec2(0.0, 1.0) * (circleRadius + upOffset);
    vec2 triUV = bottomLeftUV - triBaseCenter;
    float insideTriangle =
    step(0.0, triUV.y) * step(triUV.y, triHeight) * step(abs(triUV.x), mix(triHalfWidth, 0.0, triUV.y / triHeight));
    if (insideTriangle > 0.5) {
        bgColor = vec3(0.0, 1.0, 0.5);
    }

    // Bottom-right: Compass heading indicator
    vec2 bottomRightCenter = vec2(cornerOffset * aspectRatio, -cornerOffset);
    vec2 bottomRightUV = uv - bottomRightCenter;

    vec3 cameraForward = -normalize(vec3(u_view[0][2], u_view[1][2], u_view[2][2]));
    vec3 projectedForward = normalize(vec3(cameraForward.x, 0.0, cameraForward.z));
    float compassHeading = atan(projectedForward.x, projectedForward.z);

    // Heading indicator line
    if (length(bottomRightUV) < circleRadius) {
        float headingLineLength = circleRadius * 0.7;
        float cosHeading = cos(compassHeading);
        float sinHeading = sin(compassHeading);
        vec2 headingLineEnd = vec2(sinHeading, cosHeading) * headingLineLength;

        float distToHeadingLine =
        abs(bottomRightUV.y * headingLineEnd.x - bottomRightUV.x * headingLineEnd.y) / length(headingLineEnd);
        if (distToHeadingLine < axisThickness && dot(bottomRightUV, headingLineEnd) > 0.0 &&
            dot(bottomRightUV, headingLineEnd) < length(headingLineEnd)) {
            bgColor = vec3(1.0, 1.0, 0.0);
        }
    }

    float markerThickness = 0.002;
    float innerMarkerRadius = circleRadius + 0.005;
    float outerMarkerRadius = circleRadius + 0.035;

    // Cardinal direction markers (fixed to world space)
    for (int i = 0; i < 4; i++) {
        float angle = float(i) * 1.5708;
        vec2 markerDir = vec2(sin(angle), cos(angle));
        vec2 markerStart = bottomRightCenter + markerDir * innerMarkerRadius;
        vec2 markerEnd = bottomRightCenter + markerDir * outerMarkerRadius;

        vec2 toStart = uv - markerStart;
        vec2 lineDir = markerEnd - markerStart;
        float lineLength = length(lineDir);
        lineDir = lineDir / lineLength;

        float projection = dot(toStart, lineDir);
        vec2 closest = markerStart + lineDir * clamp(projection, 0.0, lineLength);
        float distToLine = length(uv - closest);

        if (distToLine < markerThickness) {
            bgColor = vec3(0.8);
        }
    }

    // Intercardinal markers
    outerMarkerRadius = circleRadius + 0.025;
    for (int i = 0; i < 4; i++) {
        float angle = float(i) * 1.5708 + 0.7854;
        vec2 markerDir = vec2(sin(angle), cos(angle));
        vec2 markerStart = bottomRightCenter + markerDir * innerMarkerRadius;
        vec2 markerEnd = bottomRightCenter + markerDir * outerMarkerRadius;

        vec2 toStart = uv - markerStart;
        vec2 lineDir = markerEnd - markerStart;
        float lineLength = length(lineDir);
        lineDir = lineDir / lineLength;

        float projection = dot(toStart, lineDir);
        vec2 closest = markerStart + lineDir * clamp(projection, 0.0, lineLength);
        float distToLine = length(uv - closest);

        if (distToLine < markerThickness) {
            bgColor = vec3(0.6);
        }
    }

    // Small tick marks
    outerMarkerRadius = circleRadius + 0.015;
    for (int i = 0; i < 24; i++) {
        float angle = float(i) * 0.2618;
        float mod45 = mod(angle, 0.7854);
        if (mod45 < 0.1 || mod45 > 0.68) continue;

        vec2 markerDir = vec2(sin(angle), cos(angle));
        vec2 markerStart = bottomRightCenter + markerDir * innerMarkerRadius;
        vec2 markerEnd = bottomRightCenter + markerDir * outerMarkerRadius;

        vec2 toStart = uv - markerStart;
        vec2 lineDir = markerEnd - markerStart;
        float lineLength = length(lineDir);
        lineDir = lineDir / lineLength;

        float projection = dot(toStart, lineDir);
        vec2 closest = markerStart + lineDir * clamp(projection, 0.0, lineLength);
        float distToLine = length(uv - closest);

        if (distToLine < markerThickness * 0.8) {
            bgColor = vec3(0.4);
        }
    }

    // North arrow (fixed pointing up)
    vec2 normalizedNorth = vec2(0.0, 1.0);
    float arrowStartDist = circleRadius + 0.005;
    vec2 arrowBase = bottomRightCenter + normalizedNorth * arrowStartDist;
    float arrowLength = 0.035;
    float arrowWidth = 0.015;

    vec2 localUV = uv - arrowBase;
    vec2 rotatedArrowUV = vec2(localUV.x, -localUV.y);

    float shaftLength = arrowLength * 0.6;
    float shaft = step(abs(rotatedArrowUV.x), arrowWidth * 0.3) * step(0.0, rotatedArrowUV.y) *
    step(rotatedArrowUV.y, shaftLength);

    float headStart = shaftLength;
    float headEnd = arrowLength;
    float headProgress = clamp((rotatedArrowUV.y - headStart) / (headEnd - headStart), 0.0, 1.0);
    float headWidth = arrowWidth * (1.0 - headProgress);
    float head =
    step(abs(rotatedArrowUV.x), headWidth) * step(headStart, rotatedArrowUV.y) * step(rotatedArrowUV.y, headEnd);

    if (max(shaft, head) > 0.5) {
        bgColor = vec3(1.0, 0.2, 0.2);
    }

    // Top heading bar - indicator moves
    float barLength = 0.5;
    float barThickness = 0.03;
    float topBarMask =
    step(0.85, v_screenPos.y) * step(v_screenPos.y, 0.85 + barThickness) * step(abs(uv.x), barLength);

    if (topBarMask > 0.5) {
        // Sample angle based on UV position (bar is fixed)
        float sampleAngle = (uv.x / barLength) * 3.14159;
        vec3 sampleDir = vec3(sin(sampleAngle), 0.0, cos(sampleAngle));

        vec3 barColor = vec3(0.3);
        barColor.r += max(0.0, sampleDir.x) * 0.7;
        barColor.b += max(0.0, -sampleDir.x) * 0.7;

        bgColor = barColor;

        // Current heading - indicator moves
        float currentHeading = atan(projectedForward.x, projectedForward.z);
        float normalizedHeading = (currentHeading / 3.14159) * barLength;

        if (abs(uv.x - normalizedHeading) < 0.02) {
            bgColor = vec3(0.0);
        }
    }

    // Left pitch bar - indicator moves
    float leftBarX = -0.85 * aspectRatio;
    float leftBarMask = step(uv.x, leftBarX + barThickness * 0.5) * step(leftBarX - barThickness * 0.5, uv.x) *
    step(abs(uv.y), barLength);

    if (leftBarMask > 0.5) {
        // Sample pitch based on UV position (bar is fixed)
        float samplePitch = (uv.y / barLength) * 1.5708;  // ~90 degrees

        vec3 barColor = vec3(0.3);
        barColor.g += max(0.0, sin(samplePitch)) * 0.7;
        barColor.b += max(0.0, -sin(samplePitch)) * 0.7;

        bgColor = barColor;

        // Current pitch - indicator moves
        float currentPitch = asin(clamp(dir.y, -1.0, 1.0));
        float normalizedPitch = (currentPitch / 1.5708) * barLength;

        if (abs(uv.y - normalizedPitch) < 0.02) {
            bgColor = vec3(0.0);
        }
    }

    fragColor = vec4(bgColor, 1.0);
}