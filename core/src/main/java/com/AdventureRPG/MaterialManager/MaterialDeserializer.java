package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.TextureManager.TextureManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.file.Files;

class MaterialDeserializer {
    static MaterialDefinition parse(File file, TextureManager texMgr, Gson gson) {
        try {
            String json = Files.readString(file.toPath());
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            MaterialDefinition def = new MaterialDefinition();
            def.name = root.get("name").getAsString();

            String vert = root.get("vertex").getAsString();
            String frag = root.get("fragment").getAsString();

            def.shader = new ShaderProgram(Gdx.files.internal(vert), Gdx.files.internal(frag));
            if (!def.shader.isCompiled()) {
                throw new RuntimeException("Shader compile failed: " + def.shader.getLog());
            }

            if (root.has("maps")) {
                JsonObject maps = root.getAsJsonObject("maps");
                for (String key : maps.keySet()) {
                    def.textureRefs.put(key, maps.get(key).getAsString());
                }
            }

            if (root.has("floats")) {
                JsonObject floats = root.getAsJsonObject("floats");
                for (String key : floats.keySet()) {
                    def.floatUniforms.put(key, floats.get(key).getAsFloat());
                }
            }

            // TODO: same for colors, ints, vec3s, etc.

            return def;
        } catch (Exception e) {
            throw new RuntimeException("Failed parsing material JSON " + file.getName(), e);
        }
    }
}
