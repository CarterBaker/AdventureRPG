package com.internal.bootstrap.shaderpipeline.shadermanager;

import java.io.File;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOData;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformData;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.engine.SystemPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.JsonUtility;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Parses GLSL source files into ShaderData and assembles ShaderDefinitionData from
 * JSON descriptors. Produces UBOData with UNSPECIFIED_BINDING when no explicit
 * layout binding is present — UBOManager is the sole authority on binding assignment.
 * Include resolution uses a post-order traversal so dependencies always precede dependents.
 */
class InternalBuildSystem extends SystemPackage {

    // Internal
    private InternalLoadManager internalLoadManager;
    private File root;

    // Internal \\

    @Override
    protected void get() {
        this.internalLoadManager = get(InternalLoadManager.class);
        this.root = new File(EngineSetting.SHADER_PATH);
    }

    // Parse \\

    void parseShaderFile(ShaderData shaderData) {

        String rawText = FileParserUtility.convertFileToRawText(shaderData.getShaderFile());
        ObjectArrayList<String> lines = FileParserUtility.convertRawTextToArray(rawText);

        shaderData.setVersion(parseVersionInfo(lines));

        parseUniforms(shaderData, lines);
        parseIncludes(shaderData, lines);
    }

    // Version \\

    private String parseVersionInfo(ObjectArrayList<String> lines) {

        for (int i = 0; i < lines.size(); i++)
            if (FileParserUtility.lineStartsWith(lines.get(i), "#version"))
                return lines.get(i);

        return null;
    }

    // Uniforms \\

    private void parseUniforms(ShaderData shaderData, ObjectArrayList<String> lines) {

        boolean insideBlock = false;
        UBOData currentBuffer = null;
        int braceDepth = 0;

        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i).trim();

            if (line.isEmpty())
                continue;

            if (isUniformBlockStart(line)) {

                currentBuffer = parseUniformBlockStart(line);

                if (currentBuffer != null) {
                    insideBlock = true;
                    braceDepth = FileParserUtility.countCharInString(line, '{') -
                            FileParserUtility.countCharInString(line, '}');
                }

                continue;
            }

            if (insideBlock) {

                braceDepth += FileParserUtility.countCharInString(line, '{') -
                        FileParserUtility.countCharInString(line, '}');

                if (braceDepth <= 0) {

                    if (currentBuffer != null)
                        shaderData.addBufferBlock(currentBuffer);

                    currentBuffer = null;
                    insideBlock = false;
                    continue;
                }
            }

            if (line.contains("uniform") || (insideBlock && !line.equals("}"))) {

                String fullDeclaration = line;

                while (!fullDeclaration.endsWith(";") && i + 1 < lines.size()) {
                    i++;
                    String next = lines.get(i).trim();
                    if (!next.isEmpty())
                        fullDeclaration += " " + next;
                }

                parseUniformDeclaration(fullDeclaration, currentBuffer, shaderData);
            }
        }
    }

    private boolean isUniformBlockStart(String line) {
        return line.contains("uniform") &&
                line.contains("{") &&
                !line.contains(";") &&
                !line.matches(".*\\buniform\\s+\\w+\\s+\\w+.*");
    }

    private UBOData parseUniformBlockStart(String line) {

        try {

            // Extract explicit binding from layout qualifier, or mark as unspecified.
            // FileParserUtility.extractBufferBinding must return -1 when no binding
            // attribute is present. Binding 0 is a valid explicit value.
            int binding = UBOData.UNSPECIFIED_BINDING;

            if (line.contains("layout") || line.contains("buffer"))
                binding = FileParserUtility.extractBufferBinding(line);

            int uniformIdx = line.indexOf("uniform");
            int braceIdx = line.indexOf("{");

            if (uniformIdx == -1 || braceIdx == -1)
                return null;

            String blockName = line.substring(uniformIdx + 7, braceIdx).trim();
            blockName = blockName.replaceAll("\\(.*?\\)", "").trim();

            UBOData uboData = create(UBOData.class);
            uboData.constructor(blockName, binding);

            return uboData;

        } catch (InternalException e) {
            throw e;
        } catch (Exception e) {
            return throwException("Failed to parse uniform block: " + line, e);
        }
    }

    private void parseUniformDeclaration(
            String declaration,
            UBOData currentBuffer,
            ShaderData shaderData) {

        declaration = declaration.replace(";", "").trim();

        if (declaration.isEmpty() ||
                declaration.equals("}") ||
                declaration.startsWith("struct"))
            return;

        if (currentBuffer == null && !declaration.contains("uniform"))
            return;

        declaration = declaration.replaceFirst("\\buniform\\b", "").trim();

        int lastDelimiter = FileParserUtility.findLastTypeDelimiter(declaration);
        if (lastDelimiter == -1)
            return;

        String typeStr = declaration.substring(0, lastDelimiter).trim();
        String namesStr = declaration.substring(lastDelimiter).trim();

        UniformType uniformType = UniformType.fromString(typeStr);
        if (uniformType == null)
            return;

        parseVariableNames(namesStr, uniformType, currentBuffer, shaderData);
    }

    private void parseVariableNames(
            String namesStr,
            UniformType uniformType,
            UBOData currentBuffer,
            ShaderData shaderData) {

        String[] names = namesStr.split(",");

        for (int i = 0; i < names.length; i++) {

            String name = names[i].trim();
            if (name.isEmpty())
                continue;

            int arrayCount = 1;
            String variableName = name;

            if (name.contains("[")) {

                int bracketStart = name.indexOf("[");
                int bracketEnd = name.indexOf("]");

                if (bracketStart != -1 && bracketEnd > bracketStart) {

                    String countStr = name.substring(bracketStart + 1, bracketEnd).trim();

                    if (countStr.isEmpty())
                        throwException("Unsized arrays not supported: " + name);

                    arrayCount = FileParserUtility.parseIntOrDefault(countStr, 0);

                    if (arrayCount == 0)
                        throwException("Could not resolve array size for: " + name);

                    variableName = name.substring(0, bracketStart).trim();
                }
            }

            UniformData uniform = create(UniformData.class);
            uniform.constructor(uniformType, variableName, arrayCount);

            if (currentBuffer != null)
                currentBuffer.addUniform(uniform);
            else
                shaderData.addUniform(uniform);
        }
    }

    // Includes \\

    private void parseIncludes(ShaderData shaderData, ObjectArrayList<String> lines) {

        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i);

            if (FileParserUtility.lineStartsWith(line, "#include")) {

                String includePath = FileParserUtility.extractPayloadAfterToken(line, "#include");

                if (includePath != null && !includePath.isEmpty()) {

                    ShaderData includeData = internalLoadManager.getShaderData(includePath);

                    if (includeData != null)
                        shaderData.addIncludes(includeData);
                }
            }
        }
    }

    // Compile \\

    ShaderDefinitionData compileShader(File jsonFile) {

        Path rootPath = root.toPath();
        String relativePath = rootPath.relativize(jsonFile.toPath()).toString().replace("\\", "/");
        String shaderName = relativePath.endsWith(".json")
                ? relativePath.substring(0, relativePath.length() - 5)
                : relativePath;

        JsonObject obj = JsonUtility.loadJsonObject(jsonFile);
        ShaderData vertData = internalLoadManager.getShaderData(obj.get("vert").getAsString());
        ShaderData fragData = internalLoadManager.getShaderData(obj.get("frag").getAsString());

        if (vertData == null || fragData == null)
            throwException("Json error: " + jsonFile.getName() + " — vert or frag file not found.");

        if (vertData.getShaderType() != ShaderType.VERT || fragData.getShaderType() != ShaderType.FRAG)
            throwException("Json error: " + jsonFile.getName() + " — vert/frag type mismatch.");

        ShaderDefinitionData definition = create(ShaderDefinitionData.class);
        definition.constructor(shaderName, vertData, fragData);

        return collectIncludes(definition);
    }

    /*
     * Post-order traversal: a dependency is added to the flat include list only
     * after
     * all of its own dependencies have been added. This guarantees that if include
     * A
     * depends on include B, B will always appear before A in the final source.
     */
    private ShaderDefinitionData collectIncludes(ShaderDefinitionData definition) {

        ObjectArrayList<ShaderData> visited = new ObjectArrayList<>();

        collectPostOrder(definition, definition.getVert(), visited);
        collectPostOrder(definition, definition.getFrag(), visited);

        return definition;
    }

    private void collectPostOrder(
            ShaderDefinitionData definition,
            ShaderData shaderData,
            ObjectArrayList<ShaderData> visited) {

        if (visited.contains(shaderData))
            return;

        visited.add(shaderData);

        ObjectArrayList<ShaderData> includes = shaderData.getIncludes();

        // Recurse into dependencies before adding this node
        for (int i = 0; i < includes.size(); i++)
            collectPostOrder(definition, includes.get(i), visited);

        if (shaderData.getShaderType() == ShaderType.INCLUDE &&
                !definition.getIncludes().contains(shaderData))
            definition.addInclude(shaderData);
    }
}