package editor.bootstrap.imagepipeline.util;

public enum ImageMap {

    /*
     * Every map an image may carry beside its own layers, as a texturing
     * tool lists a material's channels: the albedo, which is the image's
     * layers themselves and is never generated, then the tangent-space
     * normal in both conventions (OpenGL green up, DirectX green down),
     * height, displacement about a mid level, fine bump, ambient occlusion,
     * cavity, curvature, thickness, roughness and its inverse glossiness,
     * metallic, the specular colour and diffuse of a specular workflow,
     * emissive, opacity and the tint mask a character's skin takes. Each
     * names how it is worked out of the image and the settings it takes,
     * its own copy of each, so altering a map's settings alters only it,
     * the suffix its exported PNG takes, and the neutral value it holds
     * where nothing marks it.
     */

    ALBEDO("Albedo", "a", 0x00000000, null),
    NORMAL(
            "Normal (OpenGL)", "n", 0x8080FFFF, MapGenerateUtility::generateNormal,
            MapGenerateOption.HEIGHT_DETAIL, MapGenerateOption.HEIGHT_SOFTNESS, MapGenerateOption.HEIGHT_CONTRAST,
            MapGenerateOption.HEIGHT_INVERT, MapGenerateOption.NORMAL_STRENGTH),
    NORMAL_DIRECTX(
            "Normal (DirectX)", "ndx", 0x8080FFFF, MapGenerateUtility::generateNormalDirectX,
            MapGenerateOption.HEIGHT_DETAIL, MapGenerateOption.HEIGHT_SOFTNESS, MapGenerateOption.HEIGHT_CONTRAST,
            MapGenerateOption.HEIGHT_INVERT, MapGenerateOption.NORMAL_STRENGTH),
    HEIGHT(
            "Height", "h", 0x808080FF, MapGenerateUtility::generateHeight,
            MapGenerateOption.HEIGHT_DETAIL, MapGenerateOption.HEIGHT_SOFTNESS, MapGenerateOption.HEIGHT_CONTRAST,
            MapGenerateOption.HEIGHT_INVERT),
    DISPLACEMENT(
            "Displacement", "disp", 0x808080FF, MapGenerateUtility::generateDisplacement,
            MapGenerateOption.HEIGHT_DETAIL, MapGenerateOption.HEIGHT_SOFTNESS, MapGenerateOption.HEIGHT_CONTRAST,
            MapGenerateOption.HEIGHT_INVERT, MapGenerateOption.MID_LEVEL),
    BUMP(
            "Bump", "bmp", 0x808080FF, MapGenerateUtility::generateBump,
            MapGenerateOption.BUMP_DETAIL, MapGenerateOption.HEIGHT_CONTRAST, MapGenerateOption.HEIGHT_INVERT),
    AMBIENT_OCCLUSION(
            "Ambient Occlusion", "ao", 0xFFFFFFFF, MapGenerateUtility::generateOcclusion,
            MapGenerateOption.HEIGHT_DETAIL, MapGenerateOption.HEIGHT_SOFTNESS, MapGenerateOption.HEIGHT_CONTRAST,
            MapGenerateOption.HEIGHT_INVERT, MapGenerateOption.OCCLUSION_STRENGTH,
            MapGenerateOption.OCCLUSION_RADIUS),
    CAVITY(
            "Cavity", "cav", 0xFFFFFFFF, MapGenerateUtility::generateCavity,
            MapGenerateOption.HEIGHT_DETAIL, MapGenerateOption.HEIGHT_CONTRAST, MapGenerateOption.HEIGHT_INVERT,
            MapGenerateOption.OCCLUSION_STRENGTH, MapGenerateOption.CAVITY_RADIUS),
    CURVATURE(
            "Curvature", "curv", 0x808080FF, MapGenerateUtility::generateCurvature,
            MapGenerateOption.HEIGHT_DETAIL, MapGenerateOption.HEIGHT_SOFTNESS, MapGenerateOption.HEIGHT_CONTRAST,
            MapGenerateOption.HEIGHT_INVERT, MapGenerateOption.CURVATURE_STRENGTH),
    THICKNESS(
            "Thickness", "thick", 0x000000FF, MapGenerateUtility::generateThickness,
            MapGenerateOption.HEIGHT_DETAIL, MapGenerateOption.HEIGHT_CONTRAST, MapGenerateOption.HEIGHT_INVERT,
            MapGenerateOption.THICKNESS_RADIUS),
    ROUGHNESS(
            "Roughness", "rough", 0x808080FF, MapGenerateUtility::generateRoughness,
            MapGenerateOption.ROUGHNESS_BASE, MapGenerateOption.ROUGHNESS_VARIATION, MapGenerateOption.HEIGHT_INVERT),
    GLOSSINESS(
            "Glossiness", "gloss", 0x808080FF, MapGenerateUtility::generateGlossiness,
            MapGenerateOption.ROUGHNESS_BASE, MapGenerateOption.ROUGHNESS_VARIATION, MapGenerateOption.HEIGHT_INVERT),
    METALLIC("Metallic", "m", 0x000000FF, MapGenerateUtility::generateMetallic, MapGenerateOption.METALLIC),
    SPECULAR(
            "Specular", "s", 0x0A0A0AFF, MapGenerateUtility::generateSpecular,
            MapGenerateOption.SPECULAR_LEVEL, MapGenerateOption.METALLIC),
    DIFFUSE("Diffuse", "dif", 0x808080FF, MapGenerateUtility::generateDiffuse, MapGenerateOption.METALLIC),
    EMISSIVE(
            "Emissive", "e", 0x000000FF, MapGenerateUtility::generateEmissive,
            MapGenerateOption.GLOW_THRESHOLD, MapGenerateOption.GLOW_SOFTNESS, MapGenerateOption.GLOW_STRENGTH),
    OPACITY(
            "Opacity", "opacity", 0xFFFFFFFF, MapGenerateUtility::generateOpacity,
            MapGenerateOption.OPACITY_FROM_BRIGHTNESS, MapGenerateOption.OPACITY_CUTOFF, MapGenerateOption.HEIGHT_INVERT),
    TINT("Tint", "tint", 0x000000FF, MapGenerateUtility::generateTint, MapGenerateOption.TINT_AMOUNT);

    // Values
    public static final ImageMap[] VALUES = values();

    // Internal
    private final String label;
    private final String exportSuffix;
    private final int neutralColor;
    private final MapGenerator generator;
    private final MapGenerateOption[] options;

    // Constructor \\

    ImageMap(
            String label,
            String exportSuffix,
            int neutralColor,
            MapGenerator generator,
            MapGenerateOption... options) {
        this.label = label;
        this.exportSuffix = exportSuffix;
        this.neutralColor = neutralColor;
        this.generator = generator;
        this.options = options;
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    // The suffix an exported map's PNG takes after the image's name, the alias the engine's textures know it by where
    // it takes one (a, n, h, ao, rough, m, s, e, tint) and a name no alias holds otherwise, so Import never mistakes it
    public String getExportSuffix() {
        return exportSuffix;
    }

    // What the map holds where nothing marks it, RGBA8888, the value Erase lays on it
    public int getNeutralColor() {
        return neutralColor;
    }

    // How the map is worked out of the image; null for the albedo, the image's layers themselves
    public MapGenerator getGenerator() {
        return generator;
    }

    public boolean isGenerated() {
        return generator != null;
    }

    public MapGenerateOption[] getOptions() {
        return options;
    }
}
