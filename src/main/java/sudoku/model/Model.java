package sudoku.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Observable;
import java.util.Random;

/**
 * Central Sudoku game model. The GUI Controller, the GUI View (via update) and
 * the CLI all drive game state through this class; it holds no UI concerns.
 *
 * The Model extends java.util.Observable and calls setChanged / notifyObservers
 * after any state change so Views can refresh.
 *
 * Class invariants (enforced by {@link #checkInvariant()} called from every
 * mutating public method):
 *   I1. board != null and is 9x9.
 *   I2. Every cell value is in 0..9.
 *   I3. solution is a 9x9 complete valid Sudoku.
 *   I4. Every fixed cell in board equals the corresponding cell in solution.
 *   I5. lastMove is null or points to an editable (non-fixed) cell.
 *   I6. puzzles is non-empty.
 *
 * Notes:
 *   - The three flags (FR3) are stored here: validation feedback, hint, and
 *     puzzle selection.
 *   - Completion detection (FR1) is solely based on every cell being filled
 *     and the board being valid; it ignores "validation feedback" flag.
 *   - Hints (FR5) return a cell whose current value differs from the solution.
 */
@SuppressWarnings("deprecation")
public class Model extends Observable {

    public static final int SIZE = Board.SIZE;
    public static final int EMPTY = Board.EMPTY;

    private final List<int[][]> puzzles;
    private final Random random;

    private Board board;
    private int[][] solution;
    private int[][] initial;
    private Move lastMove;

    private boolean validationFeedback = true;
    private boolean hintEnabled = true;
    private boolean randomPuzzleSelection = true;
    private int fixedPuzzleIndex = 0;

    /**
     * Loads puzzles from the given file and initialises the game with one.
     *
     * Pre:  puzzleFile is a readable puzzle file.
     * Post: the invariant holds and the first puzzle is loaded.
     */
    public Model(Path puzzleFile) throws IOException {
        this(puzzleFile, new Random());
    }

    /** Package-visible constructor used by tests to inject a seeded Random. */
    Model(Path puzzleFile, Random random) throws IOException {
        assert puzzleFile != null;
        assert random != null;
        this.puzzles = PuzzleLoader.load(puzzleFile);
        this.random = random;
        assert !puzzles.isEmpty();
        loadPuzzleAt(0);
        checkInvariant();
    }

    // ---------------- Read-only queries -----------------------------------

    public int getSize() { return SIZE; }

    public int getCell(int r, int c) {
        assert Board.inRange(r, c);
        return board.get(r, c);
    }

    public boolean isFixed(int r, int c) {
        assert Board.inRange(r, c);
        return board.isFixed(r, c);
    }

    public boolean isEditable(int r, int c) {
        return !isFixed(r, c);
    }

    public int[][] snapshot() { return board.snapshot(); }

    public int getPuzzleCount() { return puzzles.size(); }

    public int getFixedPuzzleIndex() { return fixedPuzzleIndex; }

    // ---------------- Flags (FR3) -----------------------------------------

    public boolean isValidationFeedbackEnabled() { return validationFeedback; }

    public void setValidationFeedbackEnabled(boolean enabled) {
        this.validationFeedback = enabled;
        setChanged();
        notifyObservers();
    }

    public boolean isHintEnabled() { return hintEnabled; }

    public void setHintEnabled(boolean enabled) {
        this.hintEnabled = enabled;
        setChanged();
        notifyObservers();
    }

    public boolean isRandomPuzzleSelection() { return randomPuzzleSelection; }

    public void setRandomPuzzleSelection(boolean random) {
        this.randomPuzzleSelection = random;
        setChanged();
        notifyObservers();
    }

    /**
     * Pre: 0 <= index < getPuzzleCount().
     */
    public void setFixedPuzzleIndex(int index) {
        assert index >= 0 && index < puzzles.size() : "fixed puzzle index out of range";
        this.fixedPuzzleIndex = index;
        setChanged();
        notifyObservers();
    }

    // ---------------- Board operations (FR4, FR5) -------------------------

    /**
     * Sets an editable cell to a digit 1..9.
     *
     * Pre:  (r,c) is a valid coordinate.
     * Post: returns true if value was accepted (cell is editable and value in 1..9);
     *       in that case the cell equals value and lastMove records the change.
     *       Returns false otherwise and the board is unchanged.
     */
    public boolean setCell(int r, int c, int value) {
        assert Board.inRange(r, c);
        if (value < 1 || value > 9) return false;
        if (board.isFixed(r, c)) return false;
        int previous = board.get(r, c);
        if (previous == value) return false;
        board.set(r, c, value);
        lastMove = new Move(r, c, previous, value);
        checkInvariant();
        setChanged();
        notifyObservers();
        return true;
    }

    /**
     * Clears an editable cell.
     *
     * Pre:  (r,c) is a valid coordinate.
     * Post: returns true if the cell was editable and was cleared (or already empty
     *       and the call is a no-op that returns false). lastMove records the change
     *       when the cell was actually cleared.
     */
    public boolean clearCell(int r, int c) {
        assert Board.inRange(r, c);
        if (board.isFixed(r, c)) return false;
        int previous = board.get(r, c);
        if (previous == EMPTY) return false;
        board.set(r, c, EMPTY);
        lastMove = new Move(r, c, previous, EMPTY);
        checkInvariant();
        setChanged();
        notifyObservers();
        return true;
    }

    public boolean canUndo() { return lastMove != null; }

    /**
     * Reverts the last setCell / clearCell / applyHint.
     *
     * Pre:  canUndo() is true.
     * Post: the cell changed by the last recorded move is restored to its previous
     *       value and lastMove is cleared (single-level undo).
     */
    public boolean undo() {
        if (lastMove == null) return false;
        Move m = lastMove;
        assert !board.isFixed(m.row, m.col) : "last move must be on an editable cell";
        board.set(m.row, m.col, m.previousValue);
        lastMove = null;
        checkInvariant();
        setChanged();
        notifyObservers();
        return true;
    }

    /**
     * Finds an empty editable cell and returns {row, col, correctValue}, or null
     * if the board has no empty cells or hints are disabled.
     */
    public int[] findHint() {
        if (!hintEnabled) return null;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!board.isFixed(r, c) && board.get(r, c) == EMPTY) {
                    return new int[] { r, c, solution[r][c] };
                }
            }
        }
        return null;
    }

    /**
     * Applies a hint (fills one empty editable cell with the correct value).
     *
     * Pre:  hint flag is enabled and the board has at least one empty editable cell.
     * Post: returns true if a cell was filled (and lastMove records the change).
     */
    public boolean applyHint() {
        int[] hint = findHint();
        if (hint == null) return false;
        int r = hint[0], c = hint[1], v = hint[2];
        assert !board.isFixed(r, c);
        int previous = board.get(r, c);
        board.set(r, c, v);
        lastMove = new Move(r, c, previous, v);
        checkInvariant();
        setChanged();
        notifyObservers();
        return true;
    }

    /**
     * Restores the current puzzle to its initial state, clearing all user entries
     * and the undo history. Completion is not triggered by this operation.
     */
    public void reset() {
        this.board = new Board(initial);
        this.lastMove = null;
        checkInvariant();
        setChanged();
        notifyObservers();
    }

    /**
     * Loads a puzzle according to the Puzzle Selection Flag (random vs fixed),
     * discarding any current game state. Completion is not triggered.
     */
    public void loadNewPuzzle() {
        int idx = randomPuzzleSelection
                ? random.nextInt(puzzles.size())
                : fixedPuzzleIndex;
        loadPuzzleAt(idx);
        checkInvariant();
        setChanged();
        notifyObservers();
    }

    private void loadPuzzleAt(int idx) {
        assert idx >= 0 && idx < puzzles.size();
        int[][] puzzle = puzzles.get(idx);
        int[][] solved = Solver.solve(puzzle);
        if (solved == null) {
            throw new IllegalStateException("Puzzle " + idx + " has no solution");
        }
        this.initial = copyGrid(puzzle);
        this.solution = solved;
        this.board = new Board(puzzle);
        this.lastMove = null;
    }

    // ---------------- Validation / completion (FR1, FR2) ------------------

    /** True if every cell agrees with the Sudoku rules (no duplicates). */
    public boolean isValid() {
        int[][] g = board.snapshot();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int v = g[r][c];
                if (v == EMPTY) continue;
                if (!Solver.isValidPlacement(g, r, c, v)) return false;
            }
        }
        return true;
    }

    /** True if the cell is empty or its value does not clash with any peer. */
    public boolean isCellValid(int r, int c) {
        assert Board.inRange(r, c);
        int v = board.get(r, c);
        if (v == EMPTY) return true;
        return Solver.isValidPlacement(board.snapshot(), r, c, v);
    }

    /**
     * Completion detection (FR1): every cell is filled and the board is valid.
     * This ignores the validation-feedback flag.
     */
    public boolean isComplete() {
        int[][] g = board.snapshot();
        return Solver.isComplete(g);
    }

    // ---------------- Internal -------------------------------------------

    private static int[][] copyGrid(int[][] g) {
        int[][] c = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) System.arraycopy(g[i], 0, c[i], 0, SIZE);
        return c;
    }

    /** Asserts the class invariants; called after every mutating public method. */
    private void checkInvariant() {
        assert board != null : "I1 board";
        assert solution != null && solution.length == SIZE : "I3 solution";
        for (int r = 0; r < SIZE; r++) {
            assert solution[r].length == SIZE;
            for (int c = 0; c < SIZE; c++) {
                int v = board.get(r, c);
                assert v >= 0 && v <= 9 : "I2 cell value range";
                int s = solution[r][c];
                assert s >= 1 && s <= 9 : "I3 solution cell range";
                if (board.isFixed(r, c)) {
                    assert v == s : "I4 fixed cell must match solution";
                }
            }
        }
        assert lastMove == null || !board.isFixed(lastMove.row, lastMove.col) : "I5";
        assert !puzzles.isEmpty() : "I6 puzzles non-empty";
    }
}
