package com.AdventureRPG.Core.Util.Exceptions;

public class GraphicException {

    private GraphicException() {
    } // prevents instantiation

    // Shader file missing (JSON, .vert, .frag, include)
    public static class ShaderFileNotFoundException extends RuntimeException {
        public ShaderFileNotFoundException(String fileName) {
            super("Graphic Exception: Shader file not found: " + fileName);
        }
    }

    // Shader JSON missing required fields (vertex/fragment)
    public static class ShaderDefinitionException extends RuntimeException {
        public ShaderDefinitionException(String jsonName) {
            super("Graphic Exception: Shader definition invalid in: " + jsonName +
                    " (missing vertex or fragment field)");
        }
    }

    // GLSL compile or link failure
    public static class ShaderCompilationException extends RuntimeException {
        public ShaderCompilationException(String shaderName, String log) {
            super("Graphic Exception: Shader compile error in: " + shaderName +
                    "\nLog:\n" + log);
        }
    }

    // Pass JSON malformed (uniform missing type/value, invalid field structure)
    public static class PassDefinitionException extends RuntimeException {
        public PassDefinitionException(String jsonName) {
            super("Graphic Exception: Pass definition invalid in: " + jsonName +
                    " (missing or invalid fields)");
        }
    }

    // JSON file parsed but missing required "texture" entry
    public static class MissingTextureFieldException extends RuntimeException {
        public MissingTextureFieldException(String jsonName) {
            super("Graphic Exception: Missing required 'texture' field in material JSON: " + jsonName);
        }
    }

    // JSON references a folder/texture array that does not exist or failed to load
    public static class TextureArrayNotFoundException extends RuntimeException {
        public TextureArrayNotFoundException(String folderName, String jsonName) {
            super("Graphic Exception: TextureArray not found for folder '" + folderName +
                    "' referenced in material JSON: " + jsonName);
        }
    }

    // JSON structure invalid
    public static class MaterialDefinitionException extends RuntimeException {
        public MaterialDefinitionException(String jsonName, Throwable cause) {
            super("Graphic Exception: Invalid material definition in: " + jsonName, cause);
        }

        public MaterialDefinitionException(String jsonName) {
            super("Graphic Exception: Invalid material definition in: " + jsonName);
        }
    }

    // Shader reference in material JSON not resolvable via ShaderManager
    public static class ShaderNotFoundForMaterialException extends RuntimeException {
        public ShaderNotFoundForMaterialException(String shaderName, String jsonName) {
            super("Graphic Exception: Shader '" + shaderName +
                    "' not found for material JSON: " + jsonName);
        }
    }
}
