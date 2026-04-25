package sudoku.model;

/**
 * Back-tracking Sudoku solver and rule checks.
 *
 * Used by the Model to compute a puzzle's complete solution at load time
 * (so hints are cheap and deterministic thereafter) and to test whether a
 * candidate value can be legally placed at a cell.
 */
public final class Solver {

    private Solver() { }

    /**
     * Attempts to solve the given grid. Returns a new fully-filled 9x9 grid
     * if a solution exists, otherwise null. The input grid is not mutated.
     *
     * Pre: grid is 9x9 with values in 0..9.
     * Post: result is null, or a 9x9 grid with values in 1..9 satisfying
     *       the Sudoku rules and agreeing with grid at every non-empty cell.
     */
    public static int[][] solve(int[][] grid) {
        assert grid != null && grid.length == Board.SIZE;
        int[][] working = deepCopy(grid);
        boolean ok = backtrack(working);
        assert !ok || isComplete(working) : "solved grid must be complete";
        return ok ? working : null;
    }

    /** Returns true if placing value at (r,c) breaks no row / column / box rule. */
    public static boolean isValidPlacement(int[][] grid, int r, int c, int value) {
        assert Board.inRange(r, c);
        assert value >= 1 && value <= 9;
        for (int i = 0; i < Board.SIZE; i++) {
            if (i != c && grid[r][i] == value) return false;
            if (i != r && grid[i][c] == value) return false;
        }
        int br = (r / Board.BOX) * Board.BOX;
        int bc = (c / Board.BOX) * Board.BOX;
        for (int i = br; i < br + Board.BOX; i++) {
            for (int j = bc; j < bc + Board.BOX; j++) {
                if ((i != r || j != c) && grid[i][j] == value) return false;
            }
        }
        return true;
    }

    /** Returns true if every cell is filled (1..9) and the board is valid. */
    public static boolean isComplete(int[][] grid) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                int v = grid[r][c];
                if (v < 1 || v > 9) return false;
                if (!isValidPlacement(grid, r, c, v)) return false;
            }
        }
        return true;
    }

    private static boolean backtrack(int[][] g) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (g[r][c] == Board.EMPTY) {
                    for (int v = 1; v <= 9; v++) {
                        if (isValidPlacement(g, r, c, v)) {
                            g[r][c] = v;
                            if (backtrack(g)) return true;
                            g[r][c] = Board.EMPTY;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private static int[][] deepCopy(int[][] g) {
        int[][] out = new int[Board.SIZE][Board.SIZE];
        for (int r = 0; r < Board.SIZE; r++) {
            System.arraycopy(g[r], 0, out[r], 0, Board.SIZE);
        }
        return out;
    }
}
