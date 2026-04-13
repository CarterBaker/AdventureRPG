# MenuPipelineDoc

This is a human-readable subsystem manual. It explains responsibilities, collaboration points, and method intent for each class in scope.

Classes covered: **35**

## How to read this manual
- Start with the package flow notes at the top of each class section.
- Use **Who talks to this class** to identify collaborators.
- Use **Method intent** to understand lifecycle and API behavior.

## `core/src/program/bootstrap/menupipeline/MenuPipeline.java`

**Type:** `class MenuPipeline`
  
**Inheritance/implements:** `extends PipelinePackage`
  
**Package:** `program.bootstrap.menupipeline`
  
**File size:** 25 lines

**What this class does:** `MenuPipeline` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.fontmanager.FontManager`
- `program.bootstrap.menupipeline.menueventsmanager.MenuEventsManager`
- `program.bootstrap.menupipeline.menumanager.MenuManager`
- `program.bootstrap.menupipeline.raycastsystem.RaycastSystem`
- `program.core.engine.PipelinePackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementData.java`

**Type:** `class ElementData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 128 lines

**What this class does:** `ElementData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.util.DimensionValue`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.menupipeline.util.StackDirection`
- `program.bootstrap.menupipeline.util.TextAlign`
- `program.core.engine.DataPackage`

**Method intent:**
- `package public ElementData(String id, ElementType type, String spriteName, String text, String fontName, float[] color, LayoutStruct layout, boolean mask, StackDirection stackDirection, DimensionValue spacing, TextAlign textAlign)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getId()` — Returns current state/value.
- `public ElementType getType()` — Returns current state/value.
- `public String getSpriteName()` — Returns current state/value.
- `public String getText()` — Returns current state/value.
- `public String getFontName()` — Returns current state/value.
- `public float[] getColor()` — Returns current state/value.
- `public LayoutStruct getLayout()` — Returns current state/value.
- `public boolean isMask()` — Performs class-specific logic; see call sites and owning manager flow.
- `public StackDirection getStackDirection()` — Returns current state/value.
- `public DimensionValue getSpacing()` — Returns current state/value.
- `public TextAlign getTextAlign()` — Returns current state/value.
- `public boolean hasSprite()` — Boolean existence/availability check.
- `public boolean hasText()` — Boolean existence/availability check.
- `public boolean hasFont()` — Boolean existence/availability check.
- `public boolean hasColor()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementHandle.java`

**Type:** `class ElementHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 129 lines

**What this class does:** `ElementHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.util.DimensionValue`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.menupipeline.util.MenuAwareAction`
- `program.bootstrap.menupipeline.util.StackDirection`
- `program.bootstrap.menupipeline.util.TextAlign`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(ElementData data, Runnable clickAction, MenuAwareAction menuAwareAction, ObjectArrayList<ElementPlacementStruct> children)` — Engine-side initialization entrypoint invoked post-create.
- `public ElementData getElementData()` — Returns current state/value.
- `public String getId()` — Returns current state/value.
- `public ElementType getType()` — Returns current state/value.
- `public String getSpriteName()` — Returns current state/value.
- `public String getText()` — Returns current state/value.
- `public String getFontName()` — Returns current state/value.
- `public float[] getColor()` — Returns current state/value.
- `public LayoutStruct getLayout()` — Returns current state/value.
- `public boolean isMask()` — Performs class-specific logic; see call sites and owning manager flow.
- `public StackDirection getStackDirection()` — Returns current state/value.
- `public DimensionValue getSpacing()` — Returns current state/value.
- `public TextAlign getTextAlign()` — Returns current state/value.
- `public Runnable getClickAction()` — Returns current state/value.
- `public MenuAwareAction getMenuAwareAction()` — Returns current state/value.
- `public ObjectArrayList<ElementPlacementStruct> getChildren()` — Returns current state/value.
- `public boolean hasSprite()` — Boolean existence/availability check.
- `public boolean hasText()` — Boolean existence/availability check.
- `public boolean hasFont()` — Boolean existence/availability check.
- `public boolean hasColor()` — Boolean existence/availability check.
- `public boolean hasClickAction()` — Boolean existence/availability check.
- `public boolean hasMenuAwareAction()` — Boolean existence/availability check.
- `public boolean hasChildren()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementInstance.java`

**Type:** `class ElementInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 297 lines

**What this class does:** `ElementInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.fonts.FontInstance`
- `program.bootstrap.menupipeline.util.DimensionVector2`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.shaderpipeline.sprite.SpriteInstance`
- `program.core.engine.InstancePackage`
- `program.core.util.mathematics.matrices.Matrix4`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `public void constructor(ElementData data, SpriteInstance spriteInstance, FontInstance fontInstance, String textOverride, Runnable resolvedAction, LayoutStruct layoutOverride, ObjectArrayList<ElementInstance> children)` — Engine-side initialization entrypoint invoked post-create.
- `public void computeTransform(float parentLeft, float parentTop, float parentW, float parentH)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void computeStackedTransform(float left, float top, float parentW, float parentH)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void execute()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void addChild(ElementInstance child)` — Registers a child object into manager-owned collections.
- `public void removeChild(ElementInstance child)` — Unregisters and tears down child references.
- `public ElementInstance findChildById(String id)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setScrollX(float x)` — Mutates internal state for this object.
- `public void setScrollY(float y)` — Mutates internal state for this object.
- `public float getScrollX()` — Returns current state/value.
- `public float getScrollY()` — Returns current state/value.
- `public float getMaxScrollX()` — Returns current state/value.
- `public float getMaxScrollY()` — Returns current state/value.
- `public void setContentW(float w)` — Mutates internal state for this object.
- `public void setContentH(float h)` — Mutates internal state for this object.
- `public float getContentW()` — Returns current state/value.
- `public float getContentH()` — Returns current state/value.
- `public void setPositionOverride(DimensionVector2 pos)` — Mutates internal state for this object.
- `public void clearPositionOverride()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ElementData getElementData()` — Returns current state/value.
- `public SpriteInstance getSpriteInstance()` — Returns current state/value.
- `public FontInstance getFontInstance()` — Returns current state/value.
- `public ObjectArrayList<ElementInstance> getChildren()` — Returns current state/value.
- `public Matrix4 getTransform()` — Returns current state/value.
- `public float getComputedLeft()` — Returns current state/value.
- `public float getComputedTop()` — Returns current state/value.
- `public float getComputedW()` — Returns current state/value.
- `public float getComputedH()` — Returns current state/value.
- `public boolean hasSprite()` — Boolean existence/availability check.
- `public boolean hasFont()` — Boolean existence/availability check.
- `public boolean hasChildren()` — Boolean existence/availability check.
- `public String getText()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementOrigin.java`

**Type:** `enum ElementOrigin`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 52 lines

**What this class does:** `ElementOrigin` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Who this class talks to (direct imports):**
- `program.core.engine.EngineUtility`

**Method intent:**
- `package  ElementOrigin(float x, float y)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ElementOrigin fromString(String name)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/FileParserUtility.java`, `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`.
- `public float getX()` — Returns current state/value.
- `public float getY()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementPlacementStruct.java`

**Type:** `class ElementPlacementStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 99 lines

**What this class does:** `ElementPlacementStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.menupipeline.util.MenuAwareAction`
- `program.core.engine.StructPackage`

**Method intent:**
- `package public ElementPlacementStruct(ElementHandle master)` — Performs class-specific logic; see call sites and owning manager flow.
- `package public ElementPlacementStruct(ElementHandle master, String spriteNameOverride, String textOverride, float[] colorOverride, Runnable clickActionOverride, MenuAwareAction menuAwareActionOverride, LayoutStruct layoutOverride)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setMaster(ElementHandle master)` — Mutates internal state for this object.
- `public ElementHandle getMaster()` — Returns current state/value.
- `public String getSpriteNameOverride()` — Returns current state/value.
- `public String getTextOverride()` — Returns current state/value.
- `public float[] getColorOverride()` — Returns current state/value.
- `public Runnable getClickActionOverride()` — Returns current state/value.
- `public MenuAwareAction getMenuAwareActionOverride()` — Returns current state/value.
- `public LayoutStruct getLayoutOverride()` — Returns current state/value.
- `public boolean hasColorOverride()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/element/ElementType.java`

**Type:** `enum ElementType`
  
**Package:** `program.bootstrap.menupipeline.element`
  
**File size:** 15 lines

**What this class does:** `ElementType` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.element`.

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fontmanager/FontManager.java`

**Type:** `class FontManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.menupipeline.fontmanager`
  
**File size:** 116 lines

**What this class does:** `FontManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fontmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.menupipeline.fonts.FontHandle`
- `program.bootstrap.menupipeline.fonts.FontInstance`
- `program.core.engine.ManagerPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void dispose()` — Releases owned resources and unregisters state.
- `package void addFont(String fontName, FontHandle fontHandle)` — Registers a child object into manager-owned collections.
- `public boolean hasFont(String fontName)` — Boolean existence/availability check.
- `public int getFontIDFromFontName(String fontName)` — Returns current state/value.
- `public FontHandle getFontHandleFromFontID(int fontID)` — Returns current state/value.
- `public FontHandle getFontHandleFromFontName(String fontName)` — Returns current state/value.
- `public FontInstance cloneFont(String fontName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void request(String fontName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fontmanager/FontRasterizerUtility.java`

**Type:** `class FontRasterizerUtility`
  
**Inheritance/implements:** `extends EngineUtility`
  
**Package:** `program.bootstrap.menupipeline.fontmanager`
  
**File size:** 75 lines

**What this class does:** `FontRasterizerUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fontmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.fonts.FontTileData`
- `program.core.engine.EngineUtility`

**Method intent:**
- `package ObjectArrayList<FontTileData> rasterize(File fontFile, int size, String charset, InternalBuilder builder)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/fontmanager/InternalBuilder.java`.
- `private Font loadFont(File fontFile, int size)` — Parses external data into engine objects.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fontmanager/GLSLUtility.java`

**Type:** `class GLSLUtility`
  
**Inheritance/implements:** `extends EngineUtility`
  
**Package:** `program.bootstrap.menupipeline.fontmanager`
  
**File size:** 68 lines

**What this class does:** `GLSLUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fontmanager`.

**Who this class talks to (direct imports):**
- `program.core.app.EngineContext`
- `program.core.util.graphics.gl.GL20`
- `program.core.util.image.Pixmap`
- `program.core.engine.EngineUtility`
- `program.core.util.PixmapUtility`

**Method intent:**
- `package int pushTexture2D(BufferedImage image)` — Queues data for downstream systems (often render queues). Called via static reference from: `core/src/program/bootstrap/menupipeline/fontmanager/InternalBuilder.java`.
- `package void deleteTexture2D(int handle)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/fontmanager/FontManager.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fontmanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.menupipeline.fontmanager`
  
**File size:** 158 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fontmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicGeometryManager`
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.vao.VAOHandle`
- `program.bootstrap.geometrypipeline.vaomanager.VAOManager`
- `program.bootstrap.menupipeline.fonts.FontHandle`
- `program.bootstrap.menupipeline.fonts.FontTileData`
- `program.bootstrap.menupipeline.fonts.GlyphMetricStruct`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.AtlasUtility`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package FontHandle build(String name, File fontFile)` — Constructs derived runtime/handle data from source input.
- `private BufferedImage compositeAtlas(ObjectArrayList<FontTileData> tiles, int atlasPixelSize)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Int2ObjectOpenHashMap<GlyphMetricStruct> buildGlyphTable(ObjectArrayList<FontTileData> tiles, int atlasPixelSize)` — Constructs derived runtime/handle data from source input.
- `private Int2ObjectOpenHashMap<DynamicModelHandle> buildGlyphModels(Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs, int materialID, VAOHandle vaoHandle, int atlasPixelSize)` — Constructs derived runtime/handle data from source input.
- `package FontTileData createFontTile()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fontmanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.menupipeline.fontmanager`
  
**File size:** 91 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fontmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.fonts.FontHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void load(File file)` — Parses external data into engine objects.
- `package void request(String fontName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fonts/FontHandle.java`

**Type:** `class FontHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.menupipeline.fonts`
  
**File size:** 86 lines

**What this class does:** `FontHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fonts`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(String name, int gpuHandle, int materialID, int atlasPixelSize, Int2ObjectOpenHashMap<GlyphMetricStruct> glyphs, Int2ObjectOpenHashMap<DynamicModelHandle> glyphModels)` — Engine-side initialization entrypoint invoked post-create.
- `public String getName()` — Returns current state/value.
- `public int getGPUHandle()` — Returns current state/value.
- `public int getMaterialID()` — Returns current state/value.
- `public int getAtlasPixelSize()` — Returns current state/value.
- `public boolean hasGlyph(int codepoint)` — Boolean existence/availability check.
- `public GlyphMetricStruct getGlyph(int codepoint)` — Returns current state/value.
- `public DynamicModelHandle getGlyphModel(int codepoint)` — Returns current state/value.
- `public Int2ObjectOpenHashMap<GlyphMetricStruct> getGlyphs()` — Returns current state/value.
- `public Int2ObjectOpenHashMap<DynamicModelHandle> getGlyphModels()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fonts/FontInstance.java`

**Type:** `class FontInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.menupipeline.fonts`
  
**File size:** 171 lines

**What this class does:** `FontInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fonts`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle`
- `program.bootstrap.geometrypipeline.model.ModelInstance`
- `program.bootstrap.shaderpipeline.material.MaterialInstance`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.core.engine.InstancePackage`
- `program.core.settings.EngineSetting`
- `program.core.util.mathematics.vectors.Vector4`

**Method intent:**
- `public void constructor(FontHandle handle, DynamicModelHandle mergedModel)` — Engine-side initialization entrypoint invoked post-create.
- `public void setText(String text)` — Mutates internal state for this object.
- `public void setColor(float r, float g, float b, float a)` — Mutates internal state for this object.
- `public Vector4 getColor()` — Returns current state/value.
- `public void upload(ModelManager modelManager, MaterialManager materialManager)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void release(ModelManager modelManager)` — Performs class-specific logic; see call sites and owning manager flow.
- `public FontHandle getHandle()` — Returns current state/value.
- `public DynamicModelHandle getMergedModel()` — Returns current state/value.
- `public ModelInstance getModelInstance()` — Returns current state/value.
- `public float getTextWidth()` — Returns current state/value.
- `public float getTextHeight()` — Returns current state/value.
- `public boolean hasModel()` — Boolean existence/availability check.
- `public boolean isDirty()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fonts/FontTileData.java`

**Type:** `class FontTileData`
  
**Inheritance/implements:** `extends AtlasTileData`
  
**Package:** `program.bootstrap.menupipeline.fonts`
  
**File size:** 72 lines

**What this class does:** `FontTileData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fonts`.

**Who this class talks to (direct imports):**
- `program.core.util.atlas.AtlasTileData`

**Method intent:**
- `public void constructor(int codepoint, BufferedImage image, int bearingX, int bearingY, int advance)` — Engine-side initialization entrypoint invoked post-create.
- `public void clearImage()` — Performs class-specific logic; see call sites and owning manager flow.
- `public int getCodepoint()` — Returns current state/value.
- `public BufferedImage getImage()` — Returns current state/value.
- `public int getBearingX()` — Returns current state/value.
- `public int getBearingY()` — Returns current state/value.
- `public int getAdvance()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/fonts/GlyphMetricStruct.java`

**Type:** `class GlyphMetricStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.menupipeline.fonts`
  
**File size:** 49 lines

**What this class does:** `GlyphMetricStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.fonts`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public GlyphMetricStruct(int atlasX, int atlasY, int width, int height, int bearingX, int bearingY, int advance)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menu/MenuData.java`

**Type:** `class MenuData`
  
**Inheritance/implements:** `extends DataPackage`
  
**Package:** `program.bootstrap.menupipeline.menu`
  
**File size:** 55 lines

**What this class does:** `MenuData` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menu`.

**Who this class talks to (direct imports):**
- `program.core.engine.DataPackage`

**Method intent:**
- `package public MenuData(String name, boolean lockInput, boolean raycastInput, ObjectArrayList<String> entryPoints)` — Performs class-specific logic; see call sites and owning manager flow.
- `public String getName()` — Returns current state/value.
- `public boolean isLockInput()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isRaycastInput()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<String> getEntryPoints()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menu/MenuHandle.java`

**Type:** `class MenuHandle`
  
**Inheritance/implements:** `extends HandlePackage`
  
**Package:** `program.bootstrap.menupipeline.menu`
  
**File size:** 55 lines

**What this class does:** `MenuHandle` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menu`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.element.ElementPlacementStruct`
- `program.core.engine.HandlePackage`

**Method intent:**
- `public void constructor(MenuData data, ObjectArrayList<ElementPlacementStruct> placements)` — Engine-side initialization entrypoint invoked post-create.
- `public MenuData getMenuData()` — Returns current state/value.
- `public String getName()` — Returns current state/value.
- `public boolean isLockInput()` — Performs class-specific logic; see call sites and owning manager flow.
- `public boolean isRaycastInput()` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<String> getEntryPoints()` — Returns current state/value.
- `public ObjectArrayList<ElementPlacementStruct> getPlacements()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menu/MenuInstance.java`

**Type:** `class MenuInstance`
  
**Inheritance/implements:** `extends InstancePackage`
  
**Package:** `program.bootstrap.menupipeline.menu`
  
**File size:** 117 lines

**What this class does:** `MenuInstance` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menu`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.element.ElementInstance`
- `program.core.engine.InstancePackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `public void constructor(MenuData data, ObjectArrayList<ElementInstance> elements, WindowInstance window)` — Engine-side initialization entrypoint invoked post-create.
- `public ElementInstance getEntryPoint(int index)` — Returns current state/value.
- `public void addToEntryPoint(int index, ElementInstance element)` — Registers a child object into manager-owned collections.
- `public void removeFromEntryPoint(int index, ElementInstance element)` — Unregisters and tears down child references.
- `private ElementInstance findById(ObjectArrayList<ElementInstance> list, String id)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void show()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void hide()` — Performs class-specific logic; see call sites and owning manager flow.
- `public MenuData getMenuData()` — Returns current state/value.
- `public ObjectArrayList<ElementInstance> getElements()` — Returns current state/value.
- `public WindowInstance getWindow()` — Returns current state/value.
- `public int getWindowID()` — Returns current state/value.
- `public boolean isVisible()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menueventsmanager/MenuEventsManager.java`

**Type:** `class MenuEventsManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.menupipeline.menueventsmanager`
  
**File size:** 26 lines

**What this class does:** `MenuEventsManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menueventsmanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.menueventsmanager.menus.InventoryBranch`
- `program.bootstrap.menupipeline.menueventsmanager.menus.MainMenuBranch`
- `program.bootstrap.menupipeline.menueventsmanager.util.GenericButtonBranch`
- `program.core.engine.ManagerPackage`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menueventsmanager/menus/InventoryBranch.java`

**Type:** `class InventoryBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.menupipeline.menueventsmanager.menus`
  
**File size:** 104 lines

**What this class does:** `InventoryBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menueventsmanager.menus`.

**Who this class talks to (direct imports):**
- `program.bootstrap.entitypipeline.entity.EntityInstance`
- `program.bootstrap.entitypipeline.inventory.InventoryHandle`
- `program.bootstrap.menupipeline.element.ElementInstance`
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.bootstrap.menupipeline.menumanager.MenuManager`
- `program.core.engine.BranchPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void openInventory(EntityInstance entity, WindowInstance window)` — Activates UI/window/menu surface.
- `public void closeInventory()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void toggleInventory(EntityInstance entity, WindowInstance window)` — Switches state between active/inactive variants.
- `public boolean isOpen()` — Performs class-specific logic; see call sites and owning manager flow.
- `public void rebuildUI(InventoryHandle inventory)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void injectSlot(String displayName)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void selectItem()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menueventsmanager/menus/MainMenuBranch.java`

**Type:** `class MainMenuBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.menupipeline.menueventsmanager.menus`
  
**File size:** 46 lines

**What this class does:** `MainMenuBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menueventsmanager.menus`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.bootstrap.menupipeline.menumanager.MenuManager`
- `program.core.engine.BranchPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public MenuInstance openMenu(WindowInstance window)` — Activates UI/window/menu surface.
- `public MenuInstance closeMenu()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menueventsmanager/util/GenericButtonBranch.java`

**Type:** `class GenericButtonBranch`
  
**Inheritance/implements:** `extends BranchPackage`
  
**Package:** `program.bootstrap.menupipeline.menueventsmanager.util`
  
**File size:** 15 lines

**What this class does:** `GenericButtonBranch` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menueventsmanager.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.BranchPackage`

**Method intent:**
- `public void quitGame()` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menumanager/ElementSystem.java`

**Type:** `class ElementSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.menupipeline.menumanager`
  
**File size:** 202 lines

**What this class does:** `ElementSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menumanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.element.ElementData`
- `program.bootstrap.menupipeline.element.ElementHandle`
- `program.bootstrap.menupipeline.element.ElementInstance`
- `program.bootstrap.menupipeline.element.ElementPlacementStruct`
- `program.bootstrap.menupipeline.fontmanager.FontManager`
- `program.bootstrap.menupipeline.fonts.FontInstance`
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.bootstrap.menupipeline.util.MenuAwareAction`
- `program.bootstrap.shaderpipeline.sprite.SpriteInstance`
- `program.bootstrap.shaderpipeline.spritemanager.SpriteManager`
- `program.core.engine.SystemPackage`
- `program.core.util.RegistryUtility`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `package boolean hasMaster(String key)` — Boolean existence/availability check.
- `package ElementHandle getMaster(String key)` — Returns current state/value.
- `package void registerMaster(String key, ElementHandle handle)` — Performs class-specific logic; see call sites and owning manager flow.
- `package Iterable<String> getMasterKeys()` — Returns current state/value.
- `package boolean isFileLoading(String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void beginFileLoad(String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void endFileLoad(String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `package ObjectArrayList<ElementInstance> createInstances(ObjectArrayList<ElementPlacementStruct> placements, Supplier<MenuInstance> parentRef)` — Allocates/initializes child systems or resources.
- `private ElementInstance createInstance(ElementPlacementStruct placement, Supplier<MenuInstance> parentRef)` — Allocates/initializes child systems or resources.
- `private Runnable resolveAction(ElementHandle master, ElementPlacementStruct placement, Supplier<MenuInstance> parentRef)` — Performs class-specific logic; see call sites and owning manager flow.
- `package ElementInstance createDetachedInstance(ElementPlacementStruct placement)` — Allocates/initializes child systems or resources.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menumanager/FileParserUtility.java`

**Type:** `class FileParserUtility`
  
**Inheritance/implements:** `extends EngineUtility`
  
**Package:** `program.bootstrap.menupipeline.menumanager`
  
**File size:** 107 lines

**What this class does:** `FileParserUtility` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menumanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.element.ElementOrigin`
- `program.bootstrap.menupipeline.element.ElementType`
- `program.bootstrap.menupipeline.util.DimensionVector2`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.core.engine.EngineUtility`
- `program.core.util.JsonUtility`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `package String[] parseOnClick(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `package ElementType parseElementType(String type, String id)` — Performs class-specific logic; see call sites and owning manager flow.
- `package LayoutStruct parseLayout(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `package LayoutStruct parseLayoutOverride(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `package Vector2 parseOriginField(JsonObject json, String key)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`

**Type:** `class InternalBuilder`
  
**Inheritance/implements:** `extends BuilderPackage`
  
**Package:** `program.bootstrap.menupipeline.menumanager`
  
**File size:** 646 lines

**What this class does:** `InternalBuilder` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menumanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.element.ElementData`
- `program.bootstrap.menupipeline.element.ElementHandle`
- `program.bootstrap.menupipeline.element.ElementOrigin`
- `program.bootstrap.menupipeline.element.ElementPlacementStruct`
- `program.bootstrap.menupipeline.element.ElementType`
- `program.bootstrap.menupipeline.menu.MenuData`
- `program.bootstrap.menupipeline.menu.MenuHandle`
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.bootstrap.menupipeline.util.DimensionValue`
- `program.bootstrap.menupipeline.util.DimensionVector2`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.menupipeline.util.MenuAwareAction`
- `program.bootstrap.menupipeline.util.StackDirection`
- `program.bootstrap.menupipeline.util.TextAlign`
- `program.bootstrap.shaderpipeline.spritemanager.SpriteManager`
- `program.core.engine.BuilderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.JsonUtility`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `package void init(File root)` — Performs class-specific logic; see call sites and owning manager flow.
- `package ObjectArrayList<MenuHandle> processFile(File file, String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void resolveAllDeferredRefs()` — Performs class-specific logic; see call sites and owning manager flow.
- `private MenuHandle buildMenuHandle(String fileName, String filePath, JsonObject menuJson)` — Constructs derived runtime/handle data from source input.
- `private void registerTopLevelMasters(String filePath, JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private ObjectArrayList<ElementPlacementStruct> buildPlacements(String filePath, JsonObject parent)` — Constructs derived runtime/handle data from source input.
- `private ElementPlacementStruct buildPlacement(String filePath, JsonObject json)` — Constructs derived runtime/handle data from source input.
- `private ElementPlacementStruct buildInlinePlacement(String filePath, String id, JsonObject json)` — Constructs derived runtime/handle data from source input.
- `private ElementPlacementStruct buildUsePlacement(String filePath, String id, JsonObject json)` — Constructs derived runtime/handle data from source input.
- `private ElementPlacementStruct buildRefPlacement(String filePath, String id, JsonObject json)` — Constructs derived runtime/handle data from source input.
- `private ElementHandle buildMasterFromJson(String filePath, String id, JsonObject json)` — Constructs derived runtime/handle data from source input.
- `private ElementHandle resolveRefKey(String refKey)` — Performs class-specific logic; see call sites and owning manager flow.
- `private ElementHandle resolveTemplate(String usePath, String localId)` — Performs class-specific logic; see call sites and owning manager flow.
- `private File tryResolveFile(String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `private File resolveFile(String filePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String resolveSpriteName(String elementId, String spritePath)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Object resolveClickActionRaw(String actionClass, String actionMethod, String actionArg)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Object resolveTarget(String className, String methodName)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Method resolveMethod(Object target, String className, String methodName, String arg)` — Performs class-specific logic; see call sites and owning manager flow.
- `private LayoutStruct parseLayout(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private LayoutStruct parseLayoutOverride(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private Vector2 parseOriginField(JsonObject json, String key)` — Performs class-specific logic; see call sites and owning manager flow.
- `private ElementType parseElementType(String type, String id)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String[] parseOnClick(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.
- `private String parseFontName(JsonObject json, ElementType type, String elementId)` — Performs class-specific logic; see call sites and owning manager flow.
- `private float[] parseColor(JsonObject json)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menumanager/InternalLoader.java`

**Type:** `class InternalLoader`
  
**Inheritance/implements:** `extends LoaderPackage`
  
**Package:** `program.bootstrap.menupipeline.menumanager`
  
**File size:** 120 lines

**What this class does:** `InternalLoader` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menumanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.menu.MenuHandle`
- `program.core.engine.LoaderPackage`
- `program.core.settings.EngineSetting`
- `program.core.util.FileUtility`

**Method intent:**
- `protected void scan()` — Discovers files/resources for later load.
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void awake()` — Runs startup-time runtime activation work.
- `protected void load(File file)` — Parses external data into engine objects.
- `protected void onComplete()` — Performs class-specific logic; see call sites and owning manager flow.
- `package void request(String menuName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/menumanager/MenuManager.java`

**Type:** `class MenuManager`
  
**Inheritance/implements:** `extends ManagerPackage`
  
**Package:** `program.bootstrap.menupipeline.menumanager`
  
**File size:** 598 lines

**What this class does:** `MenuManager` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.menumanager`.

**Who this class talks to (direct imports):**
- `program.bootstrap.geometrypipeline.modelmanager.ModelManager`
- `program.bootstrap.inputpipeline.inputsystem.InputSystem`
- `program.bootstrap.menupipeline.element.ElementData`
- `program.bootstrap.menupipeline.element.ElementHandle`
- `program.bootstrap.menupipeline.element.ElementInstance`
- `program.bootstrap.menupipeline.element.ElementPlacementStruct`
- `program.bootstrap.menupipeline.element.ElementType`
- `program.bootstrap.menupipeline.fonts.FontInstance`
- `program.bootstrap.menupipeline.menu.MenuData`
- `program.bootstrap.menupipeline.menu.MenuHandle`
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.bootstrap.menupipeline.util.LayoutStruct`
- `program.bootstrap.menupipeline.util.StackDirection`
- `program.bootstrap.physicspipeline.raycastmanager.RaycastManager`
- `program.bootstrap.physicspipeline.util.ScreenRayStruct`
- `program.bootstrap.renderpipeline.rendermanager.RenderManager`
- `program.bootstrap.renderpipeline.util.MaskStruct`
- `program.bootstrap.shaderpipeline.materialmanager.MaterialManager`
- `program.core.engine.ManagerPackage`
- `program.core.kernel.window.WindowInstance`

**Method intent:**
- `protected void create()` — Allocates/initializes child systems or resources.
- `protected void get()` — Returns current state/value.
- `protected void update()` — Runs frame-step maintenance and logic.
- `private void updateRaycast()` — Runs frame-step maintenance and logic.
- `private boolean hitTestElements(ObjectArrayList<ElementInstance> elements, float mouseX, float mouseY, float clipLeft, float clipTop, float clipRight, float clipBottom)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean isHit(ElementInstance element, float mouseX, float mouseY)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderElement(ElementInstance element, float parentLeft, float parentTop, float parentW, float parentH)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderStackedElement(ElementInstance element, float left, float top, float parentW, float parentH)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderElementContent(ElementInstance element)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void renderStacked(ElementInstance parent, StackDirection dir)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void pushSpriteRenderCall(ElementInstance element)` — Queues data for downstream systems (often render queues).
- `private void pushFontRenderCall(ElementInstance element)` — Queues data for downstream systems (often render queues).
- `private void pushMask(ElementInstance element)` — Queues data for downstream systems (often render queues).
- `private void popMask()` — Performs class-specific logic; see call sites and owning manager flow.
- `private MaskStruct currentMask()` — Performs class-specific logic; see call sites and owning manager flow.
- `private void uploadFontModels(ObjectArrayList<ElementInstance> elements)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void releaseFontModels(ObjectArrayList<ElementInstance> elements)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ElementInstance inject(MenuInstance menu, int entryPoint, String masterKey, Consumer<ElementInstance> customizer)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ElementInstance inject(MenuInstance menu, int entryPoint, String masterKey)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void eject(MenuInstance menu, int entryPoint, ElementInstance instance)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void refreshText(ElementInstance element)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void applyInputLock(int delta)` — Performs class-specific logic; see call sites and owning manager flow.
- `private void applyRaycastLock(int delta)` — Performs class-specific logic; see call sites and owning manager flow.
- `package void addMenu(String menuName, MenuHandle menuHandle)` — Registers a child object into manager-owned collections.
- `public boolean isInputLocked()` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/runtime/input/PlayerInputSystem.java`.
- `public boolean hasMenu(String menuName)` — Boolean existence/availability check.
- `public int getMenuIDFromMenuName(String menuName)` — Returns current state/value.
- `public MenuHandle getMenuHandleFromMenuID(int menuID)` — Returns current state/value.
- `public MenuHandle getMenuHandleFromMenuName(String menuName)` — Returns current state/value.
- `public MenuInstance openMenu(String menuName, WindowInstance window)` — Activates UI/window/menu surface. Called via static reference from: `core/src/program/bootstrap/menupipeline/menu/MenuInstance.java`.
- `public MenuInstance closeMenu(MenuInstance instance)` — Performs class-specific logic; see call sites and owning manager flow.
- `public ObjectArrayList<MenuInstance> getActiveMenus()` — Returns current state/value.
- `public void request(String menuName)` — Triggers on-demand loading or lookup.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/raycastsystem/RaycastSystem.java`

**Type:** `class RaycastSystem`
  
**Inheritance/implements:** `extends SystemPackage`
  
**Package:** `program.bootstrap.menupipeline.raycastsystem`
  
**File size:** 141 lines

**What this class does:** `RaycastSystem` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.raycastsystem`.

**Who this class talks to (direct imports):**
- `program.bootstrap.inputpipeline.inputsystem.InputSystem`
- `program.bootstrap.menupipeline.element.ElementInstance`
- `program.bootstrap.menupipeline.element.ElementType`
- `program.bootstrap.menupipeline.menu.MenuInstance`
- `program.core.engine.SystemPackage`

**Method intent:**
- `protected void get()` — Returns current state/value.
- `public void update(ObjectArrayList<MenuInstance> activeMenus, float screenW, float screenH)` — Runs frame-step maintenance and logic.
- `private boolean hitTestElements(ObjectArrayList<ElementInstance> elements, float mouseX, float mouseY, float clipLeft, float clipTop, float clipRight, float clipBottom)` — Performs class-specific logic; see call sites and owning manager flow.
- `private boolean isHit(ElementInstance element, float mouseX, float mouseY)` — Performs class-specific logic; see call sites and owning manager flow.
- `public void setActive(boolean active)` — Mutates internal state for this object.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/DimensionValue.java`

**Type:** `class DimensionValue`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 50 lines

**What this class does:** `DimensionValue` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package private DimensionValue(float value, boolean percentage)` — Performs class-specific logic; see call sites and owning manager flow.
- `public DimensionValue parse(String raw)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`, `core/src/program/bootstrap/menupipeline/util/DimensionVector2.java`.
- `public float resolve(float parentDimension)` — Performs class-specific logic; see call sites and owning manager flow.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/DimensionVector2.java`

**Type:** `class DimensionVector2`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 59 lines

**What this class does:** `DimensionVector2` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`

**Method intent:**
- `package public DimensionVector2(DimensionValue x, DimensionValue y)` — Performs class-specific logic; see call sites and owning manager flow.
- `public DimensionVector2 parse(JsonObject json, String key, String defaultX, String defaultY)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/FileParserUtility.java`, `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`.
- `public DimensionValue getX()` — Returns current state/value.
- `public DimensionValue getY()` — Returns current state/value.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/LayoutStruct.java`

**Type:** `class LayoutStruct`
  
**Inheritance/implements:** `extends StructPackage`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 85 lines

**What this class does:** `LayoutStruct` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Who this class talks to (direct imports):**
- `program.core.engine.StructPackage`
- `program.core.util.mathematics.vectors.Vector2`

**Method intent:**
- `package public LayoutStruct(Vector2 anchor, Vector2 pivot, DimensionVector2 position, DimensionVector2 size, DimensionVector2 minSize, DimensionVector2 maxSize)` — Performs class-specific logic; see call sites and owning manager flow.
- `public LayoutStruct merge(LayoutStruct base, LayoutStruct override)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`.
- `public Vector2 getAnchor()` — Returns current state/value.
- `public Vector2 getPivot()` — Returns current state/value.
- `public DimensionVector2 getPosition()` — Returns current state/value.
- `public DimensionVector2 getSize()` — Returns current state/value.
- `public DimensionVector2 getMinSize()` — Returns current state/value.
- `public DimensionVector2 getMaxSize()` — Returns current state/value.
- `public boolean hasMinSize()` — Boolean existence/availability check.
- `public boolean hasMaxSize()` — Boolean existence/availability check.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/MenuAwareAction.java`

**Type:** `interface MenuAwareAction`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 14 lines

**What this class does:** `MenuAwareAction` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Who this class talks to (direct imports):**
- `program.bootstrap.menupipeline.menu.MenuInstance`

**Method intent:** No concrete methods parsed (interface marker, enum, or data-only declaration).

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/StackDirection.java`

**Type:** `enum StackDirection`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 22 lines

**What this class does:** `StackDirection` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Method intent:**
- `public StackDirection fromString(String s)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.

## `core/src/program/bootstrap/menupipeline/util/TextAlign.java`

**Type:** `enum TextAlign`
  
**Package:** `program.bootstrap.menupipeline.util`
  
**File size:** 18 lines

**What this class does:** `TextAlign` provides subsystem-specific behavior inferred from its APIs and collaborators in `program.bootstrap.menupipeline.util`.

**Method intent:**
- `public TextAlign fromString(String s)` — Performs class-specific logic; see call sites and owning manager flow. Called via static reference from: `core/src/program/bootstrap/menupipeline/menumanager/InternalBuilder.java`.

**Operational notes:**
- Track this class together with its owning manager/pipeline/context to understand when it is instantiated and invoked.
- For runtime behavior, follow lifecycle methods (`create/get/awake/update/draw/dispose`) in this class and immediate collaborators.
