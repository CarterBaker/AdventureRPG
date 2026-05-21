# Menu System Reference

## Coordinate System

Y+ is visual top (toward toolbar). Y- is visual bottom. X+ is right. X- is left.

The screen origin (0, 0) is the bottom-left corner. The top-right corner is (screenW, screenH).

`computedTop` is the **bottom edge** of an element in Y-up space (low Y).
`computedTop + computedH` is the **top edge** (high Y).

This applies everywhere — layout math, hit detection, scroll, font rendering. Nothing inverts Y.

---

## Anchor and Pivot

### What They Mean

**Anchor** — a point on the parent the element attaches to, expressed as a normalized (0–1) position within the parent's bounds.

**Pivot** — the point on the element itself that aligns to the anchor point.

Both are specified as a string name or an explicit `{ "x": float, "y": float }` object.

### String → Float Mapping

| String            | x   | y   |
|-------------------|-----|-----|
| `"top_left"`      | 0.0 | 1.0 |
| `"top_center"`    | 0.5 | 1.0 |
| `"top_right"`     | 1.0 | 1.0 |
| `"left"`          | 0.0 | 0.5 |
| `"center"`        | 0.5 | 0.5 |
| `"right"`         | 1.0 | 0.5 |
| `"bottom_left"`   | 0.0 | 0.0 |
| `"bottom_center"` | 0.5 | 0.0 |
| `"bottom_right"`  | 1.0 | 0.0 |

y=1.0 is the visual top of the parent. y=0.0 is the visual bottom.

### Default

When anchor or pivot is omitted, both default to `"center"` (x=0.5, y=0.5).

---

## Transform Math

```
anchorX = parentLeft + anchor.x * parentW
anchorY = parentTop  + anchor.y * parentH

tx = anchorX + posX - pivot.x * w
ty = anchorY + posY - (1.0 - pivot.y) * h
```

`tx` / `ty` become `computedLeft` / `computedTop`.

### Pivot Y Semantics

`(1.0 - pivot.y) * h` is the offset from the anchor down to the element's bottom edge.

| pivot.y | Meaning                                       |
|---------|-----------------------------------------------|
| 1.0     | Element's bottom is at the anchor — hangs up  |
| 0.5     | Element is centered on the anchor             |
| 0.0     | Element's bottom is h below the anchor        |

---

## The Top Anchor + Bottom Pivot Rule

**This is the most important layout rule.**

To place an element flush at the top of its parent and extend downward:

```json
"anchor": "top_left",
"pivot":  "bottom_left"
```

With `top` anchor, anchorY lands at the visual top of the parent (high Y).
With `bottom` pivot, `(1.0 - pivot.y) * h = h`, so the element's bottom lands at `anchorY - h`.
The element spans from `anchorY - h` (bottom) to `anchorY` (top) — flush inside the parent.

**Wrong:** `"anchor": "top_left", "pivot": "top_left"` — places the element's bottom at the
parent's top edge. The entire element is above the parent.

**Correct pairings for flush placement:**

| Intent                            | Anchor                      | Pivot                       |
|-----------------------------------|-----------------------------|-----------------------------|
| Flush to top, extend down         | `top_left/center/right`     | `bottom_left/center/right`  |
| Flush to bottom, extend up        | `bottom_left/center/right`  | `top_left/center/right`     |
| Centered in parent                | `center`                    | `center`                    |
| Left-aligned, centered vertically | `left`                      | `left`                      |
| Right-aligned, centered vertically| `right`                     | `right`                     |

---

## Position

`position.x` and `position.y` offset the element from the anchor point after pivot is applied.

- Positive Y moves the element **up** (toward visual top).
- Negative Y moves the element **down** (toward visual bottom).
- Positive X moves right. Negative X moves left.

Values can be a percentage of parent size (`"5%"`) or absolute pixels (`"10px"`).

---

## Size

`size.x` and `size.y` set the element's dimensions.

- `"100%"` fills the parent dimension.
- `"50px"` is an absolute pixel size.
- `"calc(100% - 24px)"` subtracts a fixed amount from a percentage.

`min_size` and `max_size` clamp the resolved size. Both are optional.

---

## Element Types

| Type          | Description                                               |
|---------------|-----------------------------------------------------------|
| `sprite`      | Renders a sprite quad using a material/texture            |
| `texture`     | Renders a raw texture                                     |
| `button`      | Interactive sprite — supports hover and click states      |
| `label`       | Renders text using a font                                 |
| `container`   | Invisible layout container — may be stacked               |
| `toolbar`     | Full-width bar anchored to the visual top of the screen   |
| `canvas_area` | Exposes a named screen-space region for canvas rendering  |

---

## JSON File Structure

### Top-Level Keys

```json
{
    "elements": [ ... ],
    "menus":    [ ... ]
}
```

`elements` — reusable element templates defined at file scope. Referenced elsewhere via `ref` or `use`.

`menus` — named menu instances that can be opened at runtime.

### Inline Element Definition

```json
{
    "id":     "my_element",
    "type":   "sprite",
    "sprite": "menus/DefaultElement",
    "anchor": "center",
    "pivot":  "center",
    "position": { "x": "0%",  "y": "0%"  },
    "size":     { "x": "50%", "y": "10%" },
    "elements": [ ... ]
}
```

`elements` inside an element definition are its children. Children resolve their layout
relative to their parent's `computedLeft`, `computedTop`, `computedW`, `computedH`.

### ref — Reference a Template

Copies a template element by path into this position in the hierarchy. Layout fields on the
ref override the template's layout.

```json
{
    "id":  "title",
    "ref": "MainMenu/title"
}
```

```json
{
    "id":       "close",
    "ref":      "MainMenu/close",
    "anchor":   "center",
    "pivot":    "center",
    "position": { "x": "0%", "y": "25%" },
    "size":     { "x": "80%", "y": "40%" }
}
```

### use — Instantiate a Utility Template

Resolves to a named element from another file. Layout fields on the `use` override the
template layout.

```json
{
    "id":     "quit",
    "use":    "util/GenericButtons",
    "anchor": "center",
    "pivot":  "center",
    "size":   { "x": "80%", "y": "40%" }
}
```

---

## Stacked Containers

A container with `"stack": "vertical"` or `"stack": "horizontal"` lays out its children
sequentially. Anchor, pivot, and position on children are ignored — only size is used.

### Vertical Stack

Children are placed from the **top of the parent downward**. The first child in the array
is at the top. Each child sits immediately below the previous one.

`"spacing"` adds a gap between children. Accepts pixels or percent of parent.

```json
{
    "id":      "button_list",
    "type":    "container",
    "stack":   "vertical",
    "spacing": "4px",
    "anchor":  "top_left",
    "pivot":   "bottom_left",
    "size":    { "x": "100%", "y": "100%" }
}
```

### Horizontal Stack

Children are placed left to right. First child is leftmost. Cursor advances by each
child's width plus spacing.

### Content Size

The stack system tracks total content height (vertical) or width (horizontal) and writes
it to `contentH` / `contentW` on the parent. This drives scroll clamping.

---

## Scroll

Scroll is Y-up. Positive `scrollY` moves content **upward** (content scrolls up, viewport
stays fixed).

The scroll cursor starts at `parentTop + parentH - scrollY`. Each child in a vertical stack
subtracts its height from the cursor before being placed.

`scrollY` is clamped between `0` and `max(0, contentH - computedH)`.

A scroll handle element is typically positioned at the top of its track:

```json
"anchor": "top_left",
"pivot":  "bottom_left"
```

The handle's Y position is updated at runtime via `positionOverride` as the user scrolls.

---

## Toolbar

`type: "toolbar"` bypasses anchor, pivot, and position entirely. It is always placed flush
at the visual top of the screen, full screen width. Only `size.y` is read from the layout.

Children of the toolbar stack horizontally automatically.

---

## Labels

Labels render text inside an element. They inherit parent bounds when no layout is specified.

Omitting anchor, pivot, and size on a label gives it `center/center` and the default size,
which centers the text within the parent. This is the recommended default for simple labels.

```json
{
    "id":    "my_label",
    "type":  "label",
    "font":  "MontserratAlternates",
    "text":  "Hello",
    "color": [1, 1, 1, 1],
    "align": "left"
}
```

Text alignment options: `"left"`, `"center"`, `"right"`.

Font size resolves against the element's computed height.

---

## on_click

Attached to `button` elements. Calls a registered class method when clicked.

```json
"on_click": {
    "class":  "application.runtime.menueventsmanager.menus.MyBranch",
    "method": "doSomething",
    "arg":    "optional_string"
}
```

Use `"arg": "$parent"` to pass the owning `MenuInstance` to the handler. The method must
accept a `MenuInstance` parameter. Omit `arg` entirely for a no-argument method.

---

## Hover and Click States

Defined on a button's handle. When hovered, the hover state layout override replaces the
element's layout. When click-expanded, click state children render as a dropdown overlay
below the element, stacking downward from `computedTop` of the owning element.

---

## Mask

`"mask": true` on a container clips all children to its computed bounds. Masks intersect —
a child mask is clipped to its parent mask rectangle.

```json
{
    "id":   "clipped_list",
    "type": "container",
    "mask": true,
    "size": { "x": "100%", "y": "80%" }
}
```

---

## Common Patterns

### Centered on screen

```json
"anchor": "center",
"pivot":  "center"
```

### Flush to top of parent, full width, fixed height

```json
"anchor": "top_center",
"pivot":  "bottom_center",
"size":   { "x": "100%", "y": "40px" }
```

### Flush to bottom of parent, full width

```json
"anchor": "bottom_center",
"pivot":  "top_center",
"size":   { "x": "100%", "y": "40px" }
```

### Left-aligned panel, vertically centered

```json
"anchor":   "left",
"pivot":    "left",
"position": { "x": "1%", "y": "0%" },
"size":     { "x": "20%", "y": "80%" }
```

### Two buttons stacked vertically in a centered container

```json
{
    "id":       "button_container",
    "use":      "util/Containers",
    "anchor":   "center",
    "pivot":    "center",
    "size":     { "x": "20%", "y": "20%" },
    "elements": [
        {
            "id":       "top_button",
            "anchor":   "center",
            "pivot":    "center",
            "position": { "x": "0%", "y": "25%" },
            "size":     { "x": "80%", "y": "40%" }
        },
        {
            "id":       "bottom_button",
            "anchor":   "center",
            "pivot":    "center",
            "position": { "x": "0%", "y": "-25%" },
            "size":     { "x": "80%", "y": "40%" }
        }
    ]
}
```

### Scrollable list with scrollbar

```json
{
    "id":     "panel",
    "anchor": "left",
    "pivot":  "left",
    "size":   { "x": "20%", "y": "80%" },
    "elements": [
        {
            "id":     "list",
            "type":   "container",
            "mask":   true,
            "stack":  "vertical",
            "anchor": "top_left",
            "pivot":  "bottom_left",
            "size":   { "x": "88%", "y": "100%" }
        },
        {
            "id":     "scrollbar_track",
            "anchor": "top_right",
            "pivot":  "bottom_right",
            "size":   { "x": "10%", "y": "100%" },
            "elements": [
                {
                    "id":     "scroll_handle",
                    "anchor": "top_left",
                    "pivot":  "bottom_left",
                    "size":   { "x": "100%", "y": "20%" }
                }
            ]
        }
    ]
}
```

---

## Quick Reference — Rules

1. **Y+ is up.** Positive `position.y` moves elements toward the visual top.
2. **`computedTop` is the bottom edge.** `computedTop + computedH` is the top edge.
3. **`top_*` anchor + `bottom_*` pivot** = element hangs down from the top of the parent. ✓
4. **`top_*` anchor + `top_*` pivot** = element is entirely above the parent. Almost never correct.
5. **`bottom_*` anchor + `top_*` pivot** = element rises from the bottom of the parent. ✓
6. **Omitting anchor/pivot** defaults to `center/center`.
7. **Stack containers** ignore anchor, pivot, and position on children. Only `size` matters.
8. **Labels without layout** default to `center/center` and fill the parent — text centers automatically.
9. **`position.y` negative** moves down. Positive moves up.
10. **Toolbar** ignores all layout. Always full screen width, always at visual top.
