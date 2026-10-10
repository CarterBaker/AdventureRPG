#ifndef PATCH_CULLING_GLSL
#define PATCH_CULLING_GLSL

#include "includes/CameraData.glsl"

// Per-patch view culling for the tessellation control stage. A patch whose bounding sphere lies wholly outside the
// near plane or any side plane of the view frustum is dropped before a single vertex is evaluated, so the detail ring
// behind and beside the camera costs nothing in evaluation. The sphere spans the patch's four corners and is widened
// by PATCH_CULL_MARGIN_BLOCKS, more than the bevel, natural distortion, height relief and edge warp can ever move a
// vertex together, so a displaced patch is never dropped while any of it can reach the screen. A dropped patch is
// off screen, so the patches beside it can never show a crack along the edge it no longer draws. The far plane is
// left to the chunk culling, which already scopes the grid to the render distance.

const float PATCH_CULL_MARGIN_BLOCKS = 1.5;

vec4 getViewProjectionRow(int row) {
    return vec4(u_viewProjection[0][row], u_viewProjection[1][row], u_viewProjection[2][row], u_viewProjection[3][row]);
}

bool isOutsidePlane(vec4 plane, vec3 center, float radius) {
    return dot(plane.xyz, center) + plane.w < -radius * length(plane.xyz);
}

bool isPatchOutsideView(vec3 corner0, vec3 corner1, vec3 corner2, vec3 corner3) {
    vec3 center = (corner0 + corner1 + corner2 + corner3) * 0.25;

    float radiusSq = max(
        max(dot(corner0 - center, corner0 - center), dot(corner1 - center, corner1 - center)),
        max(dot(corner2 - center, corner2 - center), dot(corner3 - center, corner3 - center)));
    float radius = sqrt(radiusSq) + PATCH_CULL_MARGIN_BLOCKS;

    vec4 rowX = getViewProjectionRow(0);
    vec4 rowY = getViewProjectionRow(1);
    vec4 rowZ = getViewProjectionRow(2);
    vec4 rowW = getViewProjectionRow(3);

    return isOutsidePlane(rowW + rowX, center, radius)
        || isOutsidePlane(rowW - rowX, center, radius)
        || isOutsidePlane(rowW + rowY, center, radius)
        || isOutsidePlane(rowW - rowY, center, radius)
        || isOutsidePlane(rowW + rowZ, center, radius);
}

#endif
