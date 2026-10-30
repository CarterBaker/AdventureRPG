Implement the complete texture streaming system below for AdventureRPG in this one conversation, start to finish,
without stopping between parts and without asking questions: every decision is already made below. It is a large,
cross-cutting task: read every system it touches, trace every caller, use subagents for broad searches, and follow
CLAUDE.md and every file in docs/ exactly (Engine, Style, Performance, ARPG, Build). Deliver everything as one zip,
AdventureRPG_Delivery.zip with TASK TextureStreaming, per CLAUDE.md's "Delivering Code" rules, with the full Added /
Replaced / DELETE lists. Never commit or push.

# Goal

Make 128px textures the norm, with 256 and 512 possible, at fixed GPU memory that never grows with content, with
smooth filtering (no pixel-art look), no extra draw calls and no hitches. Textures upgrade one at a time: sizes may
be mixed freely across tiles, and replacing a tile's PNGs and re-baking is all an upgrade takes.

# Current state (verify each point before relying on it)

- Texture code: core/src/application/bootstrap/shaderpipeline/texturemanager/ (TextureLoader, TextureBuilder,
  TextureManager, TextureGLSLUtility, AliasLibrarySystem) and shaderpipeline/texture/ (TextureArrayStruct,
  TextureTileStruct, TextureData, TextureHandle, ...). Atlas packing: core/src/engine/assets/atlas/AtlasUtility.
- Each pack directory under assets/textures/ (surface/standard 367 tiles, items/standard 413, characters/standard
  26; all 32px except characters at 16px) becomes one sampler2DArray. Each layer is one alias (8 aliases in
  assets/texturealiases: AO, Albedo, Emissive, Growth, Height, Metal, Normal, Specular), and each layer is a
  power-of-two square atlas of every tile. All 8 layers are uploaded as RGBA8 even when no tile provides that
  alias. Upload: TextureGLSLUtility.pushTextureArray with GL_NEAREST and no mipmaps.
- Shaders pick alias layers via u_layer_* in the SurfaceStandard UBO and tile with fract() inside the atlas
  (assets/shaders/surface/includes/TiledSampling.glsl, tileUV), using flat vUVOrigin and u_uvPerBlock.
  TextureLoader.seedUBO sets u_uvPerBlock from the FIRST tile's size, so mixed tile sizes in a pack are
  unsupported today.
- Shaders that sample u_textureArray: surface/includes (Albedo, Normal, AO, Specular, Metal, Emissive, Height,
  HeightDisplacement in the TES via texture() so always level 0, Coverage, Content, SurfaceTier*), items/*
  (standard, vehicle, equipment, inventory), trees/includes (TreeBark, TreeShading, TreeLeafSprig, which already
  uses textureGrad and is the precedent), characters/*, and the editor shaders (textureviewer, itemeditor,
  characterview). Find every other one with grep.
- Content and covering overlays (Content.glsl, Coverage.glsl, ContentBufferSystem, CoveringBufferSystem) sample
  other tiles at the same offset as the block's tile; trace how they address tiles.
- TextureManager.getTileIDFromTextureName loads the whole pack synchronously on a miss.
- Each tile already gets a baked average color and reveal (TextureBuilder), which the far tier uses beyond
  u_detailRadius; nothing beyond the radius reads textures for content or coverings.
- Terrain already streams: worldpipeline chunkstreammanager/ChunkQueueManager, macrostreammanager,
  megastreammanager (prioritized pools, per-frame budgets, unload). pbomanager exists. ProfilerGLSLUtility has
  GPU timestamp queries. The GL context is 4.1 (EngineSetting.OPENGL_VERSION_MAJOR/MINOR).
- Fonts register through TextureManager as single-layer arrays; sprites and menus use SpriteManager. Leave both on
  their current path unless sharing the new path is trivial and seamless.

# Decisions (final)

1. Baked texture data stays in .arpg files, so every data file remains an .arpg. Add a raw-bytes element to the
   ARPG format additively: a new EngineSetting.ARPG_TAG_* plus the matching ArpgValueStruct (or a dedicated
   struct) and ArpgUtility accessors, with a defined text-form representation (base64 string under a reserved
   marker) so decode/encode/export/convert/verify keep working. Do not bump ARPG_FORMAT_VERSION: every existing
   .arpg must still read, and must re-encode byte-identically; prove it by running verify over assets/ and an
   export/encode round trip on a sample of every content type. Update docs/ARPG.md for the new element, the
   baked-texture content type and the bake command.
2. Compression: BC7 (BPTC) for albedo and material, BC5 (RGTC) for normals with z rebuilt in the shader. Require
   GL 4.2 or ARB_texture_compression_bptc; check at startup and throwException with a clear message naming the
   extension if missing. macOS is not a target.
3. Sources below 128px are upscaled nearest-neighbour into the 128 tier, which keeps their exact pixel look (the
   level-two mip of a 4x nearest upscale reproduces the original). There is no 32 tier.
4. Pools and presets: 128 tier 1024 slots, 256 tier 256 slots, 512 tier 64 slots, and a 16x16 fallback array
   holding every tile in the game. Texture quality setting: Low = 128 only, Medium = 128 + 256, High = all three;
   default Medium. Anisotropy setting: off / 2x / 4x / 8x / 16x, default 8x, clamped to the driver maximum. Both
   go through the settings pipeline exactly like the existing graphics settings, with their constants in the
   right Setting class.

# Design to build

1. One texture array per map type: albedo (RGBA, alpha = cutout), normal, and material (AO, specular, metal and
   height packed into one RGBA layer). Growth and emissive go in a small optional set read only by the tiles that
   have them. One layer per tile, no atlas. All arrays of a tier share slot indices: slot N holds the same tile
   in every array, and a tile missing a map gets that map's default written into its slot. Remove the u_layer_*
   alias indices and the atlas path from TextureBuilder; keep AtlasUtility only if something else still uses it.
2. Meshes store a tile ID in place of the atlas UV origin: update the Quad Records packing in EngineSetting, the
   greedy mesher's merge key, and every vertex format and shader input that carried vUVOrigin, including the
   content, covering, item, tree and character paths. A small integer texture maps tile ID to slot per tier, read
   with texelFetch (no SSBOs on GL 4.1). Per-layer UVs run 0..1 per block whatever the tile's resolution, tiled by
   GL_REPEAT instead of fract() atlas math. Remove seedUBO's first-tile assumption.
3. Filtering: full mip chains, GL_LINEAR_MIPMAP_LINEAR, linear magnification, anisotropic filtering
   (EXT_texture_filter_anisotropic). Fragment reads use textureGrad, with gradients taken once at the top of
   main() from the continuous planar coordinate and rotated by the face spin, never inside non-uniform control
   flow. TES height reads use textureLod at the level matching the vertex spacing.
4. Fixed pools allocated once at startup from the quality setting. The shader computes the LOD it wants, reads the
   highest resident tier that serves it, and blends across tier boundaries so nothing pops; a tile with no slot
   reads the fallback array, so a texture is never missing, and the baked average color still serves the far
   tier. Query GL_MAX_ARRAY_TEXTURE_LAYERS and the fragment texture-unit limit at startup and throwException
   clearly if the pools exceed them. Changing the quality setting reallocates the pools and re-streams.
5. Streaming in the ChunkQueueManager pattern: chunks register the tile IDs their meshes use on load and release
   them on unload (reference counts); NPCs, items, furnishings, trees and the inventory UI do the same on
   spawn/despawn and open/close. Tiles at zero references stay cached until a slot is needed; eviction is least
   recently used. When every slot is in use, the farthest in-use tile gives up its slot, stepping down a tier
   before falling back. Disk reads run on the thread pipeline; uploads (glCompressedTexSubImage3D per layer and
   mip) go through reused PBO staging buffers under a per-frame budget. A texture miss queues the tile and returns
   immediately: nothing blocks the main thread. Prefetch along the player's movement direction. Bootstrap loads
   every definition, the fallback array and the averages, and fills the pools around the spawn before play.
   Zero steady-state garbage: preallocated int arrays for the free list, LRU and reference counts, no boxing, no
   iterators or lambdas in hot paths.
6. Offline bake as a tool beside Lwjgl3LauncherArpg under lwjgl3/, with a Gradle task like lwjgl3:arpg: decodes the
   PNGs, enforces power-of-two square tiles with every map of a tile the same size (upscaling smaller maps to
   match), packs channels, generates mips, compresses with a BC7 and BC5 encoder written in the engine's own code
   (no new libraries), writes the 16x16 fallback, and precomputes the average color and reveal exactly as
   TextureBuilder does today. A tile fills every tier up to its source size. It re-bakes only tiles whose sources
   changed (store a source checksum in the baked file). At runtime the game only reads baked files and uploads
   ready bytes: no ImageIO and no atlasing at load. Run the bake over all current textures and ship its output in
   the zip, sealed through ArpgUtility like every other .arpg.
7. Profiler counters: distinct tiles referenced, resident tiles per tier, evictions, fallback reads, uploads and
   upload time per frame, through the existing profiler pipeline.

Keep unchanged: chunk/macro/mega streaming, greedy meshing, the detail radius and near/far tiers, the baked average
color and reveal, the registry and name-to-ID rules (tile IDs assigned in registration order, never persisted),
eager loading of every definition, the ARPG format for definitions, tessellation, the deferred G-buffer, one draw
call per chunk, and freeing CPU data after upload.

# Verification before delivering

- Compile everything with Gradle or, if it cannot start, the direct compile in docs/Build.md. Zero errors.
- Run the bake tool over assets/textures and verify every baked file; run ArpgUtility verify over all of assets/.
- Validate every changed shader with glslangValidator if obtainable (resolve the engine's #include handling
  first); otherwise re-read each one adversarially for type errors, undeclared names, version-400 features and
  derivatives in non-uniform control flow.
- Confirm in code before relying on them: 1 block = 1 metre, the detail radius values, and every place that
  decoded vUVOrigin or u_layer_*.
- Re-read the whole diff adversarially for leftovers: no TODOs, no stubs, no dead atlas code, no debug output.
- In the reply, state memory before and after (current atlas layout versus the pools, using the real tile counts)
  and what the owner must test in game, since this session has no GPU.
