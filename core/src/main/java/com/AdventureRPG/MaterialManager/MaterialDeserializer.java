package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.TextureManager.TextureManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Vector4;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class MaterialDeserializer {

    // --------- PUBLIC API ---------
    static Material parse(File file, TextureManager texMgr, Gson gson) {
        try {
            String json = Files.readString(file.toPath());
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            Material mat = new Material();

            // --- TEXTURES (whole atlas per type) ---
            if (root.has("maps")) {
                JsonObject maps = root.getAsJsonObject("maps");
                for (String key : maps.keySet()) {
                    String folder = maps.get(key).getAsString(); // you said this is the atlas folder name
                    String k = key.toLowerCase();

                    TextureAtlas atlas = texMgr.getAtlas(folder, k);
                    if (atlas == null || atlas.getTextures().size == 0)
                        continue;

                    Texture tex = atlas.getTextures().first();
                    TextureDescriptor<Texture> desc = new TextureDescriptor<>(tex);
                    // copy conservative fields that exist in all libGDX versions
                    // (no flipY here — that field does not exist on TextureDescriptor)
                    desc.minFilter = tex.getMinFilter();
                    desc.magFilter = tex.getMagFilter();
                    // wraps are not on Texture, so keep defaults on the descriptor (ClampToEdge)

                    switch (k) {
                        case "albedo":
                        case "diffuse":
                            mat.set(new TextureAttribute(TextureAttribute.Diffuse, desc));
                            break;
                        case "normal":
                            mat.set(new TextureAttribute(TextureAttribute.Normal, desc));
                            break;
                        case "specular":
                            mat.set(new TextureAttribute(TextureAttribute.Specular, desc));
                            break;
                        case "emissive":
                            mat.set(new TextureAttribute(TextureAttribute.Emissive, desc));
                            break;
                        case "ambient":
                            mat.set(new TextureAttribute(TextureAttribute.Ambient, desc));
                            break;
                        case "reflection":
                            mat.set(new TextureAttribute(TextureAttribute.Reflection, desc));
                            break;
                        default:
                            // arbitrary texture map name: keep it as a custom attribute
                            mat.set(CustomTextureAttribute.of(k, desc));
                            break;
                    }
                }
            }

            // --- FLOATS ---
            if (root.has("floats")) {
                JsonObject floats = root.getAsJsonObject("floats");
                for (String key : floats.keySet()) {
                    float v = floats.get(key).getAsFloat();
                    mat.set(CustomFloatAttribute.of(key, v));
                }
            }

            // --- INTS ---
            if (root.has("ints")) {
                JsonObject ints = root.getAsJsonObject("ints");
                for (String key : ints.keySet()) {
                    int v = ints.get(key).getAsInt();
                    mat.set(CustomIntAttribute.of(key, v));
                }
            }

            // --- COLORS ---
            if (root.has("colors")) {
                JsonObject colors = root.getAsJsonObject("colors");
                for (String key : colors.keySet()) {
                    Color c = parseColor(colors.get(key));
                    // Map well-known names to built-ins for DefaultShader compatibility
                    switch (key.toLowerCase()) {
                        case "diffuse":
                            mat.set(ColorAttribute.createDiffuse(c));
                            break;
                        case "ambient":
                            mat.set(ColorAttribute.createAmbient(c));
                            break;
                        case "specular":
                            mat.set(ColorAttribute.createSpecular(c));
                            break;
                        case "emissive":
                            mat.set(ColorAttribute.createEmissive(c));
                            break;
                        case "reflection":
                            mat.set(ColorAttribute.createReflection(c));
                            break;
                        case "fog":
                            mat.set(ColorAttribute.createFog(c));
                            break;
                        default:
                            // arbitrary color uniform name
                            mat.set(CustomColorAttribute.of(key, c));
                            break;
                    }
                }
            }

            // --- VEC3 ---
            if (root.has("vec3")) {
                JsonObject vecs = root.getAsJsonObject("vec3");
                for (String key : vecs.keySet()) {
                    var arr = vecs.getAsJsonArray(key);
                    Vector3 v = new Vector3(
                            arr.get(0).getAsFloat(),
                            arr.get(1).getAsFloat(),
                            arr.get(2).getAsFloat());
                    mat.set(CustomVec3Attribute.of(key, v));
                }
            }

            // --- VEC2 ---
            if (root.has("vec2")) {
                JsonObject vecs = root.getAsJsonObject("vec2");
                for (String key : vecs.keySet()) {
                    var arr = vecs.getAsJsonArray(key);
                    Vector2 v = new Vector2(
                            arr.get(0).getAsFloat(),
                            arr.get(1).getAsFloat());
                    mat.set(CustomVec2Attribute.of(key, v));
                }
            }

            // --- VEC4 ---
            if (root.has("vec4")) {
                JsonObject vecs = root.getAsJsonObject("vec4");
                for (String key : vecs.keySet()) {
                    var arr = vecs.getAsJsonArray(key);
                    Vector4 v = new Vector4(
                            arr.get(0).getAsFloat(),
                            arr.get(1).getAsFloat(),
                            arr.get(2).getAsFloat(),
                            arr.get(3).getAsFloat());
                    mat.set(CustomVec4Attribute.of(key, v));
                }
            }

            // --- MATRICES ---
            if (root.has("mat3")) {
                JsonObject mats = root.getAsJsonObject("mat3");
                for (String key : mats.keySet()) {
                    var arr = mats.getAsJsonArray(key);
                    Matrix3 m = new Matrix3();
                    for (int i = 0; i < 9; i++)
                        m.val[i] = arr.get(i).getAsFloat();
                    mat.set(CustomMat3Attribute.of(key, m));
                }
            }

            if (root.has("mat4")) {
                JsonObject mats = root.getAsJsonObject("mat4");
                for (String key : mats.keySet()) {
                    var arr = mats.getAsJsonArray(key);
                    Matrix4 m = new Matrix4();
                    for (int i = 0; i < 16; i++)
                        m.val[i] = arr.get(i).getAsFloat();
                    mat.set(CustomMat4Attribute.of(key, m));
                }
            }

            // --- BOOLEAN ---
            if (root.has("bools")) {
                JsonObject bools = root.getAsJsonObject("bools");
                for (String key : bools.keySet()) {
                    boolean v = bools.get(key).getAsBoolean();
                    mat.set(CustomBoolAttribute.of(key, v));
                }
            }

            // --- DOUBLE ---
            if (root.has("doubles")) {
                JsonObject doubles = root.getAsJsonObject("doubles");
                for (String key : doubles.keySet()) {
                    double v = doubles.get(key).getAsDouble();
                    mat.set(CustomDoubleAttribute.of(key, v));
                }
            }

            return mat;
        } catch (Exception e) {
            throw new RuntimeException("Failed parsing material JSON " + file.getName(), e);
        }
    }

    // --------- HELPERS ---------
    private static Color parseColor(JsonElement el) {
        if (el.isJsonArray()) {
            float r = el.getAsJsonArray().get(0).getAsFloat();
            float g = el.getAsJsonArray().get(1).getAsFloat();
            float b = el.getAsJsonArray().get(2).getAsFloat();
            float a = el.getAsJsonArray().size() > 3 ? el.getAsJsonArray().get(3).getAsFloat() : 1f;
            return new Color(r, g, b, a);
        } else {
            return Color.valueOf(el.getAsString());
        }
    }

    // =====================================================================================
    // Custom attributes that allow arbitrary names/aliases (one type per alias)
    // =====================================================================================

    /** Base for “dynamic alias” custom attributes */
    private static abstract class DynamicAliasAttribute extends Attribute {
        protected DynamicAliasAttribute(long type) {
            super(type);
        }

        // Each subclass keeps its own alias->type map
        protected static long typeFor(Map<String, Long> map, String alias) {
            Long t = map.get(alias);
            if (t == null) {
                t = register(alias); // <-- valid: we're inside an Attribute subclass
                map.put(alias, t);
            }
            return t;
        }
    }

    // -------- Float --------
    static final class CustomFloatAttribute extends DynamicAliasAttribute {
        private static final Map<String, Long> TYPES = new HashMap<>();
        public float value;

        public static CustomFloatAttribute of(String alias, float v) {
            return new CustomFloatAttribute(typeFor(TYPES, alias), v);
        }

        private CustomFloatAttribute(long type, float v) {
            super(type);
            this.value = v;
        }

        @Override
        public Attribute copy() {
            return new CustomFloatAttribute(type, value);
        }

        @Override
        public int compareTo(Attribute o) {
            if (type != o.type)
                return type < o.type ? -1 : 1;
            float ov = ((CustomFloatAttribute) o).value;
            return Float.compare(value, ov);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CustomFloatAttribute))
                return false;
            CustomFloatAttribute other = (CustomFloatAttribute) obj;
            return type == other.type && Float.compare(value, other.value) == 0;
        }
    }

    // -------- Int --------
    static final class CustomIntAttribute extends DynamicAliasAttribute {
        private static final Map<String, Long> TYPES = new HashMap<>();
        public int value;

        public static CustomIntAttribute of(String alias, int v) {
            return new CustomIntAttribute(typeFor(TYPES, alias), v);
        }

        private CustomIntAttribute(long type, int v) {
            super(type);
            this.value = v;
        }

        @Override
        public Attribute copy() {
            return new CustomIntAttribute(type, value);
        }

        @Override
        public int compareTo(Attribute o) {
            if (type != o.type)
                return type < o.type ? -1 : 1;
            int ov = ((CustomIntAttribute) o).value;
            return Integer.compare(value, ov);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CustomIntAttribute))
                return false;
            CustomIntAttribute other = (CustomIntAttribute) obj;
            return type == other.type && value == other.value;
        }
    }

    // -------- Color (arbitrary alias) --------
    static final class CustomColorAttribute extends DynamicAliasAttribute {
        private static final Map<String, Long> TYPES = new HashMap<>();
        public final Color color = new Color();

        public static CustomColorAttribute of(String alias, Color c) {
            return new CustomColorAttribute(typeFor(TYPES, alias), c);
        }

        private CustomColorAttribute(long type, Color c) {
            super(type);
            if (c != null)
                this.color.set(c);
        }

        @Override
        public Attribute copy() {
            return new CustomColorAttribute(type, color);
        }

        @Override
        public int compareTo(Attribute o) {
            if (type != o.type)
                return type < o.type ? -1 : 1;
            CustomColorAttribute other = (CustomColorAttribute) o;
            // compare in RGBA order
            int r = Float.compare(color.r, other.color.r);
            if (r != 0)
                return r;
            int g = Float.compare(color.g, other.color.g);
            if (g != 0)
                return g;
            int b = Float.compare(color.b, other.color.b);
            if (b != 0)
                return b;
            return Float.compare(color.a, other.color.a);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, color.r, color.g, color.b, color.a);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CustomColorAttribute))
                return false;
            CustomColorAttribute other = (CustomColorAttribute) obj;
            return type == other.type &&
                    Float.compare(color.r, other.color.r) == 0 &&
                    Float.compare(color.g, other.color.g) == 0 &&
                    Float.compare(color.b, other.color.b) == 0 &&
                    Float.compare(color.a, other.color.a) == 0;
        }
    }

    // -------- Vec3 --------
    static final class CustomVec3Attribute extends DynamicAliasAttribute {
        private static final Map<String, Long> TYPES = new HashMap<>();
        public final Vector3 value = new Vector3();

        public static CustomVec3Attribute of(String alias, Vector3 v) {
            return new CustomVec3Attribute(typeFor(TYPES, alias), v);
        }

        private CustomVec3Attribute(long type, Vector3 v) {
            super(type);
            if (v != null)
                this.value.set(v);
        }

        @Override
        public Attribute copy() {
            return new CustomVec3Attribute(type, value);
        }

        @Override
        public int compareTo(Attribute o) {
            if (type != o.type)
                return type < o.type ? -1 : 1;
            CustomVec3Attribute other = (CustomVec3Attribute) o;
            int x = Float.compare(value.x, other.value.x);
            if (x != 0)
                return x;
            int y = Float.compare(value.y, other.value.y);
            if (y != 0)
                return y;
            return Float.compare(value.z, other.value.z);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value.x, value.y, value.z);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CustomVec3Attribute))
                return false;
            CustomVec3Attribute other = (CustomVec3Attribute) obj;
            return type == other.type &&
                    Float.compare(value.x, other.value.x) == 0 &&
                    Float.compare(value.y, other.value.y) == 0 &&
                    Float.compare(value.z, other.value.z) == 0;
        }
    }

    // -------- Texture (arbitrary alias) --------
    static final class CustomTextureAttribute extends DynamicAliasAttribute {
        private static final Map<String, Long> TYPES = new HashMap<>();
        public final TextureDescriptor<Texture> descriptor;

        public static CustomTextureAttribute of(String alias, TextureDescriptor<Texture> d) {
            return new CustomTextureAttribute(typeFor(TYPES, alias), safeCopy(d));
        }

        private CustomTextureAttribute(long type, TextureDescriptor<Texture> d) {
            super(type);
            this.descriptor = d;
        }

        @Override
        public Attribute copy() {
            return new CustomTextureAttribute(type, safeCopy(descriptor));
        }

        @Override
        public int compareTo(Attribute o) {
            if (type != o.type)
                return type < o.type ? -1 : 1;
            CustomTextureAttribute other = (CustomTextureAttribute) o;
            // compare by texture object identity, then basic params
            if (descriptor.texture != other.descriptor.texture)
                return System.identityHashCode(descriptor.texture) - System.identityHashCode(other.descriptor.texture);
            int m1 = descriptor.minFilter.ordinal() - other.descriptor.minFilter.ordinal();
            if (m1 != 0)
                return m1;
            int m2 = descriptor.magFilter.ordinal() - other.descriptor.magFilter.ordinal();
            if (m2 != 0)
                return m2;
            int u = descriptor.uWrap.ordinal() - other.descriptor.uWrap.ordinal();
            if (u != 0)
                return u;
            return descriptor.vWrap.ordinal() - other.descriptor.vWrap.ordinal();
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, descriptor.texture, descriptor.minFilter, descriptor.magFilter,
                    descriptor.uWrap, descriptor.vWrap);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CustomTextureAttribute))
                return false;
            CustomTextureAttribute other = (CustomTextureAttribute) obj;
            return type == other.type &&
                    descriptor.texture == other.descriptor.texture &&
                    descriptor.minFilter == other.descriptor.minFilter &&
                    descriptor.magFilter == other.descriptor.magFilter &&
                    descriptor.uWrap == other.descriptor.uWrap &&
                    descriptor.vWrap == other.descriptor.vWrap;
        }

        private static TextureDescriptor<Texture> safeCopy(TextureDescriptor<Texture> src) {
            TextureDescriptor<Texture> d = new TextureDescriptor<>(src.texture);
            // Copy only widely-available fields (keeps this source-compatible across libGDX
            // versions)
            d.minFilter = src.minFilter;
            d.magFilter = src.magFilter;
            d.uWrap = src.uWrap;
            d.vWrap = src.vWrap;
            // Do NOT touch non-existent fields like "flipY"
            return d;
        }
    }

    // -------- Vector2 --------
    static final class CustomVec2Attribute extends DynamicAliasAttribute {
        private static final Map<String, Long> TYPES = new HashMap<>();
        public final Vector2 value = new Vector2();

        public static CustomVec2Attribute of(String alias, Vector2 v) {
            return new CustomVec2Attribute(typeFor(TYPES, alias), v);
        }

        private CustomVec2Attribute(long type, Vector2 v) {
            super(type);
            if (v != null)
                value.set(v);
        }

        @Override
        public Attribute copy() {
            return new CustomVec2Attribute(type, value);
        }

        @Override
        public int compareTo(Attribute o) {
            if (type != o.type)
                return type < o.type ? -1 : 1;
            CustomVec2Attribute other = (CustomVec2Attribute) o;
            int x = Float.compare(value.x, other.value.x);
            if (x != 0)
                return x;
            return Float.compare(value.y, other.value.y);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value.x, value.y);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CustomVec2Attribute))
                return false;
            CustomVec2Attribute other = (CustomVec2Attribute) obj;
            return type == other.type &&
                    Float.compare(value.x, other.value.x) == 0 &&
                    Float.compare(value.y, other.value.y) == 0;
        }
    }

    // -------- Vector4 --------
    static final class CustomVec4Attribute extends DynamicAliasAttribute {
        private static final Map<String, Long> TYPES = new HashMap<>();
        public final Vector4 value = new Vector4();

        public static CustomVec4Attribute of(String alias, Vector4 v) {
            return new CustomVec4Attribute(typeFor(TYPES, alias), v);
        }

        private CustomVec4Attribute(long type, Vector4 v) {
            super(type);
            if (v != null)
                value.set(v);
        }

        @Override
        public Attribute copy() {
            return new CustomVec4Attribute(type, value);
        }

        @Override
        public int compareTo(Attribute o) {
            if (type != o.type)
                return type < o.type ? -1 : 1;
            CustomVec4Attribute other = (CustomVec4Attribute) o;
            int x = Float.compare(value.x, other.value.x);
            if (x != 0)
                return x;
            int y = Float.compare(value.y, other.value.y);
            if (y != 0)
                return y;
            int z = Float.compare(value.z, other.value.z);
            if (z != 0)
                return z;
            return Float.compare(value.w, other.value.w);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value.x, value.y, value.z, value.w);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CustomVec4Attribute))
                return false;
            CustomVec4Attribute other = (CustomVec4Attribute) obj;
            return type == other.type &&
                    Float.compare(value.x, other.value.x) == 0 &&
                    Float.compare(value.y, other.value.y) == 0 &&
                    Float.compare(value.z, other.value.z) == 0 &&
                    Float.compare(value.w, other.value.w) == 0;
        }
    }

    // -------- Matrix3 --------
    static final class CustomMat3Attribute extends DynamicAliasAttribute {
        private static final Map<String, Long> TYPES = new HashMap<>();
        public final Matrix3 value = new Matrix3();

        public static CustomMat3Attribute of(String alias, Matrix3 m) {
            return new CustomMat3Attribute(typeFor(TYPES, alias), m);
        }

        private CustomMat3Attribute(long type, Matrix3 m) {
            super(type);
            if (m != null)
                value.set(m);
        }

        @Override
        public Attribute copy() {
            return new CustomMat3Attribute(type, value);
        }

        @Override
        public int compareTo(Attribute o) {
            if (type != o.type)
                return type < o.type ? -1 : 1;
            CustomMat3Attribute other = (CustomMat3Attribute) o;
            for (int i = 0; i < 9; i++) {
                int cmp = Float.compare(value.val[i], other.value.val[i]);
                if (cmp != 0)
                    return cmp;
            }
            return 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, Arrays.hashCode(value.val));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CustomMat3Attribute))
                return false;
            CustomMat3Attribute other = (CustomMat3Attribute) obj;
            return type == other.type && Arrays.equals(value.val, other.value.val);
        }
    }

    // -------- Matrix4 --------
    static final class CustomMat4Attribute extends DynamicAliasAttribute {
        private static final Map<String, Long> TYPES = new HashMap<>();
        public final Matrix4 value = new Matrix4();

        public static CustomMat4Attribute of(String alias, Matrix4 m) {
            return new CustomMat4Attribute(typeFor(TYPES, alias), m);
        }

        private CustomMat4Attribute(long type, Matrix4 m) {
            super(type);
            if (m != null)
                value.set(m);
        }

        @Override
        public Attribute copy() {
            return new CustomMat4Attribute(type, value);
        }

        @Override
        public int compareTo(Attribute o) {
            if (type != o.type)
                return type < o.type ? -1 : 1;
            CustomMat4Attribute other = (CustomMat4Attribute) o;
            for (int i = 0; i < 16; i++) {
                int cmp = Float.compare(value.val[i], other.value.val[i]);
                if (cmp != 0)
                    return cmp;
            }
            return 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, Arrays.hashCode(value.val));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CustomMat4Attribute))
                return false;
            CustomMat4Attribute other = (CustomMat4Attribute) obj;
            return type == other.type && Arrays.equals(value.val, other.value.val);
        }
    }

    // -------- Boolean --------
    static final class CustomBoolAttribute extends DynamicAliasAttribute {
        private static final Map<String, Long> TYPES = new HashMap<>();
        public final boolean value;

        public static CustomBoolAttribute of(String alias, boolean v) {
            return new CustomBoolAttribute(typeFor(TYPES, alias), v);
        }

        private CustomBoolAttribute(long type, boolean v) {
            super(type);
            this.value = v;
        }

        @Override
        public Attribute copy() {
            return new CustomBoolAttribute(type, value);
        }

        @Override
        public int compareTo(Attribute o) {
            if (type != o.type)
                return type < o.type ? -1 : 1;
            CustomBoolAttribute other = (CustomBoolAttribute) o;
            return Boolean.compare(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CustomBoolAttribute))
                return false;
            CustomBoolAttribute other = (CustomBoolAttribute) obj;
            return type == other.type && value == other.value;
        }
    }

    // -------- Double --------
    static final class CustomDoubleAttribute extends DynamicAliasAttribute {
        private static final Map<String, Long> TYPES = new HashMap<>();
        public final double value;

        public static CustomDoubleAttribute of(String alias, double v) {
            return new CustomDoubleAttribute(typeFor(TYPES, alias), v);
        }

        private CustomDoubleAttribute(long type, double v) {
            super(type);
            this.value = v;
        }

        @Override
        public Attribute copy() {
            return new CustomDoubleAttribute(type, value);
        }

        @Override
        public int compareTo(Attribute o) {
            if (type != o.type)
                return type < o.type ? -1 : 1;
            CustomDoubleAttribute other = (CustomDoubleAttribute) o;
            return Double.compare(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof CustomDoubleAttribute))
                return false;
            CustomDoubleAttribute other = (CustomDoubleAttribute) obj;
            return type == other.type && Double.compare(value, other.value) == 0;
        }
    }

}
