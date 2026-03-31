# LibGDX Removal Plan (Core + Runtime)

This project can be migrated off LibGDX without rewriting gameplay systems by
preserving the current engine package boundaries and replacing backend glue in
layers.

## Current High-Coupling Areas

1. **Application/window lifecycle** (`Game`, `Screen`, `ApplicationListener`)
2. **Input bridge** (`Gdx.input`, `InputProcessor`, `Input.Keys`)
3. **GL dispatch** (`Gdx.gl` wrappers in GLSL utility classes)
4. **Image/pixmap loading** (`Pixmap`)
5. **Math interop shims** (methods accepting `com.badlogic.gdx.math.*`)

## Migration Strategy

### Phase 1 — Keycode decoupling (safe, low-risk)
- Replace `Input.Keys` references in core settings/systems with engine-owned
  key constants.
- Keep runtime behavior by mirroring current backend keycodes first, then
  remap to backend-neutral codes when the input backend is extracted.

### Phase 2 — Input backend extraction
- Introduce an engine `InputBackend` interface for cursor capture and per-frame
  deltas.
- Move GLFW callback registration to the lwjgl3 module.
- Remove `InputProcessor` and `Gdx.input` usage from core.

### Phase 3 — Window/app lifecycle extraction
- Replace LibGDX lifecycle types with engine-owned interfaces in core.
- Keep LWJGL as backend launcher, but directly own window loop.
- Detached preview windows become first-class engine windows.

### Phase 4 — GL wrapper replacement
- Replace `Gdx.gl` usage with direct LWJGL OpenGL bindings (`org.lwjgl.opengl.GL*`).
- Keep existing GLSLUtility API surface so render systems remain unchanged.

### Phase 5 — Resource/image replacement
- Replace `Pixmap` usage with `ImageIO` + byte buffers/stb-image where needed.
- Keep atlas/font build APIs unchanged for systems.

### Phase 6 — Dependency cleanup
- Remove all remaining `com.badlogic.gdx.*` imports.
- Remove LibGDX dependencies from Gradle modules.
- Keep Gradle and project structure unchanged.

## Guardrails

- **Do not redesign engine architecture.** Keep manager/system/context contracts.
- **Preserve existing method names where possible.**
- **Refactor in compile-safe slices** with backend adapters first, then remove
  LibGDX dependency last.
