package sudoku.model;

/**
 * Pure data representation of a 9x9 Sudoku board.
 *
 * Holds the current value of every cell and a flag marking whether the cell
 * was pre-filled by the puzzle (fixed) or is editable by the player.
 *
 * Invariants:
 *   - cells is 9x9 and every value is in 0..9 (0 meaning empty)
 *   - fixed is 9x9
 *   - for every (r,c): fixed[r][c] implies cells[r][c] != EMPTY
 */
public final class Board {

    public static final int SIZE = 9;
    public static final int BOX = 3;
    public static final int EMPTY = 0;

    private final int[][] cells;
    private final boolean[][] fixed;

    /** Builds an empty board (all cells 0, none fixed). */
    public Board() {
        this.cells = new int[SIZE][SIZE];
        this.fixed = new boolean[SIZE][SIZE];
    }

    /**
     * Builds a board from an initial grid; non-zero cells are marked fixed.
     * Pre: initial is 9x9 and every value is in 0..9.
     */
    public Board(int[][] initial) {
        assert initial != null && initial.length == SIZE : "initial must be 9 rows";
        this.cells = new int[SIZE][SIZE];
        this.fixed = new boolean[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            assert initial[r] != null && initial[r].length == SIZE : "row length 9";
            for (int c = 0; c < SIZE; c++) {
                int v = initial[r][c];
                assert v >= 0 && v <= 9 : "cell value in 0..9";
                cells[r][c] = v;
                fixed[r][c] = v != EMPTY;
            }
        }
    }

    public int get(int r, int c) {
        assert inRange(r, c);
        return cells[r][c];
    }

    /**
     * Sets a cell value.
     * Pre: (r,c) in range, value in 0..9, !isFixed(r,c).
     * Post: get(r,c) == value.
     */
    public void set(int r, int c, int value) {
        assert inRange(r, c) : "(r,c) out of range";
        assert value >= 0 && value <= 9 : "value in 0..9";
        assert !fixed[r][c] : "cannot modify a fixed cell";
        cells[r][c] = value;
        assert cells[r][c] == value;
    }

    public boolean isFixed(int r, int c) {
        assert inRange(r, c);
        return fixed[r][c];
    }

    /** Returns a defensive copy of the cell grid. */
    public int[][] snapshot() {
        int[][] copy = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(cells[r], 0, copy[r], 0, SIZE);
        }
        return copy;
    }

    public static boolean inRange(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
    }
}
