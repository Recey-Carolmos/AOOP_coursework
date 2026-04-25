package sudoku.model;

/**
 * A single user action that can be undone: records the cell coordinate plus
 * the value that was there before, so the Model can restore it.
 */
public final class Move {

    public final int row;
    public final int col;
    public final int previousValue;
    public final int newValue;

    public Move(int row, int col, int previousValue, int newValue) {
        assert Board.inRange(row, col);
        assert previousValue >= 0 && previousValue <= 9;
        assert newValue >= 0 && newValue <= 9;
        this.row = row;
        this.col = col;
        this.previousValue = previousValue;
        this.newValue = newValue;
    }
}
