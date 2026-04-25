package sudoku.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 tests for {@link Model}. Three significantly different scenarios are
 * covered: (1) rejection of edits to pre-filled cells, (2) completion detection
 * that ignores the validation-feedback flag, and (3) single-level undo
 * semantics including history reset after {@code reset()}.
 *
 * Each test writes a deterministic puzzle to a temporary file so the Model
 * starts in an exactly-known state.
 */
class ModelTest {

    // A well-known easy Sudoku puzzle and its unique solution. Using a known
    // solvable fixture keeps the tests reproducible (no reliance on puzzles.txt).
    private static final String PUZZLE_LINE =
            "530070000" +
            "600195000" +
            "098000060" +
            "800060003" +
            "400803001" +
            "700020006" +
            "060000280" +
            "000419005" +
            "000080079";

    private static final int[][] SOLUTION = {
            { 5, 3, 4, 6, 7, 8, 9, 1, 2 },
            { 6, 7, 2, 1, 9, 5, 3, 4, 8 },
            { 1, 9, 8, 3, 4, 2, 5, 6, 7 },
            { 8, 5, 9, 7, 6, 1, 4, 2, 3 },
            { 4, 2, 6, 8, 5, 3, 7, 9, 1 },
            { 7, 1, 3, 9, 2, 4, 8, 5, 6 },
            { 9, 6, 1, 5, 3, 7, 2, 8, 4 },
            { 2, 8, 7, 4, 1, 9, 6, 3, 5 },
            { 3, 4, 5, 2, 8, 6, 1, 7, 9 },
    };

    @TempDir
    Path tmp;

    private Model model;

    @BeforeEach
    void loadFixture() throws IOException {
        Path file = tmp.resolve("fixture.txt");
        Files.writeString(file, PUZZLE_LINE + "\n");
        // Seeded Random is harmless here; puzzle file contains a single puzzle.
        model = new Model(file, new Random(0));
    }

    /**
     * Scenario 1: Pre-filled cells must never be modified (FR4). Neither
     * setCell nor clearCell can change a fixed cell, and applyHint also
     * refuses to overwrite one.
     */
    @Test
    void preFilledCellsAreImmutable() {
        // (0,0) is pre-filled with 5 in the fixture.
        assertTrue(model.isFixed(0, 0), "pre-condition: (0,0) should be fixed");
        assertEquals(5, model.getCell(0, 0));

        assertFalse(model.setCell(0, 0, 7), "setCell must reject a fixed cell");
        assertEquals(5, model.getCell(0, 0), "value must be unchanged");

        assertFalse(model.clearCell(0, 0), "clearCell must reject a fixed cell");
        assertEquals(5, model.getCell(0, 0));

        // An editable cell ((0,2) is empty in the fixture) should accept edits.
        assertTrue(model.isEditable(0, 2));
        assertTrue(model.setCell(0, 2, 4));
        assertEquals(4, model.getCell(0, 2));

        // Out-of-range digits are rejected without altering the cell.
        assertFalse(model.setCell(0, 2, 0));
        assertFalse(model.setCell(0, 2, 10));
        assertEquals(4, model.getCell(0, 2));
    }

    /**
     * Scenario 2: Completion detection (FR1) fires only when the entire board
     * matches the Sudoku rules, and is independent of the validation-feedback
     * flag (FR2). Filling the board with the solution must signal completion
     * whether the flag is on or off.
     */
    @Test
    void completionDetectionIgnoresValidationFlag() {
        // Find an empty editable cell to leave out; completion must not trigger
        // while at least one such cell is still blank.
        int[] blank = firstEmpty();
        int br = blank[0], bc = blank[1];

        model.setValidationFeedbackEnabled(false);
        fillWith(SOLUTION, br, bc);
        assertFalse(model.isComplete(), "not yet complete while one cell is empty");

        assertTrue(model.setCell(br, bc, SOLUTION[br][bc]));
        assertTrue(model.isComplete(), "should be complete with feedback disabled");

        // Reset, flip the flag on, and do the same: completion must still fire.
        model.reset();
        assertFalse(model.isComplete());
        model.setValidationFeedbackEnabled(true);
        fillWith(SOLUTION, -1, -1);
        assertTrue(model.isComplete(), "should still complete with feedback enabled");
    }

    /**
     * Scenario 3: Undo is single-level, targets only editable cells (FR5), and
     * its history is discarded on reset. After a single setCell the user can
     * undo once; a second undo is a no-op, and reset wipes the buffer even if
     * an undoable move was outstanding.
     */
    @Test
    void undoIsSingleLevelAndResetClearsHistory() {
        int r = 0, c = 2; // empty editable cell
        assertEquals(0, model.getCell(r, c));

        assertTrue(model.setCell(r, c, 4));
        assertTrue(model.canUndo());
        assertTrue(model.undo());
        assertEquals(0, model.getCell(r, c), "undo must clear the change");
        assertFalse(model.canUndo(), "single-level undo: no further history");
        assertFalse(model.undo(), "second undo returns false");

        // Another move, then reset: undo history must be gone.
        assertTrue(model.setCell(r, c, 4));
        assertTrue(model.canUndo());
        model.reset();
        assertFalse(model.canUndo(), "reset must clear undo history");
        assertEquals(0, model.getCell(r, c), "reset restores initial state");

        // After reset the board must equal the puzzle's initial state.
        int[][] snapshot = model.snapshot();
        int[][] expected = parsePuzzle(PUZZLE_LINE);
        assertArrayEquals(expected, snapshot);

        // Sanity: the current board is not equal to SOLUTION.
        assertNotEquals(deepFlatten(SOLUTION), deepFlatten(snapshot));
    }

    // ---------- helpers ---------------------------------------------------

    /** Fills every editable cell with the solution value; skip cell (sr,sc) if set. */
    private void fillWith(int[][] sol, int sr, int sc) {
        for (int r = 0; r < Model.SIZE; r++) {
            for (int c = 0; c < Model.SIZE; c++) {
                if (r == sr && c == sc) continue;
                if (model.isEditable(r, c) && model.getCell(r, c) == 0) {
                    boolean ok = model.setCell(r, c, sol[r][c]);
                    assertTrue(ok, "setCell must succeed for (" + r + "," + c + ")");
                }
            }
        }
    }

    private int[] firstEmpty() {
        for (int r = 0; r < Model.SIZE; r++) {
            for (int c = 0; c < Model.SIZE; c++) {
                if (model.isEditable(r, c) && model.getCell(r, c) == 0) return new int[] { r, c };
            }
        }
        throw new IllegalStateException("no empty cell in fixture");
    }

    private static int[][] parsePuzzle(String line) {
        int[][] g = new int[9][9];
        for (int i = 0; i < 81; i++) g[i / 9][i % 9] = line.charAt(i) - '0';
        return g;
    }

    private static int deepFlatten(int[][] g) {
        int hash = 17;
        for (int[] row : g) for (int v : row) hash = hash * 31 + v;
        return hash;
    }
}
