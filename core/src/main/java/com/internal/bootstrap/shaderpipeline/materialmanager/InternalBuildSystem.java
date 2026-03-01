package com.internal.bootstrap.shaderpipeline.materialmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderHandle;
import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureHandle;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.mathematics.matrices.*;
import com.internal.core.util.mathematics.vectors.*;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Constructs MaterialHandle objects from JSON descriptors during bootstrap.
 * Resolves shader and UBO references by name, parses uniform initial values,
 * and resolves texture names to GPU handles. Released after bootstrap completes.
 */
public class InternalBuildSystem extends SystemPackage {

    // Internal
    private TextureManager textureManager;
    private ShaderManager shaderManager;
    private UBOManager uboManager;

    // Internal \\

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.shaderManager = get(ShaderManager.class);
        this.uboManager = get(UBOManager.class);
    }

    // Material Management \\

    MaterialHandle buildMaterial(File root, File file, int materialID) {

        String materialName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        JsonObject json = JsonUtility.loadJsonObject(file);
        int shaderID = getShaderID(json);

        ShaderHandle shader = shaderManager.getShaderFromShaderID(shaderID);

        Object2ObjectOpenHashMap<String, UBOHandle> ubos = buildUBOMap(shader);
        Object2ObjectOpenHashMap<String, Uniform<?>> uniforms = shader.getUniforms();

        updateUniforms(json, uniforms);

        MaterialHandle materialHandle = create(MaterialHandle.class);
        materialHandle.constructor(materialName, materialID, shader, ubos, uniforms);

        return materialHandle;
    }

    private int getShaderID(JsonObject json) {
        String shaderName = JsonUtility.validateString(json, "shader");
        return shaderManager.getShaderIDFromShaderName(shaderName);
    }

    private Object2ObjectOpenHashMap<String, UBOHandle> buildUBOMap(ShaderHandle shader) {

        ObjectArrayList<String> blockNames = shader.getUBOBlockNames();
        Object2ObjectOpenHashMap<String, UBOHandle> ubos = new Object2ObjectOpenHashMap<>();

        for (int i = 0; i < blockNames.size(); i++) {
            String name = blockNames.get(i);
            ubos.put(name, uboManager.getUBOHandleFromUBOName(name));
        }

        return ubos;
    }

    private void updateUniforms(
            JsonObject json,
            Object2ObjectOpenHashMap<String, Uniform<?>> uniforms) {

        for (String key : json.keySet()) {

            if (key.equals("shader"))
                continue;

            Uniform<?> uniform = uniforms.get(key);
            if (uniform == null)
                continue;

            updateUniform(json, key, uniform);
        }
    }

    private void updateUniform(
            JsonObject json,
            String uniformName,
            Uniform<?> uniform) {

        JsonElement element = json.get(uniformName);

        if (!element.isJsonObject())
            throwException("Uniform '" + uniformName + "' must be a JSON object with 'type' and 'value' fields");

        JsonObject uniformData = element.getAsJsonObject();

        if (!uniformData.has("type"))
            throwException("Uniform '" + uniformName + "' missing required 'type' field");

        if (!uniformData.has("value"))
            throwException("Uniform '" + uniformName + "' missing required 'value' field");

        String type = uniformData.get("type").getAsString();
        JsonElement valueElement = uniformData.get("value");
        boolean isArray = uniformData.has("count");

        UniformAttribute<?> attribute = uniform.attribute();

        if (isArray)
            updateArrayUniform(attribute, type, valueElement);
        else
            updateSingleUniform(attribute, type, valueElement);
    }

    @SuppressWarnings("unchecked")
    private void updateSingleUniform(
            UniformAttribute<?> attribute,
            String type,
            JsonElement valueElement) {

        switch (type) {
            case "FLOAT" -> ((UniformAttribute<Float>) attribute).set(valueElement.getAsFloat());
            case "DOUBLE" -> ((UniformAttribute<Double>) attribute).set(valueElement.getAsDouble());
            case "INT" -> ((UniformAttribute<Integer>) attribute).set(valueElement.getAsInt());
            case "BOOL" -> ((UniformAttribute<Boolean>) attribute).set(valueElement.getAsBoolean());
            case "VECTOR2" -> ((UniformAttribute<Vector2>) attribute)
                    .set(MaterialParseUtility.parseVector2(valueElement.getAsJsonArray()));
            case "VECTOR2_DOUBLE" -> ((UniformAttribute<Vector2Double>) attribute)
                    .set(MaterialParseUtility.parseVector2Double(valueElement.getAsJsonArray()));
            case "VECTOR2_INT" -> ((UniformAttribute<Vector2Int>) attribute)
                    .set(MaterialParseUtility.parseVector2Int(valueElement.getAsJsonArray()));
            case "VECTOR2_BOOLEAN" -> ((UniformAttribute<Vector2Boolean>) attribute)
                    .set(MaterialParseUtility.parseVector2Boolean(valueElement.getAsJsonArray()));
            case "VECTOR3" -> ((UniformAttribute<Vector3>) attribute)
                    .set(MaterialParseUtility.parseVector3(valueElement.getAsJsonArray()));
            case "VECTOR3_DOUBLE" -> ((UniformAttribute<Vector3Double>) attribute)
                    .set(MaterialParseUtility.parseVector3Double(valueElement.getAsJsonArray()));
            case "VECTOR3_INT" -> ((UniformAttribute<Vector3Int>) attribute)
                    .set(MaterialParseUtility.parseVector3Int(valueElement.getAsJsonArray()));
            case "VECTOR3_BOOLEAN" -> ((UniformAttribute<Vector3Boolean>) attribute)
                    .set(MaterialParseUtility.parseVector3Boolean(valueElement.getAsJsonArray()));
            case "VECTOR4" -> ((UniformAttribute<Vector4>) attribute)
                    .set(MaterialParseUtility.parseVector4(valueElement.getAsJsonArray()));
            case "VECTOR4_DOUBLE" -> ((UniformAttribute<Vector4Double>) attribute)
                    .set(MaterialParseUtility.parseVector4Double(valueElement.getAsJsonArray()));
            case "VECTOR4_INT" -> ((UniformAttribute<Vector4Int>) attribute)
                    .set(MaterialParseUtility.parseVector4Int(valueElement.getAsJsonArray()));
            case "VECTOR4_BOOLEAN" -> ((UniformAttribute<Vector4Boolean>) attribute)
                    .set(MaterialParseUtility.parseVector4Boolean(valueElement.getAsJsonArray()));
            case "MATRIX2" -> ((UniformAttribute<Matrix2>) attribute)
                    .set(MaterialParseUtility.parseMatrix2(valueElement.getAsJsonArray()));
            case "MATRIX3" -> ((UniformAttribute<Matrix3>) attribute)
                    .set(MaterialParseUtility.parseMatrix3(valueElement.getAsJsonArray()));
            case "MATRIX4" -> ((UniformAttribute<Matrix4>) attribute)
                    .set(MaterialParseUtility.parseMatrix4(valueElement.getAsJsonArray()));
            case "MATRIX2_DOUBLE" -> ((UniformAttribute<Matrix2Double>) attribute)
                    .set(MaterialParseUtility.parseMatrix2Double(valueElement.getAsJsonArray()));
            case "MATRIX3_DOUBLE" -> ((UniformAttribute<Matrix3Double>) attribute)
                    .set(MaterialParseUtility.parseMatrix3Double(valueElement.getAsJsonArray()));
            case "MATRIX4_DOUBLE" -> ((UniformAttribute<Matrix4Double>) attribute)
                    .set(MaterialParseUtility.parseMatrix4Double(valueElement.getAsJsonArray()));
            case "SAMPLE_IMAGE_2D",
                    "SAMPLE_IMAGE_2D_ARRAY" ->
                ((UniformAttribute<Integer>) attribute).set(resolveTextureGPUHandle(valueElement.getAsString()));
            default -> throwException("Unsupported uniform type: " + type);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateArrayUniform(
            UniformAttribute<?> attribute,
            String type,
            JsonElement valueElement) {

        if (!valueElement.isJsonArray())
            throwException("Array uniform value must be a JSON array");

        JsonArray array = valueElement.getAsJsonArray();

        switch (type) {
            case "FLOAT" -> {
                Float[] values = new Float[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = array.get(i).getAsFloat();
                ((UniformAttribute<Float[]>) attribute).set(values);
            }
            case "DOUBLE" -> {
                Double[] values = new Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = array.get(i).getAsDouble();
                ((UniformAttribute<Double[]>) attribute).set(values);
            }
            case "INT" -> {
                Integer[] values = new Integer[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = array.get(i).getAsInt();
                ((UniformAttribute<Integer[]>) attribute).set(values);
            }
            case "BOOL" -> {
                Boolean[] values = new Boolean[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = array.get(i).getAsBoolean();
                ((UniformAttribute<Boolean[]>) attribute).set(values);
            }
            case "VECTOR2" -> {
                Vector2[] values = new Vector2[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseVector2(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector2[]>) attribute).set(values);
            }
            case "VECTOR2_DOUBLE" -> {
                Vector2Double[] values = new Vector2Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseVector2Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector2Double[]>) attribute).set(values);
            }
            case "VECTOR2_INT" -> {
                Vector2Int[] values = new Vector2Int[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseVector2Int(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector2Int[]>) attribute).set(values);
            }
            case "VECTOR3" -> {
                Vector3[] values = new Vector3[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseVector3(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector3[]>) attribute).set(values);
            }
            case "VECTOR3_DOUBLE" -> {
                Vector3Double[] values = new Vector3Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseVector3Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector3Double[]>) attribute).set(values);
            }
            case "VECTOR3_INT" -> {
                Vector3Int[] values = new Vector3Int[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseVector3Int(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector3Int[]>) attribute).set(values);
            }
            case "VECTOR4" -> {
                Vector4[] values = new Vector4[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseVector4(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector4[]>) attribute).set(values);
            }
            case "VECTOR4_DOUBLE" -> {
                Vector4Double[] values = new Vector4Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseVector4Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector4Double[]>) attribute).set(values);
            }
            case "VECTOR4_INT" -> {
                Vector4Int[] values = new Vector4Int[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseVector4Int(array.get(i).getAsJsonArray());
                ((UniformAttribute<Vector4Int[]>) attribute).set(values);
            }
            case "MATRIX2" -> {
                Matrix2[] values = new Matrix2[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseMatrix2(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix2[]>) attribute).set(values);
            }
            case "MATRIX3" -> {
                Matrix3[] values = new Matrix3[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseMatrix3(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix3[]>) attribute).set(values);
            }
            case "MATRIX4" -> {
                Matrix4[] values = new Matrix4[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseMatrix4(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix4[]>) attribute).set(values);
            }
            case "MATRIX2_DOUBLE" -> {
                Matrix2Double[] values = new Matrix2Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseMatrix2Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix2Double[]>) attribute).set(values);
            }
            case "MATRIX3_DOUBLE" -> {
                Matrix3Double[] values = new Matrix3Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseMatrix3Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix3Double[]>) attribute).set(values);
            }
            case "MATRIX4_DOUBLE" -> {
                Matrix4Double[] values = new Matrix4Double[array.size()];
                for (int i = 0; i < array.size(); i++)
                    values[i] = MaterialParseUtility.parseMatrix4Double(array.get(i).getAsJsonArray());
                ((UniformAttribute<Matrix4Double[]>) attribute).set(values);
            }
            default -> throwException("Unsupported uniform array type: " + type);
        }
    }

    private int resolveTextureGPUHandle(String textureArrayName) {
        TextureHandle textureHandle = textureManager.getArrayHandleFromArrayName(textureArrayName);
        if (textureHandle == null)
            throwException("Texture array not found: " + textureArrayName);
        return textureHandle.getGPUHandle();
    }
}