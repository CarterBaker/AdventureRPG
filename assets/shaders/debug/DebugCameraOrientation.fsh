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

    float phi = atan(dir.z, dir.x);
    float theta = acos(clamp(dir.y, -1.0, 1.0));
    float checkerSize = 0.3;
    float checker1 = mod(floor(phi / checkerSize) + floor(theta / checkerSize), 2.0);

    vec3 color1 = vec3(0.2);
    vec3 color2 = vec3(0.4);
    color1.r += max(0.0, dir.x) * 0.3;
    color1.gb += max(0.0, -dir.x) * 0.3;
    color1.g += max(0.0, dir.y) * 0.3;
    color1.b += max(0.0, -dir.y) * 0.3;
    color2.r += max(0.0, dir.x) * 0.5;
    color2.gb += max(0.0, -dir.x) * 0.5;
    color2.g += max(0.0, dir.y) * 0.5;
    color2.b += max(0.0, -dir.y) * 0.5;

    vec3 bgColor = mix(color1, color2, checker1);

    float lineWidth = 0.005;

    // X-axis (red) - full circle in YZ plane where X=0
    vec3 xAxis = vec3(1.0, 0.0, 0.0);
    float xDist = abs(dot(dir, xAxis));
    float xZeroLine = step(xDist, lineWidth);
    if (xZeroLine > 0.5) {
        bgColor = mix(bgColor, vec3(1.0, 0.0, 0.0), 0.8);
    }

    // Y-axis (green) - full circle in XZ plane where Y=0
    vec3 yAxis = vec3(0.0, 1.0, 0.0);
    float yDist = abs(dot(dir, yAxis));
    float yZeroLine = step(yDist, lineWidth);
    if (yZeroLine > 0.5) {
        bgColor = mix(bgColor, vec3(0.0, 1.0, 0.0), 0.8);
    }

    // Z-axis (blue) - full circle in XY plane where Z=0
    vec3 zAxis = vec3(0.0, 0.0, 1.0);
    float zDist = abs(dot(dir, zAxis));
    float zZeroLine = step(zDist, lineWidth);
    if (zZeroLine > 0.5) {
        bgColor = mix(bgColor, vec3(0.0, 0.0, 1.0), 0.8);
    }

    vec3 worldUp = vec3(0.0, 1.0, 0.0);
    vec3 cameraUpFromMatrix = normalize(vec3(u_view[0][1], u_view[1][1], u_view[2][1]));
    vec3 cameraRightFromMatrix = normalize(vec3(u_view[0][0], u_view[1][0], u_view[2][0]));

    vec3 rightDir = normalize(cross(dir, worldUp));
    vec3 upDir = normalize(cross(rightDir, dir));
    float upAlignment = dot(upDir, worldUp);
    float rollAmount = 1.0 - abs(upAlignment);

    vec3 viewDir = normalize(dir);
    vec3 projectedCameraUp = normalize(cameraUpFromMatrix - dot(cameraUpFromMatrix, viewDir) * viewDir);
    vec3 projectedWorldUp = normalize(worldUp - dot(worldUp, viewDir) * viewDir);
    float rollAngle =
    atan(dot(cross(projectedWorldUp, projectedCameraUp), viewDir), dot(projectedWorldUp, projectedCameraUp));

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
            bgColor = vec3(0.0, 1.0, 0.0);
        } else {
            bgColor = vec3(0.0, 0.0, 1.0);
        }
    }

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
            float rollIntensity = clamp(rollAmount * 5.0, 0.0, 1.0);
            if (rollAmount < 0.05) {
                bgColor = vec3(0.0, 1.0, 0.0);
            } else {
                bgColor = mix(vec3(1.0, 1.0, 0.0), vec3(1.0, 0.0, 0.0), rollIntensity);
            }
        }
    }

    float axisScale = 0.06;
    float axisThickness = 0.005;

    vec2 topLeftCenter = vec2(-cornerOffset * aspectRatio, cornerOffset);
    vec2 topLeftUV = uv - topLeftCenter;

    if (abs(topLeftUV.y) < axisThickness && topLeftUV.x > 0.0 && topLeftUV.x < axisScale) {
        bgColor = vec3(1.0, 0.0, 0.0);
    }
    if (abs(topLeftUV.x) < axisThickness && topLeftUV.y > 0.0 && topLeftUV.y < axisScale) {
        bgColor = vec3(0.0, 1.0, 0.0);
    }
    if (length(topLeftUV) < 0.01 && length(topLeftUV) > 0.006) {
        bgColor = vec3(0.0, 0.0, 1.0);
    }

    vec2 topRightCenter = vec2(cornerOffset * aspectRatio, cornerOffset);
    vec2 topRightUV = uv - topRightCenter;

    if (length(topRightUV) < circleRadius) {
        vec3 cameraRight = normalize(cross(dir, worldUp));
        vec3 cameraUp = normalize(cross(cameraRight, dir));
        vec3 cameraForward = dir;

        vec2 camX2D = vec2(dot(cameraRight, vec3(1, 0, 0)), dot(cameraRight, vec3(0, 1, 0))) * axisScale;
        vec2 camY2D = vec2(dot(cameraUp, vec3(1, 0, 0)), dot(cameraUp, vec3(0, 1, 0))) * axisScale;
        vec2 camZ2D = vec2(dot(cameraForward, vec3(1, 0, 0)), dot(cameraForward, vec3(0, 1, 0))) * axisScale;

        float distToXAxis =
        length(camX2D) > 0.01 ? abs(topRightUV.y * camX2D.x - topRightUV.x * camX2D.y) / length(camX2D) : 999.0;
        if (distToXAxis < axisThickness && dot(topRightUV, camX2D) > 0.0 && dot(topRightUV, camX2D) < length(camX2D)) {
            bgColor = vec3(1.0, 0.0, 0.0);
        }

        float distToYAxis =
        length(camY2D) > 0.01 ? abs(topRightUV.y * camY2D.x - topRightUV.x * camY2D.y) / length(camY2D) : 999.0;
        if (distToYAxis < axisThickness && dot(topRightUV, camY2D) > 0.0 && dot(topRightUV, camY2D) < length(camY2D)) {
            bgColor = vec3(0.0, 1.0, 0.0);
        }

        float distToZAxis =
        length(camZ2D) > 0.01 ? abs(topRightUV.y * camZ2D.x - topRightUV.x * camZ2D.y) / length(camZ2D) : 999.0;
        if (distToZAxis < axisThickness && dot(topRightUV, camZ2D) > 0.0 && dot(topRightUV, camZ2D) < length(camZ2D)) {
            bgColor = vec3(0.0, 0.0, 1.0);
        }
    }

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

    vec2 bottomRightCenter = vec2(cornerOffset * aspectRatio, -cornerOffset);
    vec2 bottomRightUV = uv - bottomRightCenter;

    // Calculate compass heading for the heading indicator
    vec3 cameraForward = -normalize(vec3(u_view[0][2], u_view[1][2], u_view[2][2]));
    vec3 projectedForward = normalize(vec3(cameraForward.x, 0.0, cameraForward.z));
    float compassHeading = atan(projectedForward.x, projectedForward.z);

    // Heading indicator line - ROTATES to show where you're looking
    if (length(bottomRightUV) < circleRadius) {
        float headingLineLength = circleRadius * 0.7;

        // Use the compass heading to rotate the line
        float headingAngle = compassHeading;
        float cosHeading = cos(headingAngle);
        float sinHeading = sin(headingAngle);
        vec2 headingLineEnd = vec2(sinHeading, cosHeading) * headingLineLength;

        float distToHeadingLine =
        abs(bottomRightUV.y * headingLineEnd.x - bottomRightUV.x * headingLineEnd.y) / length(headingLineEnd);
        if (distToHeadingLine < axisThickness && dot(bottomRightUV, headingLineEnd) > 0.0 &&
            dot(bottomRightUV, headingLineEnd) < length(headingLineEnd)) {
            bgColor = vec3(1.0, 1.0, 0.0);
        }
    }

    float northAngle = -compassHeading;
    float cosNorth = cos(northAngle);
    float sinNorth = sin(northAngle);

    float markerThickness = 0.002;
    float innerMarkerRadius = circleRadius + 0.005;
    float outerMarkerRadius = circleRadius + 0.035;

    // Cardinal directions (N, E, S, W) - FIXED
    for (int i = 0; i < 4; i++) {
        float angle = float(i) * 1.5708;
        vec2 markerDir = vec2(cos(angle), sin(angle));

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

    // Intercardinal directions (NE, SE, SW, NW) - FIXED
    outerMarkerRadius = circleRadius + 0.025;
    for (int i = 0; i < 4; i++) {
        float angle = float(i) * 1.5708 + 0.7854;
        vec2 markerDir = vec2(cos(angle), sin(angle));

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

    // Small tick marks - FIXED
    outerMarkerRadius = circleRadius + 0.015;
    for (int i = 0; i < 24; i++) {
        float angle = float(i) * 0.2618;
        float mod45 = mod(angle, 0.7854);
        if (mod45 < 0.1 || mod45 > 0.68) continue;

        vec2 markerDir = vec2(cos(angle), sin(angle));

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

    // North arrow - FIXED pointing up at screen top
    vec2 normalizedNorth = vec2(0.0, 1.0); {
        float arrowStartDist = circleRadius + 0.005;
        vec2 arrowBase = bottomRightCenter + normalizedNorth * arrowStartDist;
        float arrowLength = 0.035;
        float arrowWidth = 0.015;

        float northDirAngle = atan(normalizedNorth.y, normalizedNorth.x);
        float cosArrow = cos(-northDirAngle);
        float sinArrow = sin(-northDirAngle);
        vec2 localUV = uv - arrowBase;
        vec2 rotatedArrowUV =
        vec2(localUV.x * cosArrow - localUV.y * sinArrow, localUV.x * sinArrow + localUV.y * cosArrow);

        float shaftLength = arrowLength * 0.6;
        float shaftDist = abs(rotatedArrowUV.y);
        float shaftProgress = rotatedArrowUV.x;
        float shaft = step(shaftDist, arrowWidth * 0.3) * step(0.0, shaftProgress) * step(shaftProgress, shaftLength);

        float headStart = shaftLength;
        float headEnd = arrowLength;
        float headProgress = clamp((shaftProgress - headStart) / (headEnd - headStart), 0.0, 1.0);
        float headWidth = arrowWidth * (1.0 - headProgress);
        float head =
        step(abs(rotatedArrowUV.y), headWidth) * step(headStart, shaftProgress) * step(shaftProgress, headEnd);

        float northArrowMask = max(shaft, head);
        if (northArrowMask > 0.5) {
            bgColor = vec3(1.0, 0.2, 0.2);
        }
    }

    float barLength = 0.5;
    float barThickness = 0.03;

    float topBarMask =
    step(0.85, v_screenPos.y) * step(v_screenPos.y, 0.85 + barThickness) * step(abs(uv.x), barLength);

    if (topBarMask > 0.5) {
        float angle = atan(dir.z, dir.x);
        float normalizedAngle = (angle / 3.14159) * 0.5 + 0.5;

        float sampleAngle = (uv.x / barLength) * 3.14159;
        vec3 sampleDir = normalize(vec3(sin(sampleAngle), 0.0, cos(sampleAngle)));

        vec3 barColor = vec3(0.3);
        barColor.r += max(0.0, sampleDir.x) * 0.7;
        barColor.gb += max(0.0, -sampleDir.x) * 0.7;

        bgColor = barColor;

        float indicatorPos = (normalizedAngle * 2.0 - 1.0) * barLength;
        float indicatorSize = 0.02;
        if (abs(uv.x - indicatorPos) < indicatorSize) {
            bgColor = vec3(0.0);
        }
    }

    float leftBarX = -0.85 * aspectRatio;
    float leftBarMask = step(uv.x, leftBarX + barThickness * 0.5) * step(leftBarX - barThickness * 0.5, uv.x) *
    step(abs(uv.y), barLength);

    if (leftBarMask > 0.5) {
        float normalizedPitch = dir.y * 0.5 + 0.5;

        float samplePitch = uv.y / barLength;
        vec3 sampleDir = normalize(vec3(0.0, samplePitch, sqrt(1.0 - samplePitch * samplePitch)));

        vec3 barColor = vec3(0.3);
        barColor.g += max(0.0, sampleDir.y) * 0.7;
        barColor.b += max(0.0, -sampleDir.y) * 0.7;

        bgColor = barColor;

        float indicatorPos = (normalizedPitch * 2.0 - 1.0) * barLength;
        float indicatorSize = 0.02;
        if (abs(uv.y - indicatorPos) < indicatorSize) {
            bgColor = vec3(0.0);
        }
    }

    fragColor = vec4(bgColor, 1.0);
}