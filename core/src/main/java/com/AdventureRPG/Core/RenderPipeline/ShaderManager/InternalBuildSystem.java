package com.AdventureRPG.core.renderpipeline.shadermanager;

import java.io.File;
import java.nio.file.Path;

import com.AdventureRPG.core.kernel.EngineSetting;
import com.AdventureRPG.core.kernel.SystemFrame;
import com.AdventureRPG.core.util.FileParserUtility;
import com.AdventureRPG.core.util.JsonUtility;
import com.AdventureRPG.core.util.Exceptions.FileException;
import com.AdventureRPG.core.util.Exceptions.GraphicException;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InternalBuildSystem extends SystemFrame {

    // Internal
    private InternalLoadManager internalLoadManager;
    private File root;

    // Base \\

    @Override
    protected void init() {
        this.internalLoadManager = gameEngine.get(InternalLoadManager.class);
        this.root = new File(EngineSetting.SHADER_PATH);
    }

    // Compile \\

    void parseShaderFile(ShaderDataInstance shaderDataInstance) {

        String rawText = FileParserUtility.convertFileToRawText(shaderDataInstance.shaderFile());
        ObjectArrayList<String> textArray = FileParserUtility.convertRawTextToArray(rawText);

        shaderDataInstance.setVersion(parseVersionInfo(textArray));

        parseUniforms(
                shaderDataInstance,
                textArray);

        parseIncludes(
                shaderDataInstance,
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
            ShaderDataInstance shaderDataInstance,
            ObjectArrayList<String> textArray) {

        boolean insideBlock = false;
        LayoutDataInstance currentLayout = null;
        int braceDepth = 0;

        for (int i = 0; i < textArray.size(); i++) {
            String line = textArray.get(i).trim();

            // Skip empty lines
            if (line.isEmpty())
                continue;

            // Check for uniform block start (with or without layout)
            if (isUniformBlockStart(line)) {

                currentLayout = parseUniformBlockStart(line);

                if (currentLayout != null) {

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

                    if (currentLayout != null)
                        shaderDataInstance.addLayoutBlock(currentLayout);

                    currentLayout = null;
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
                parseUniformDeclaration(fullDeclaration, currentLayout, shaderDataInstance);
            }
        }
    }

    private boolean isUniformBlockStart(String line) {
        // Matches: "layout(...) uniform BlockName {" or "uniform BlockName {"
        return line.contains("uniform") &&
                line.contains("{") &&
                !line.contains(";") &&
                !line.matches(".*\\buniform\\s+\\w+\\s+\\w+.*"); // Not "uniform type name"
    }

    private LayoutDataInstance parseUniformBlockStart(String line) {

        try {

            int binding = 0;

            // Extract binding if layout exists
            if (line.contains("layout"))
                binding = FileParserUtility.extractLayoutBinding(line);

            // Extract block name (text between "uniform" and "{")
            int uniformIdx = line.indexOf("uniform");
            int braceIdx = line.indexOf("{");

            if (uniformIdx != -1 && braceIdx != -1) {

                String blockName = line.substring(uniformIdx + 7, braceIdx).trim();
                // Remove any layout qualifiers that might be in the name
                blockName = blockName.replaceAll("\\(.*?\\)", "").trim();

                return new LayoutDataInstance(blockName, binding);
            }

        }

        catch (Exception e) {
            throw new GraphicException.ShaderProgramException(
                    "Failed to parse uniform block: " + line);
        }
        return null;
    }

    private void parseUniformDeclaration(
            String declaration,
            LayoutDataInstance currentLayout,
            ShaderDataInstance shaderDataInstance) {

        // Clean up declaration
        declaration = declaration.replace(";", "").trim();

        // Skip empty, closing braces, or struct definitions
        if (declaration.isEmpty() ||
                declaration.equals("}") ||
                declaration.startsWith("struct"))
            return;

        // Skip if not a uniform declaration (when outside layout blocks)
        if (currentLayout == null && !declaration.contains("uniform"))
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
        parseVariableNames(namesStr, uniformType, currentLayout, shaderDataInstance);
    }

    private void parseVariableNames(
            String namesStr,
            UniformType uniformType,
            LayoutDataInstance currentLayout,
            ShaderDataInstance shaderDataInstance) {

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
            UniformDataInstance uniform = new UniformDataInstance(uniformType, variableName, arrayCount);

            // Add to appropriate container
            if (currentLayout != null)
                currentLayout.addUniform(uniform);
            else
                shaderDataInstance.addUniform(uniform);
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
            ShaderDataInstance shaderDataInstance,
            ObjectArrayList<String> textArray) {

        for (String line : textArray) {

            if (FileParserUtility.lineStartsWith(line, "#include")) {

                String includePath = FileParserUtility.extractPayloadAfterToken(line, "#include");

                if (includePath != null && !includePath.isEmpty()) {

                    ShaderDataInstance includeShader = internalLoadManager.getShaderData(includePath);

                    if (includeShader != null)
                        shaderDataInstance.addIncludes(includeShader);
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
        ShaderDataInstance vertData = internalLoadManager.getShaderData(obj.get("vert").getAsString());
        ShaderDataInstance fragData = internalLoadManager.getShaderData(obj.get("frag").getAsString());

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

        ObjectArrayList<ShaderDataInstance> visited = new ObjectArrayList<>();

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
            ShaderDataInstance shaderData,
            ObjectArrayList<ShaderDataInstance> visited) {

        if (visited.contains(shaderData))
            return; // Cycle detected

        visited.add(shaderData);

        if (shaderData.shaderType() == ShaderType.INCLUDE &&
                !shaderDefinition.getIncludes().contains(shaderData))
            shaderDefinition.addInclude(shaderData);

        for (ShaderDataInstance include : shaderData.getIncludes())
            collectRecursiveIncludes(shaderDefinition, include, visited);
    }
}