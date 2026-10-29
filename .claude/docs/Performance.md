# Performance

The rules that keep frame time flat and memory small. Read it before any change to rendering, meshing, world
generation, per-frame or async work, or anything that allocates or stores bulk data. `.claude/CLAUDE.md` still
applies in full.

---

Performance is the top priority. Lag spikes, GC pauses and memory growth are bugs.

- **Zero steady-state garbage.** Nothing allocates per frame or per quad. Pool, reuse and clear instead of `new`;
  keep scratch buffers on the owner (one per thread where work is async); no boxing, no iterators or lambdas in hot
  paths, no `ensureCapacity(size + n)` growth loops (reserve the exact total once, then append).
- **Release what nothing will read again.** `clear()` keeps storage for an imminent rebuild; once data is on the GPU
  or dumped, release its storage (`QuadPacketInstance.release()`, trimmed lists).
- **Smallest footprint.** Pack data into the fewest bits that hold its range (see the Quad Records group in
  `EngineSetting`); never duplicate per-quad data per vertex; never keep a CPU copy the GPU already holds.
- **All generated mesh is greedily merged.** Terrain, partial blocks, water, items, trees and structures go through
  a greedy mesher and merge as far as their shared attributes allow. Never emit one quad per face.
- **Measure before and after.** A change that affects memory or frame time states its numbers.
