# Style

How code is laid out: indentation, spacing, comments, field groups and section headers, with the reference shape
every class follows. Read it before writing or changing any Java code. `.claude/CLAUDE.md` still applies in full.

---

- 4-space indentation, 120-column limit, LF line endings (see `.editorconfig` / `.clang-format`).
- **Spaced neatly and easy to read.** A blank line after the class declaration, after a method's opening brace
  when the body has more than one statement, and between logical groups of statements. One-line getters and
  setters stay compact. Multi-parameter signatures wrap one parameter per line, as in existing code.
- **Class comment:** at most one short paragraph, inside a `/* */` block, placed directly under the class
  declaration. Nothing more. No other block comments, no Javadoc, no explanatory essays in methods.
- **Field groups** use short `//` labels, matching existing code:

  ```java
  // Internal
  private CalendarManager calendarManager;

  // Palette
  private Object2ObjectOpenHashMap<String, CalendarHandle> calendarName2CalendarHandle;
  ```

- **Section headers** use `// Name \\` and group methods, reusing the existing names wherever they fit:
  `// Base \\`, `// Internal \\`, `// Constructor \\`, `// Management \\`, `// Load \\`, `// Build \\`,
  `// On-Demand \\`, `// Update \\`, `// Render \\`, `// Draw \\`, `// Utility \\`, `// Accessible \\`.

Reference shape:

```java
public class ExampleManager extends ManagerPackage {

    /*
     * One short paragraph describing what this class owns and how it is used.
     */

    // Internal
    private OtherManager otherManager;

    // Palette
    private Object2ObjectOpenHashMap<String, ExampleHandle> exampleName2ExampleHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.exampleName2ExampleHandle = new Object2ObjectOpenHashMap<>();
        create(ExampleLoader.class);
    }

    @Override
    protected void get() {
        this.otherManager = get(OtherManager.class);
    }

    // Management \\

    void addExampleHandle(ExampleHandle exampleHandle) {
        exampleName2ExampleHandle.put(exampleHandle.getExampleName(), exampleHandle);
    }

    // Accessible \\

    public ExampleHandle getExampleHandle(String exampleName) {
        return exampleName2ExampleHandle.get(exampleName);
    }
}
```
