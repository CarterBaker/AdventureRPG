package com.AdventureRPG.core.shaders.shadermanager;

import java.io.File;
import java.nio.file.Path;

import com.AdventureRPG.core.engine.EngineSetting;
import com.AdventureRPG.core.engine.SystemFrame;
import com.AdventureRPG.core.shaders.ubomanager.UBOData;
import com.AdventureRPG.core.shaders.uniforms.UniformData;
import com.AdventureRPG.core.shaders.uniforms.UniformType;
import com.AdventureRPG.core.util.JsonUtility;
import com.AdventureRPG.core.util.Exceptions.FileException;
import com.AdventureRPG.core.util.Exceptions.GraphicException;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InternalBuildSystem extends SystemFrame {

    // Internal
    private InternalLoadManager internalLoadManager;
    private File root;

    private int nextAutoBinding;
    private IntOpenHashSet usedBindings;

    // Base \\

    @Override
    protected void init() {

        // Internal
        this.internalLoadManager = gameEngine.get(InternalLoadManager.class);
        this.root = new File(EngineSetting.SHADER_PATH);

        this.nextAutoBinding = 0;
        this.usedBindings = new IntOpenHashSet();
    }

    // Compile \\

    void parseShaderFile(ShaderData shaderData) {

        String rawText = FileParserUtility.convertFileToRawText(shaderData.shaderFile());
        ObjectArrayList<String> textArray = FileParserUtility.convertRawTextToArray(rawText);

        shaderData.setVersion(parseVersionInfo(textArray));

        parseUniforms(
                shaderData,
                textArray);

        parseIncludes(
                shaderData,
                textArray);
    }

    // Version \\

    private String parseVersionInfo(
            ObjectArrayList<String> textArray) {

        for (String line : textArray)
            if (FileParserUtility.lineStartsWith(line, "#version"))
                return line;

        return null;
    }

    // Uniforms \\

    private void parseUniforms(
            ShaderData shaderData,
            ObjectArrayList<String> textArray) {

        boolean insideBlock = false;
        UBOData currentBuffer = null;
        int braceDepth = 0;

        for (int i = 0; i < textArray.size(); i++) {
            String line = textArray.get(i).trim();

            // Skip empty lines
            if (line.isEmpty())
                continue;

            // Check for uniform block start (with or without buffer)
            if (isUniformBlockStart(line)) {

                currentBuffer = parseUniformBlockStart(line);

                if (currentBuffer != null) {

                    insideBlock = true;
                    braceDepth = FileParserUtility.countCharInString(line, '{') -
                            FileParserUtility.countCharInString(line, '}');
                }

                continue;
            }

            // Track brace depth when inside block
            if (insideBlock) {

                braceDepth += FileParserUtility.countCharInString(line, '{') -
                        FileParserUtility.countCharInString(line, '}');

                // Block ended - add it to shader data
                if (braceDepth <= 0) {

                    if (currentBuffer != null)
                        shaderData.addBufferBlock(currentBuffer);

                    currentBuffer = null;
                    insideBlock = false;

                    continue;
                }
            }

            // Parse uniform declarations (both standalone and inside blocks)
            if (line.contains("uniform") || (insideBlock && !line.equals("}"))) {

                // Handle multi-line declarations
                String fullDeclaration = line;
                while (!fullDeclaration.endsWith(";") && i + 1 < textArray.size()) {

                    i++;
                    String nextLine = textArray.get(i).trim();

                    if (!nextLine.isEmpty())
                        fullDeclaration += " " + nextLine;
                }

                // Parse and add uniforms
                parseUniformDeclaration(fullDeclaration, currentBuffer, shaderData);
            }
        }
    }

    private boolean isUniformBlockStart(String line) {
        // Matches: "buffer(...) uniform BlockName {" or "uniform BlockName {"
        return line.contains("uniform") &&
                line.contains("{") &&
                !line.contains(";") &&
                !line.matches(".*\\buniform\\s+\\w+\\s+\\w+.*"); // Not "uniform type name"
    }

    private UBOData parseUniformBlockStart(String line) {

        try {

            int binding = -1; // -1 means "not specified"

            // Extract binding if layout or buffer exists
            if (line.contains("layout") || line.contains("buffer"))
                binding = FileParserUtility.extractBufferBinding(line);

            // Extract block name (text between "uniform" and "{")
            int uniformIdx = line.indexOf("uniform");
            int braceIdx = line.indexOf("{");

            if (uniformIdx != -1 && braceIdx != -1) {

                String blockName = line.substring(uniformIdx + 7, braceIdx).trim();
                // Remove any buffer/layout qualifiers that might be in the name
                blockName = blockName.replaceAll("\\(.*?\\)", "").trim();

                // Auto-assign binding if not specified
                if (binding == -1 || binding == 0) {
                    binding = nextAutoBinding++;
                    System.out.println("Auto-assigned binding " + binding + " to UBO '" + blockName + "'");
                }

                // Check for binding collision
                if (usedBindings.contains(binding)) {
                    throw new GraphicException.ShaderProgramException(
                            "Binding point collision! UBO '" + blockName + "' attempted to use binding " + binding +
                                    " which is already in use. Each UBO must have a unique binding point.");
                }

                // Mark this binding as used
                usedBindings.add(binding);

                return new UBOData(blockName, binding);
            }

        }

        catch (GraphicException.ShaderProgramException e) {
            throw e; // Re-throw our own exceptions
        } catch (Exception e) {
            throw new GraphicException.ShaderProgramException(
                    "Failed to parse uniform block: " + line, e);
        }
        return null;
    }

    private void parseUniformDeclaration(
            String declaration,
            UBOData currentBuffer,
            ShaderData shaderData) {

        // Clean up declaration
        declaration = declaration.replace(";", "").trim();

        // Skip empty, closing braces, or struct definitions
        if (declaration.isEmpty() ||
                declaration.equals("}") ||
                declaration.startsWith("struct"))
            return;

        // Skip if not a uniform declaration (when outside buffer blocks)
        if (currentBuffer == null && !declaration.contains("uniform"))
            return;

        // Remove "uniform" keyword
        declaration = declaration.replaceFirst("\\buniform\\b", "").trim();

        // Split into type and variable names (handle spaces in type names)
        int lastSpaceBeforeNames = FileParserUtility.findLastTypeDelimiter(declaration);
        if (lastSpaceBeforeNames == -1)
            return;

        String typeStr = declaration.substring(0, lastSpaceBeforeNames).trim();
        String namesStr = declaration.substring(lastSpaceBeforeNames).trim();

        // Parse uniform type
        UniformType uniformType = parseUniformType(typeStr);
        if (uniformType == null)
            return;

        // Parse all variable names
        parseVariableNames(namesStr, uniformType, currentBuffer, shaderData);
    }

    private void parseVariableNames(
            String namesStr,
            UniformType uniformType,
            UBOData currentBuffer,
            ShaderData shaderData) {

        String[] names = namesStr.split(",");

        for (String name : names) {

            name = name.trim();
            if (name.isEmpty())
                continue;

            // Parse array syntax
            int arrayCount = 1;
            String variableName = name;

            if (name.contains("[")) {
                int bracketStart = name.indexOf("[");
                int bracketEnd = name.indexOf("]");

                if (bracketStart != -1 && bracketEnd != -1 && bracketEnd > bracketStart) {

                    String countStr = name.substring(bracketStart + 1, bracketEnd).trim();

                    if (countStr.isEmpty()) // Handle empty brackets: uniform vec4 values[];
                        arrayCount = 0; // Mark as dynamic/unsized

                    else // Try to parse as integer, default to 0 for macros/constants
                        arrayCount = FileParserUtility.parseIntOrDefault(countStr, 0);

                    variableName = name.substring(0, bracketStart).trim();
                }
            }

            if (arrayCount == 0)
                throw new GraphicException.ShaderProgramException(
                        "Unsized arrays not supported: " + variableName);

            // Create uniform instance
            UniformData uniform = new UniformData(uniformType, variableName, arrayCount);

            // Add to appropriate container
            if (currentBuffer != null)
                currentBuffer.addUniform(uniform);
            else
                shaderData.addUniform(uniform);
        }
    }

    private UniformType parseUniformType(String typeStr) {

        try {
            // Try standard GLSL types first
            return UniformType.valueOf(typeStr.toUpperCase());
        }

        catch (IllegalArgumentException e) {

            // Fall back to custom parser (handles user types, structs, etc.)
            try {
                return UniformType.fromString(typeStr);
            }

            catch (Exception ex) {
                // Unknown type - you could return a default or null
                return null;
            }
        }
    }

    // Includes \\

    private void parseIncludes(
            ShaderData shaderData,
            ObjectArrayList<String> textArray) {

        for (String line : textArray) {

            if (FileParserUtility.lineStartsWith(line, "#include")) {

                String includePath = FileParserUtility.extractPayloadAfterToken(line, "#include");

                if (includePath != null && !includePath.isEmpty()) {

                    ShaderData includeShader = internalLoadManager.getShaderData(includePath);

                    if (includeShader != null)
                        shaderData.addIncludes(includeShader);
                }
            }
        }
    }

    // Compile \\

    ShaderDefinitionInstance compileShader(File jsonFile) {

        // Extract relative path from root shader directory
        Path rootPath = root.toPath();
        Path jsonPath = jsonFile.toPath();
        String relativePath = rootPath.relativize(jsonPath).toString().replace("\\", "/");

        // Remove the .json extension to get the shader name with path
        String shaderName = relativePath;
        if (shaderName.endsWith(".json")) {
            shaderName = shaderName.substring(0, shaderName.length() - 5);
        }

        JsonObject obj = JsonUtility.loadJsonObject(jsonFile);
        ShaderData vertData = internalLoadManager.getShaderData(obj.get("vert").getAsString());
        ShaderData fragData = internalLoadManager.getShaderData(obj.get("frag").getAsString());

        if (vertData == null || fragData == null)
            throw new FileException.FileReadException(
                    "Json data error: " + jsonFile.getName() + ", The vert or frag file defined could not be found");

        if (vertData.shaderType() != ShaderType.VERT || fragData.shaderType() != ShaderType.FRAG)
            throw new GraphicException.ShaderProgramException(
                    "Json data error: " + jsonFile.getName()
                            + ", The vert or frag files defined do not match the corresponding type");

        return sortIncludes(new ShaderDefinitionInstance(shaderName, vertData, fragData));
    }

    private ShaderDefinitionInstance sortIncludes(ShaderDefinitionInstance shaderDefinition) {

        ObjectArrayList<ShaderData> visited = new ObjectArrayList<>();

        collectRecursiveIncludes(
                shaderDefinition,
                shaderDefinition.vert,
                visited);
        collectRecursiveIncludes(
                shaderDefinition,
                shaderDefinition.frag,
                visited);

        return shaderDefinition;
    }

    private void collectRecursiveIncludes(
            ShaderDefinitionInstance shaderDefinition,
            ShaderData shaderData,
            ObjectArrayList<ShaderData> visited) {

        if (visited.contains(shaderData))
            return; // Cycle detected

        visited.add(shaderData);

        if (shaderData.shaderType() == ShaderType.INCLUDE &&
                !shaderDefinition.getIncludes().contains(shaderData))
            shaderDefinition.addInclude(shaderData);

        for (ShaderData include : shaderData.getIncludes())
            collectRecursiveIncludes(shaderDefinition, include, visited);
    }
}