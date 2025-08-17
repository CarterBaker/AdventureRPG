package com.AdventureRPG.MaterialManager;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

class MaterialDefinition {
    int id;
    String name;
    ShaderProgram shader;
    Map<String, String> textureRefs = new HashMap<>(); // e.g. "albedo" -> "stoneAtlas"
    Map<String, Float> floatUniforms = new HashMap<>();
    Map<String, String> stringUniforms = new HashMap<>();
    // Add Color, Vector3, etc. later as needed
}
