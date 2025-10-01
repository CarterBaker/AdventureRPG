#version 150

// =========================
// pbr.glsl (with vertex color support)
// =========================

// -----------------------------
// Required varyings
// -----------------------------
in vec3 v_worldPos;
in vec3 v_normal;
in vec3 v_tangent;
in vec2 v_uv;
in vec4 v_color;  // <--- vertex color from VS (0..1, usually biome/mesh tint)

// -----------------------------
// Uniforms (same as before)
// -----------------------------
uniform sampler2DArray u_textureArray;
uniform vec3 u_cameraPos;
uniform vec3 u_lightDirection;
uniform vec3 u_lightColor;
uniform float u_lightIntensity;
uniform vec3 u_ambientColor;
uniform float u_ambientIntensity;
uniform float u_parallaxScale;
uniform float u_minParallaxLayers;
uniform float u_maxParallaxLayers;
uniform bool u_useParallax;
uniform bool u_srgbOutput;
uniform bool u_linearizeAlbedo;

// -----------------------------
// Output
// -----------------------------
out vec4 fragColor;

// -----------------------------
// Layer indices
// -----------------------------
const int LAYER_ALBEDO = 0;
const int LAYER_NORMAL = 1;
const int LAYER_METAL = 2;
const int LAYER_ROUGHNESS = 3;
const int LAYER_HEIGHT = 4;
const int LAYER_AO = 5;

// -----------------------------
// Constants & utilities
// -----------------------------
const float PI = 3.14159265358979323846;
const float EPS = 1e-5;

// Sample a specific layer of the 2D texture array
vec4 sampleLayer(int layerIndex, vec2 uv) {
    return texture(u_textureArray, vec3(uv, float(layerIndex)));
}

// Optional sRGB -> linear conversion (approx)
vec3 srgbToLinear(vec3 c) {
    // fast gamma approx; if you want exact, use conditional branch
    return pow(c, vec3(2.2));
}
vec3 linearToSrgb(vec3 c) {
    return pow(c, vec3(1.0 / 2.2));
}

// -----------------------------
// Map fetchers (basic decoders)
// -----------------------------
vec4 fetchAlbedo(vec2 uv) {
    vec4 a = sampleLayer(LAYER_ALBEDO, uv);
    if (u_linearizeAlbedo) a.rgb = srgbToLinear(a.rgb);
    return a;
}

vec3 fetchNormalTS(vec2 uv) {
    // returns tangent-space normal in [-1,1]
    vec3 n = sampleLayer(LAYER_NORMAL, uv).xyz * 2.0 - 1.0;
    return normalize(n);
}

float fetchMetallic(vec2 uv) {
    return clamp(sampleLayer(LAYER_METAL, uv).r, 0.0, 1.0);
}

float fetchRoughness(vec2 uv) {
    return clamp(sampleLayer(LAYER_ROUGHNESS, uv).r, 0.02, 1.0);  // avoid zero roughness
}

float fetchHeight(vec2 uv) {
    return sampleLayer(LAYER_HEIGHT, uv).r;
}

float fetchAO(vec2 uv) {
    return clamp(sampleLayer(LAYER_AO, uv).r, 0.0, 1.0);
}

// -----------------------------
// TBN construction and normal transform
// - v_normal and v_tangent are expected in world space.
// - If tangent has no handedness, bitangent is computed via cross(N, T).
// -----------------------------
mat3 buildTBN(vec3 N, vec3 T) {
    vec3 n = normalize(N);
    vec3 t = normalize(T - n * dot(n, T));  // Gram-Schmidt
    vec3 b = cross(n, t);
    return mat3(t, b, n);  // columns are tangent, bitangent, normal
}

// Convert sampled tangent-space normal to world space
vec3 normalFromMap(vec3 normalTS, vec3 N, vec3 T) {
    mat3 TBN = buildTBN(N, T);
    // normalTS is in tangent space as (x right, y up, z forward)
    return normalize(TBN * normalTS);
}

// -----------------------------
// Simple parallax offset (cheap)
// - Uses tangent-space view vector and height map to offset UV
// - Accurate POM would be heavier; this is robust and cheap
// -----------------------------
vec2 parallaxOffsetSimple(vec2 uv, vec3 viewDirTS, float heightValue) {
    // avoid division by near-zero
    float vz = max(abs(viewDirTS.z), 0.001);
    vec2 offset = (viewDirTS.xy / vz) * (heightValue * u_parallaxScale);
    return uv - offset;  // subtract because height pushes surface away from viewer
}

// -----------------------------
// Cook-Torrance microfacet helpers
// -----------------------------
float DistributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;

    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom + EPS;
    return a2 / denom;
}

float GeometrySchlickGGX(float NdotV, float k) {
    return NdotV / (NdotV * (1.0 - k) + k + EPS);
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float k) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx1 = GeometrySchlickGGX(NdotV, k);
    float ggx2 = GeometrySchlickGGX(NdotL, k);
    return ggx1 * ggx2;
}

vec3 FresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

// -----------------------------
// Main PBR evaluation (single directional light + ambient)
// -----------------------------
vec3 evaluateBRDF(vec3 N, vec3 V, vec3 L, vec3 albedo, float metallic, float roughness, float ao, vec3 lightColor,
    float lightIntensity) {
    vec3 H = normalize(V + L);

    float NdotL = max(dot(N, L), 0.0);
    float NdotV = max(dot(N, V), 0.0);
    float NdotH = max(dot(N, H), 0.0);
    float VdotH = max(dot(V, H), 0.0);

    // F0
    vec3 F0 = mix(vec3(0.04), albedo, metallic);

    // D, G, F
    float D = DistributionGGX(N, H, roughness);
    float k = (roughness + 1.0) * (roughness + 1.0) / 8.0;  // Schlick-GGX geometric factor
    float G = GeometrySmith(N, V, L, k);
    vec3 F = FresnelSchlick(VdotH, F0);

    vec3 numerator = D * G * F;
    float denom = 4.0 * max(NdotV * NdotL, EPS) + EPS;
    vec3 specular = numerator / denom;

    // diffuse (Lambert) multiplied by (1 - metallic)
    vec3 kd = (1.0 - F) * (1.0 - metallic);  // energy-conserving
    vec3 diffuse = (kd * albedo) / PI;

    // Combine
    vec3 radiance = lightColor * lightIntensity;
    vec3 Lo = (diffuse + specular) * radiance * NdotL;

    // Add a simple ambient term (IBL would be better)
    vec3 ambient = (albedo * 0.03) * u_ambientColor * u_ambientIntensity * ao;

    return ambient + Lo;
}

// -----------------------------
// Main entry point
// -----------------------------
void runPBR() {
    // Base fetches
    vec4 albedoSample = fetchAlbedo(v_uv);
    vec3 baseAlbedo = albedoSample.rgb;  // texture color
    float alpha = albedoSample.a;        // texture alpha

    // Natural biome tint
    // Use the vertex color as the blending weight for each channel
    // This keeps most of the texture albedo while subtly tinting
    baseAlbedo = mix(baseAlbedo, baseAlbedo * v_color.rgb, v_color.a);
    alpha = mix(alpha, alpha * v_color.a, v_color.a);

    // Height-based parallax
    vec3 N_geom = normalize(v_normal);
    vec3 T_world = normalize(v_tangent);
    mat3 TBN_geom = buildTBN(N_geom, T_world);

    vec3 viewDirWorld = normalize(u_cameraPos - v_worldPos);
    vec3 viewDirTS = transpose(TBN_geom) * viewDirWorld;

    vec2 uv_par = v_uv;
    if (u_useParallax) {
        float h = fetchHeight(v_uv);
        uv_par = parallaxOffsetSimple(v_uv, viewDirTS, h);
    }

    // Sample maps with parallaxed UV
    vec3 normalTS = fetchNormalTS(uv_par);
    vec3 N = normalFromMap(normalTS, N_geom, T_world);

    float metallic = fetchMetallic(uv_par);
    float roughness = fetchRoughness(uv_par);
    float ao = fetchAO(uv_par);

    // camera & light
    vec3 V = normalize(u_cameraPos - v_worldPos);
    vec3 L = normalize(u_lightDirection);
    vec3 lightCol = u_lightColor;

    // Evaluate BRDF
    vec3 color = evaluateBRDF(N, V, L, baseAlbedo, metallic, roughness, ao, lightCol, u_lightIntensity);

    // tone mapping + gamma
    vec3 tone = color / (color + vec3(1.0));
    vec3 finalColor = u_srgbOutput ? linearToSrgb(tone) : tone;

    fragColor = vec4(finalColor, alpha);
}
