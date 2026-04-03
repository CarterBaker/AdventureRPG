# ShaderPipelineDoc

_Generated: 2026-04-03_

## Overview
ShaderPipelineDoc covers 98 classes across 21 packages. This reference follows EngineContext naming/lifecycle conventions and summarizes class responsibilities by suffix rules.

## Integration Notes
- `core.engine` referenced via 49 imports.
- `core.kernel` referenced via 1 imports.

## Package Breakdown

### `shaderpipeline`

| Class | Role |
|---|---|
| `ShaderPipeline` | Pipeline registration entry point that wires dependency order. |

### `shaderpipeline.material`

| Class | Role |
|---|---|
| `MaterialData` | Raw data payload struct used by handles/instances. |
| `MaterialHandle` | Persistent manager-registered wrapper around data. |
| `MaterialInstance` | Runtime mutable clone of a handle. |

### `shaderpipeline.materialmanager`

| Class | Role |
|---|---|
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `MaterialManager` | Owns registration, lifecycle, and public retrieval. |

### `shaderpipeline.pass`

| Class | Role |
|---|---|
| `PassData` | Raw data payload struct used by handles/instances. |
| `PassHandle` | Persistent manager-registered wrapper around data. |
| `PassInstance` | Runtime mutable clone of a handle. |

### `shaderpipeline.passmanager`

| Class | Role |
|---|---|
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `PassManager` | Owns registration, lifecycle, and public retrieval. |

### `shaderpipeline.shader`

| Class | Role |
|---|---|
| `ShaderData` | Raw data payload struct used by handles/instances. |
| `ShaderHandle` | Persistent manager-registered wrapper around data. |
| `ShaderSourceStruct` | Lightweight struct without engine lifecycle. |
| `ShaderType` | Enum/type descriptor used for branching. |

### `shaderpipeline.shadermanager`

| Class | Role |
|---|---|
| `FileParserUtility` | Static helper utility. |
| `GLSLUtility` | Stateless OpenGL helper utility for shader-side logic. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `ShaderManager` | Owns registration, lifecycle, and public retrieval. |

### `shaderpipeline.sprite`

| Class | Role |
|---|---|
| `SpriteData` | Raw data payload struct used by handles/instances. |
| `SpriteHandle` | Persistent manager-registered wrapper around data. |
| `SpriteInstance` | Runtime mutable clone of a handle. |

### `shaderpipeline.spritemanager`

| Class | Role |
|---|---|
| `GLSLUtility` | Stateless OpenGL helper utility for shader-side logic. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `SpriteManager` | Owns registration, lifecycle, and public retrieval. |

### `shaderpipeline.texture`

| Class | Role |
|---|---|
| `TextureArrayStruct` | Lightweight struct without engine lifecycle. |
| `TextureAtlasStruct` | Lightweight struct without engine lifecycle. |
| `TextureData` | Raw data payload struct used by handles/instances. |
| `TextureHandle` | Persistent manager-registered wrapper around data. |
| `TextureTileStruct` | Lightweight struct without engine lifecycle. |

### `shaderpipeline.texturemanager`

| Class | Role |
|---|---|
| `AliasLibrarySystem` | Single-job helper system for focused tasks. |
| `AliasStruct` | Lightweight struct without engine lifecycle. |
| `GLSLUtility` | Stateless OpenGL helper utility for shader-side logic. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `TextureManager` | Owns registration, lifecycle, and public retrieval. |

### `shaderpipeline.ubo`

| Class | Role |
|---|---|
| `UBOData` | Raw data payload struct used by handles/instances. |
| `UBOHandle` | Persistent manager-registered wrapper around data. |
| `UBOInstance` | Runtime mutable clone of a handle. |

### `shaderpipeline.ubomanager`

| Class | Role |
|---|---|
| `GLSLUtility` | Stateless OpenGL helper utility for shader-side logic. |
| `InternalBuilder` | Loader-owned internal builder helper. |
| `InternalLoader` | Manager-created internal loader helper. |
| `UBOManager` | Owns registration, lifecycle, and public retrieval. |

### `shaderpipeline.uniforms`

| Class | Role |
|---|---|
| `UniformAttributeStruct` | Lightweight struct without engine lifecycle. |
| `UniformData` | Raw data payload struct used by handles/instances. |
| `UniformStruct` | Lightweight struct without engine lifecycle. |
| `UniformType` | Enum/type descriptor used for branching. |
| `UniformUtility` | Static helper utility. |

### `shaderpipeline.uniforms.matrices`

| Class | Role |
|---|---|
| `Matrix2DoubleUniform` | Specialized support class for this subsystem. |
| `Matrix2Uniform` | Specialized support class for this subsystem. |
| `Matrix3DoubleUniform` | Specialized support class for this subsystem. |
| `Matrix3Uniform` | Specialized support class for this subsystem. |
| `Matrix4DoubleUniform` | Specialized support class for this subsystem. |
| `Matrix4Uniform` | Specialized support class for this subsystem. |

### `shaderpipeline.uniforms.matrixArrays`

| Class | Role |
|---|---|
| `Matrix2ArrayUniform` | Specialized support class for this subsystem. |
| `Matrix2DoubleArrayUniform` | Specialized support class for this subsystem. |
| `Matrix3ArrayUniform` | Specialized support class for this subsystem. |
| `Matrix3DoubleArrayUniform` | Specialized support class for this subsystem. |
| `Matrix4ArrayUniform` | Specialized support class for this subsystem. |
| `Matrix4DoubleArrayUniform` | Specialized support class for this subsystem. |

### `shaderpipeline.uniforms.samplers`

| Class | Role |
|---|---|
| `SampleImage2DArrayUniform` | Specialized support class for this subsystem. |
| `SampleImage2DUniform` | Specialized support class for this subsystem. |

### `shaderpipeline.uniforms.scalarArrays`

| Class | Role |
|---|---|
| `BooleanArrayUniform` | Specialized support class for this subsystem. |
| `DoubleArrayUniform` | Specialized support class for this subsystem. |
| `FloatArrayUniform` | Specialized support class for this subsystem. |
| `IntegerArrayUniform` | Specialized support class for this subsystem. |

### `shaderpipeline.uniforms.scalars`

| Class | Role |
|---|---|
| `BooleanUniform` | Specialized support class for this subsystem. |
| `DoubleUniform` | Specialized support class for this subsystem. |
| `FloatUniform` | Specialized support class for this subsystem. |
| `IntegerUniform` | Specialized support class for this subsystem. |

### `shaderpipeline.uniforms.vectorarrays`

| Class | Role |
|---|---|
| `Vector2ArrayUniform` | Specialized support class for this subsystem. |
| `Vector2BooleanArrayUniform` | Specialized support class for this subsystem. |
| `Vector2DoubleArrayUniform` | Specialized support class for this subsystem. |
| `Vector2IntArrayUniform` | Specialized support class for this subsystem. |
| `Vector3ArrayUniform` | Specialized support class for this subsystem. |
| `Vector3BooleanArrayUniform` | Specialized support class for this subsystem. |
| `Vector3DoubleArrayUniform` | Specialized support class for this subsystem. |
| `Vector3IntArrayUniform` | Specialized support class for this subsystem. |
| `Vector4ArrayUniform` | Specialized support class for this subsystem. |
| `Vector4BooleanArrayUniform` | Specialized support class for this subsystem. |
| `Vector4DoubleArrayUniform` | Specialized support class for this subsystem. |
| `Vector4IntArrayUniform` | Specialized support class for this subsystem. |

### `shaderpipeline.uniforms.vectors`

| Class | Role |
|---|---|
| `Vector2BooleanUniform` | Specialized support class for this subsystem. |
| `Vector2DoubleUniform` | Specialized support class for this subsystem. |
| `Vector2IntUniform` | Specialized support class for this subsystem. |
| `Vector2Uniform` | Specialized support class for this subsystem. |
| `Vector3BooleanUniform` | Specialized support class for this subsystem. |
| `Vector3DoubleUniform` | Specialized support class for this subsystem. |
| `Vector3IntUniform` | Specialized support class for this subsystem. |
| `Vector3Uniform` | Specialized support class for this subsystem. |
| `Vector4BooleanUniform` | Specialized support class for this subsystem. |
| `Vector4DoubleUniform` | Specialized support class for this subsystem. |
| `Vector4IntUniform` | Specialized support class for this subsystem. |
| `Vector4Uniform` | Specialized support class for this subsystem. |

## Class Role Summary

| Class | Package | Role |
|---|---|---|
| `AliasLibrarySystem` | `program.bootstrap.shaderpipeline.texturemanager` | Single-job helper system for focused tasks. |
| `AliasStruct` | `program.bootstrap.shaderpipeline.texturemanager` | Lightweight struct without engine lifecycle. |
| `BooleanArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.scalarArrays` | Specialized support class for this subsystem. |
| `BooleanUniform` | `program.bootstrap.shaderpipeline.uniforms.scalars` | Specialized support class for this subsystem. |
| `DoubleArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.scalarArrays` | Specialized support class for this subsystem. |
| `DoubleUniform` | `program.bootstrap.shaderpipeline.uniforms.scalars` | Specialized support class for this subsystem. |
| `FileParserUtility` | `program.bootstrap.shaderpipeline.shadermanager` | Static helper utility. |
| `FloatArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.scalarArrays` | Specialized support class for this subsystem. |
| `FloatUniform` | `program.bootstrap.shaderpipeline.uniforms.scalars` | Specialized support class for this subsystem. |
| `GLSLUtility` | `program.bootstrap.shaderpipeline.shadermanager` | Stateless OpenGL helper utility for shader-side logic. |
| `GLSLUtility` | `program.bootstrap.shaderpipeline.spritemanager` | Stateless OpenGL helper utility for shader-side logic. |
| `GLSLUtility` | `program.bootstrap.shaderpipeline.texturemanager` | Stateless OpenGL helper utility for shader-side logic. |
| `GLSLUtility` | `program.bootstrap.shaderpipeline.ubomanager` | Stateless OpenGL helper utility for shader-side logic. |
| `IntegerArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.scalarArrays` | Specialized support class for this subsystem. |
| `IntegerUniform` | `program.bootstrap.shaderpipeline.uniforms.scalars` | Specialized support class for this subsystem. |
| `InternalBuilder` | `program.bootstrap.shaderpipeline.materialmanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.shaderpipeline.passmanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.shaderpipeline.shadermanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.shaderpipeline.spritemanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.shaderpipeline.texturemanager` | Loader-owned internal builder helper. |
| `InternalBuilder` | `program.bootstrap.shaderpipeline.ubomanager` | Loader-owned internal builder helper. |
| `InternalLoader` | `program.bootstrap.shaderpipeline.materialmanager` | Manager-created internal loader helper. |
| `InternalLoader` | `program.bootstrap.shaderpipeline.passmanager` | Manager-created internal loader helper. |
| `InternalLoader` | `program.bootstrap.shaderpipeline.shadermanager` | Manager-created internal loader helper. |
| `InternalLoader` | `program.bootstrap.shaderpipeline.spritemanager` | Manager-created internal loader helper. |
| `InternalLoader` | `program.bootstrap.shaderpipeline.texturemanager` | Manager-created internal loader helper. |
| `InternalLoader` | `program.bootstrap.shaderpipeline.ubomanager` | Manager-created internal loader helper. |
| `MaterialData` | `program.bootstrap.shaderpipeline.material` | Raw data payload struct used by handles/instances. |
| `MaterialHandle` | `program.bootstrap.shaderpipeline.material` | Persistent manager-registered wrapper around data. |
| `MaterialInstance` | `program.bootstrap.shaderpipeline.material` | Runtime mutable clone of a handle. |
| `MaterialManager` | `program.bootstrap.shaderpipeline.materialmanager` | Owns registration, lifecycle, and public retrieval. |
| `Matrix2ArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.matrixArrays` | Specialized support class for this subsystem. |
| `Matrix2DoubleArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.matrixArrays` | Specialized support class for this subsystem. |
| `Matrix2DoubleUniform` | `program.bootstrap.shaderpipeline.uniforms.matrices` | Specialized support class for this subsystem. |
| `Matrix2Uniform` | `program.bootstrap.shaderpipeline.uniforms.matrices` | Specialized support class for this subsystem. |
| `Matrix3ArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.matrixArrays` | Specialized support class for this subsystem. |
| `Matrix3DoubleArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.matrixArrays` | Specialized support class for this subsystem. |
| `Matrix3DoubleUniform` | `program.bootstrap.shaderpipeline.uniforms.matrices` | Specialized support class for this subsystem. |
| `Matrix3Uniform` | `program.bootstrap.shaderpipeline.uniforms.matrices` | Specialized support class for this subsystem. |
| `Matrix4ArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.matrixArrays` | Specialized support class for this subsystem. |
| `Matrix4DoubleArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.matrixArrays` | Specialized support class for this subsystem. |
| `Matrix4DoubleUniform` | `program.bootstrap.shaderpipeline.uniforms.matrices` | Specialized support class for this subsystem. |
| `Matrix4Uniform` | `program.bootstrap.shaderpipeline.uniforms.matrices` | Specialized support class for this subsystem. |
| `PassData` | `program.bootstrap.shaderpipeline.pass` | Raw data payload struct used by handles/instances. |
| `PassHandle` | `program.bootstrap.shaderpipeline.pass` | Persistent manager-registered wrapper around data. |
| `PassInstance` | `program.bootstrap.shaderpipeline.pass` | Runtime mutable clone of a handle. |
| `PassManager` | `program.bootstrap.shaderpipeline.passmanager` | Owns registration, lifecycle, and public retrieval. |
| `SampleImage2DArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.samplers` | Specialized support class for this subsystem. |
| `SampleImage2DUniform` | `program.bootstrap.shaderpipeline.uniforms.samplers` | Specialized support class for this subsystem. |
| `ShaderData` | `program.bootstrap.shaderpipeline.shader` | Raw data payload struct used by handles/instances. |
| `ShaderHandle` | `program.bootstrap.shaderpipeline.shader` | Persistent manager-registered wrapper around data. |
| `ShaderManager` | `program.bootstrap.shaderpipeline.shadermanager` | Owns registration, lifecycle, and public retrieval. |
| `ShaderPipeline` | `program.bootstrap.shaderpipeline` | Pipeline registration entry point that wires dependency order. |
| `ShaderSourceStruct` | `program.bootstrap.shaderpipeline.shader` | Lightweight struct without engine lifecycle. |
| `ShaderType` | `program.bootstrap.shaderpipeline.shader` | Enum/type descriptor used for branching. |
| `SpriteData` | `program.bootstrap.shaderpipeline.sprite` | Raw data payload struct used by handles/instances. |
| `SpriteHandle` | `program.bootstrap.shaderpipeline.sprite` | Persistent manager-registered wrapper around data. |
| `SpriteInstance` | `program.bootstrap.shaderpipeline.sprite` | Runtime mutable clone of a handle. |
| `SpriteManager` | `program.bootstrap.shaderpipeline.spritemanager` | Owns registration, lifecycle, and public retrieval. |
| `TextureArrayStruct` | `program.bootstrap.shaderpipeline.texture` | Lightweight struct without engine lifecycle. |
| `TextureAtlasStruct` | `program.bootstrap.shaderpipeline.texture` | Lightweight struct without engine lifecycle. |
| `TextureData` | `program.bootstrap.shaderpipeline.texture` | Raw data payload struct used by handles/instances. |
| `TextureHandle` | `program.bootstrap.shaderpipeline.texture` | Persistent manager-registered wrapper around data. |
| `TextureManager` | `program.bootstrap.shaderpipeline.texturemanager` | Owns registration, lifecycle, and public retrieval. |
| `TextureTileStruct` | `program.bootstrap.shaderpipeline.texture` | Lightweight struct without engine lifecycle. |
| `UBOData` | `program.bootstrap.shaderpipeline.ubo` | Raw data payload struct used by handles/instances. |
| `UBOHandle` | `program.bootstrap.shaderpipeline.ubo` | Persistent manager-registered wrapper around data. |
| `UBOInstance` | `program.bootstrap.shaderpipeline.ubo` | Runtime mutable clone of a handle. |
| `UBOManager` | `program.bootstrap.shaderpipeline.ubomanager` | Owns registration, lifecycle, and public retrieval. |
| `UniformAttributeStruct` | `program.bootstrap.shaderpipeline.uniforms` | Lightweight struct without engine lifecycle. |
| `UniformData` | `program.bootstrap.shaderpipeline.uniforms` | Raw data payload struct used by handles/instances. |
| `UniformStruct` | `program.bootstrap.shaderpipeline.uniforms` | Lightweight struct without engine lifecycle. |
| `UniformType` | `program.bootstrap.shaderpipeline.uniforms` | Enum/type descriptor used for branching. |
| `UniformUtility` | `program.bootstrap.shaderpipeline.uniforms` | Static helper utility. |
| `Vector2ArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector2BooleanArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector2BooleanUniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |
| `Vector2DoubleArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector2DoubleUniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |
| `Vector2IntArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector2IntUniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |
| `Vector2Uniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |
| `Vector3ArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector3BooleanArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector3BooleanUniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |
| `Vector3DoubleArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector3DoubleUniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |
| `Vector3IntArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector3IntUniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |
| `Vector3Uniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |
| `Vector4ArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector4BooleanArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector4BooleanUniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |
| `Vector4DoubleArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector4DoubleUniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |
| `Vector4IntArrayUniform` | `program.bootstrap.shaderpipeline.uniforms.vectorarrays` | Specialized support class for this subsystem. |
| `Vector4IntUniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |
| `Vector4Uniform` | `program.bootstrap.shaderpipeline.uniforms.vectors` | Specialized support class for this subsystem. |

## Naming Convention Reference

| Suffix | Meaning |
|---|---|
| `Data` | Raw data payload struct used by handles/instances. |
| `Handle` | Persistent manager-registered wrapper around data. |
| `Instance` | Runtime mutable clone of a handle. |
| `Manager` | Owns registration, lifecycle, and public retrieval. |
| `Loader` | Bootstrap loader that scans files and requests builds. |
| `Builder` | Bootstrap builder that parses source data into handles. |
| `Branch` | Manager-owned internal computation branch. |
| `System` | Single-job helper system for focused tasks. |
| `Struct` | Lightweight struct without engine lifecycle. |
