# Sudoku — Java (CHC6186 AOOP Coursework)

Two versions of Sudoku sharing a single Model:

- **GUI** — `sudoku.GUIMain` (Swing, MVC)
- **CLI** — `sudoku.cli.SudokuCLI` (Model only, no separate view/controller)

Built with plain `javac` — no Maven, Gradle or other build framework.

## Layout

```
src/main/java/sudoku/
  model/      Board, Move, Solver, PuzzleLoader, Model (extends Observable)
  controller/ SudokuController (Observer)
  view/       SudokuView        (Observer, Swing JFrame)
  cli/        SudokuCLI
  GUIMain.java
src/test/java/sudoku/
  model/ModelTest.java          (three distinct JUnit 5 tests)
lib/
  junit-platform-console-standalone.jar   (test runner, used only for tests)
```

## Build & run

Run each snippet from the project root (`sudoku-java/`). `-ea` enables the
assertion-based invariants, pre- and post-conditions on the Model.

### Compile the main sources

```bash
mkdir -p target/classes
find src/main/java -name '*.java' > target/sources.txt
javac --release 11 -d target/classes @target/sources.txt
```

### Launch the GUI

```bash
java -ea -cp target/classes sudoku.GUIMain
```

### Launch the CLI

```bash
java -ea -cp target/classes sudoku.cli.SudokuCLI
```

### Compile and run the JUnit tests

```bash
mkdir -p target/test-classes
find src/test/java -name '*.java' > target/tests.txt
javac --release 11 \
    -cp "target/classes:lib/junit-platform-console-standalone.jar" \
    -d target/test-classes @target/tests.txt
java -ea -jar lib/junit-platform-console-standalone.jar \
    --class-path target/classes:target/test-classes \
    --scan-class-path --details=tree
```

### Clean

```bash
rm -rf target
```

## CLI commands

```
set R C V   set cell at row R col C (1-9) to digit V (1-9)
clear R C   clear an editable cell
undo        revert last move
hint        fill one correct empty cell
reset       restore puzzle to initial state
new         load another puzzle from puzzles.txt
show        redraw the board
flags       show flag states
help        command reference
quit        exit
```

## Design notes

- `Model extends java.util.Observable` and calls `setChanged` / `notifyObservers`
  after every state change (FR1, FR2, rubric "Model A").
- Three boolean flags live on the Model (FR3): validation feedback, hint
  enabled, random puzzle selection.
- `SudokuController` forwards only valid requests to the Model, queries the
  Model to enable/disable buttons, and holds no Swing references.
- `SudokuView implements java.util.Observer`; its `update` method is the only
  place that re-reads the Model for display.
- `SudokuCLI` drives the Model directly per NFR3 — no controller or view.
- Assertions cover Model invariants, pre- and post-conditions; enable with
  `-ea` to exercise them.
