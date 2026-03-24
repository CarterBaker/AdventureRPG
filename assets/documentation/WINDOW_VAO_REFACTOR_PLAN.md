# Window-Specific VAO Refactoring Plan

## Executive Summary

**Problem**: Editor crashes when opening detached windows because VAOs are GL context-specific but the code assumes one VAO works everywhere.

**Solution**: Change from storing VAOInstance (GPU object) in meshes to storing VAOHandle (template), with VAOManager tracking (window, template) → VAOInstance mappings.

**Impact**: ~24 files need changes across VAO, mesh, composite buffer, and render systems.

---

## How The System Currently Works

### VAO Layer

**VAOHandle** (template):
- Pure layout descriptor
- Contains `VAOData(attributeHandle=0, attrSizes[])`
- No GPU resources
- Created at bootstrap from JSON

**VAOInstance** (GPU object):
- Contains `VAOData` with real GPU handle (attributeHandle > 0)
- Created from VAOHandle template
- Represents actual GL vertex array object

**VAOManager**:
- Has palette: name → VAOHandle
- Creates VAOInstances from VAOHandles via `createVAOInstance(VAOHandle)`
- Has package-private GLSLUtility for GL isolation
- Currently creates ONE instance per template (no window tracking)

### Mesh Layer

**MeshHandle** (bootstrap-loaded mesh):
- Stores ONE `VAOInstance` + `VBOHandle` + `IBOHandle`
- Created by MeshManager's InternalLoader/InternalBuilder
- Lives forever, registered in MeshManager palette

**MeshInstance** (runtime-created mesh):
- Stores ONE `VAOInstance` + `VBOInstance` + `IBOInstance`
- Created via MeshManager.createMesh()
- Caller must dispose when done

**MeshData**:
- Aggregates `VAOData` + `VBOData` + `IBOData`
- Convenience methods: `getAttributeHandle()`, `getVertexHandle()`, etc.
- Used by ModelInstance

**ModelInstance**:
- Stores `MeshData` + `MaterialInstance`
- Exposes `getVAO()` returning GPU handle integer
- Handed to render systems

### VBO/IBO Layer

**VBOHandle/IBOHandle**:
- Store GPU handle + vertex/index data
- Created at bootstrap from JSON
- Owned by MeshManager palette

**VBOInstance/IBOInstance**:
- Created at runtime
- Store GPU handle + data
- Owned by MeshInstance

**VBOManager/IBOManager**:
- Create VBO/IBO instances
- Upload methods likely bind VAO during upload (need to verify)
- Have package-private GLSLUtility

### Composite Buffer System

**CompositeBufferInstance**:
- Stores instance VBO + ONE `compositeVAO` integer
- Used for instanced rendering
- Grows dynamically

**CompositeBufferManager**:
- Constructor creates composite VAO via GLSLUtility
- References `meshHandle.getVAOInstance()` during creation
- Has package-private GLSLUtility with `createInstancedVAO()`

**CompositeRenderSystem**:
- Collects composite render calls
- `draw()` binds single composite VAO per buffer
- Called by RenderSystem when depth=0

### Render System

**RenderSystem**:
- `draw(WindowInstance window)` flushes window's render queue
- For each render call:
  - Gets `model.getVAO()` → GPU handle integer
  - Binds VAO directly via `GLSLUtility.bindVAO(handle)`
- Calls `compositeRenderSystem.draw()` at depth 0

**RenderManager**:
- Routes render calls to windows
- Calls `renderSystem.draw(window)` to flush

### Window System

**WindowInstance**:
- Wraps WindowData (ID, dimensions)
- Owns RenderQueueHandle
- Main window flushed by engine draw loop
- Detached windows flush in ApplicationListener.render() callback
- `dispose()` called when window closes

**WindowManager**:
- Tracks all windows (main + detached)
- Issues window IDs (main=0, detached=1+)
- Manages active window for input

---

## What Is Wrong

### The Crash Scenario

1. Main window creates MeshHandle with VAOInstance (GPU handle 42 in main GL context)
2. User opens detached window → new OS window, new GL context
3. Detached window's render callback fires
4. RenderSystem.draw(detachedWindow) executes
5. Gets model.getVAO() → returns 42
6. Calls GLSLUtility.bindVAO(42)
7. **VAO handle 42 doesn't exist in detached window's GL context**
8. **OpenGL error or crash**

### Root Cause

**VAOs are GL context-local**:
- Each OS window has its own GL context
- VAO objects created in context A are invalid in context B
- GL spec: VAO handles are context-specific state

**VBOs/IBOs are shareable**:
- LibGDX enables resource sharing for buffers
- VBO/IBO handles work across contexts
- Only VAOs need per-window tracking

**Current architecture assumes**:
- One VAO per mesh is sufficient
- `model.getVAO()` returns universally valid handle
- Both assumptions break with multiple windows

### What Also Breaks

**Composite buffers have same problem**:
- Store single `compositeVAO` integer
- Created in main window context
- Invalid in detached window context
- Same crash pattern

**VBO/IBO upload might bind wrong VAO**:
- If upload methods bind a VAO during upload
- That VAO might not exist in current context
- Need to verify VBO/IBO manager code

---

## What Needs To Change

### Conceptual Shift

**Before**: One VAO per mesh (global)
**After**: One VAO per (window, mesh) pair

**Before**: VAOInstance stored in mesh
**After**: VAOHandle stored in mesh, VAOInstances tracked by manager

**Before**: `model.getVAO()` → direct GPU handle
**After**: Resolve at render time: `vaoManager.getOrCreateVAOInstance(template, window)`

### Storage Changes

**MeshHandle**: VAOInstance → VAOHandle
**MeshInstance**: VAOInstance → VAOHandle
**MeshData**: Store/expose VAOHandle instead of attributeHandle
**ModelInstance**: `getVAO()` removed, add `getVAOHandle()`

**CompositeBufferInstance**: `int compositeVAO` → `Int2IntOpenHashMap window2CompositeVAO`

### VAOManager Changes

**Add window tracking**:
```java
private Int2ObjectOpenHashMap<Short2ObjectOpenHashMap<VAOInstance>> 
    window2Template2VAOInstance;
```

**Add template ID to VAOHandle**:
```java
private final short templateID;
```

**New method**:
```java
public VAOInstance getOrCreateVAOInstance(VAOHandle template, WindowInstance window) {
    // Check cache: (windowID, templateID) → VAOInstance
    // If exists, return cached
    // If not, create new VAOInstance for this window's context
    // Store in cache and return
}
```

### Render Changes

**RenderSystem.drawBatchedRenderCall()**:
```java
// Before:
int vao = model.getVAO();
GLSLUtility.bindVAO(vao);

// After:
VAOInstance vaoInstance = vaoManager.getOrCreateVAOInstance(
    model.getVAOHandle(), 
    window
);
int vao = vaoInstance.getVAOData().getAttributeHandle();
GLSLUtility.bindVAO(vao);
```

**CompositeRenderSystem.draw()**:
```java
// Add parameter: draw(WindowInstance window)
// Check if buffer has VAO for this window
// Create if needed
// Bind window-specific VAO
```

### Cleanup Changes

**WindowInstance.dispose()**:
```java
// Clean up all VAOInstances for this window
vaoManager.removeWindowVAOs(getWindowID());
```

---

## The Plan

### Phase 1: Review All Current Files

I need to see the complete current implementation of these files to understand:
- Exact field types and names
- Method signatures
- How VAO/VBO/IBO interact during upload
- How composite VAOs are created
- GL isolation patterns

**Files needed** (24 total):

#### VAO System (4 files):
1. `com/internal/bootstrap/geometrypipeline/vao/VAOHandle.java`
2. `com/internal/bootstrap/geometrypipeline/vao/VAOInstance.java`
3. `com/internal/bootstrap/geometrypipeline/vao/VAOData.java`
4. `com/internal/bootstrap/geometrypipeline/vaomanager/VAOManager.java`

#### VBO System (4 files):
5. `com/internal/bootstrap/geometrypipeline/vbo/VBOHandle.java`
6. `com/internal/bootstrap/geometrypipeline/vbo/VBOInstance.java`
7. `com/internal/bootstrap/geometrypipeline/vbo/VBOData.java`
8. `com/internal/bootstrap/geometrypipeline/vbomanager/VBOManager.java`

#### IBO System (4 files):
9. `com/internal/bootstrap/geometrypipeline/ibo/IBOHandle.java`
10. `com/internal/bootstrap/geometrypipeline/ibo/IBOInstance.java`
11. `com/internal/bootstrap/geometrypipeline/ibo/IBOData.java`
12. `com/internal/bootstrap/geometrypipeline/ibomanager/IBOManager.java`

#### Mesh System (5 files):
13. `com/internal/bootstrap/geometrypipeline/mesh/MeshHandle.java`
14. `com/internal/bootstrap/geometrypipeline/mesh/MeshInstance.java`
15. `com/internal/bootstrap/geometrypipeline/mesh/MeshData.java`
16. `com/internal/bootstrap/geometrypipeline/model/ModelInstance.java`
17. `com/internal/bootstrap/geometrypipeline/meshmanager/MeshManager.java`

#### Composite Buffer System (4 files):
18. `com/internal/bootstrap/geometrypipeline/compositebuffer/CompositeBufferInstance.java`
19. `com/internal/bootstrap/geometrypipeline/compositebuffer/CompositeBufferData.java`
20. `com/internal/bootstrap/geometrypipeline/compositebuffermanager/CompositeBufferManager.java`
21. `com/internal/bootstrap/renderpipeline/compositerendersystem/CompositeRenderSystem.java`

#### Render System (2 files):
22. `com/internal/bootstrap/renderpipeline/rendermanager/RenderSystem.java`
23. `com/internal/bootstrap/renderpipeline/rendermanager/RenderManager.java`

#### Window System (2 files):
24. `com/internal/bootstrap/renderpipeline/window/WindowInstance.java`
25. `com/internal/bootstrap/renderpipeline/windowmanager/WindowManager.java`

### Phase 2: Design Detailed Changes

Once I see all files, I will document:

**For each file**:
- Current state summary
- Required changes (field types, method signatures)
- New methods needed
- Interaction changes with other files

**Change dependency order**:
- Which files must change first
- Which files depend on those changes
- Safe incremental refactoring path

### Phase 3: Implement Changes

Create complete, correct implementations for all modified files:
- No assumptions
- No placeholder comments
- Complete methods
- Proper error handling
- Following your architecture patterns exactly

### Phase 4: Validation

**Compile check**:
- All references valid
- No missing methods
- Type safety preserved

**Logic check**:
- VAO resolution happens at correct time
- Window parameter threaded through correctly
- Cleanup happens in right place

**Pattern check**:
- Follows your package hierarchy rules
- GL isolation in GLSLUtility
- Naming conventions respected
- Comment style matches

---

## Key Questions To Answer During Review

### VAOManager Questions:
- Does it currently have a loader or is it populated by MeshManager's loader?
- How is templateID assigned? (nextTemplateID counter?)
- Where is GLSLUtility located and what methods does it have?

### VBO/IBO Manager Questions:
- Do upload methods bind a VAO? If so, which VAO?
- Do they take VAOInstance as parameter?
- Will they need to change to accept VAOHandle?

### Composite Buffer Questions:
- When is composite VAO created? (constructor? lazy?)
- How does CompositeBufferManager's GLSLUtility work?
- What does `createInstancedVAO()` signature look like?
- Does it reference `meshHandle.getVAOInstance()`?

### Render System Questions:
- Does RenderSystem have reference to VAOManager already?
- What's the exact signature of `drawBatchedRenderCall()`?
- Does it already receive window parameter?

### Window Questions:
- Does WindowInstance.dispose() currently clean anything up?
- Are window IDs assigned sequentially?
- Main window always ID 0?

---

## Success Criteria

After refactoring:

✅ Main window renders correctly
✅ Detached window opens without crash
✅ Both windows render independently with correct VAOs
✅ Closing detached window cleans up its VAOs (no leaks)
✅ Re-opening detached window creates fresh VAOs
✅ Composite buffers work in both windows
✅ Zero allocation in steady state (cached VAO lookups)
✅ No broken compile references
✅ Architecture patterns preserved

---

## Notes

- VAOs are tiny GPU state objects (~100 bytes each)
- Window count is small (1-5 typically)
- Total VAO overhead: (num_windows × num_unique_meshes) × 100 bytes ≈ negligible
- VBO/IBO data shared across windows (only duplicate VAO state)
- Lazy creation means detached windows only create VAOs for meshes they actually render

---

## Next Steps

1. **Provide all 25 files listed in Phase 1**
2. I will review and document exact changes needed
3. I will implement complete, correct versions of all files
4. You test and verify

No more assumptions. No more made-up code. Just show me what's actually there.
