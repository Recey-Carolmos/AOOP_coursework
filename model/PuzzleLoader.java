package sudoku.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads Sudoku puzzles from a text file where each non-blank line is an
 * 81-character string of digits 0..9 (0 meaning empty cell), read in
 * row-major order.
 */
public final class PuzzleLoader {

    private PuzzleLoader() { }

    /**
     * Reads all puzzles from the given file.
     *
     * Pre: path is a readable file whose non-blank lines are length-81 digit strings.
     * Post: returns a non-null list; each element is a 9x9 int[][] with values in 0..9.
     */
    public static List<int[][]> load(Path path) throws IOException {
        assert path != null;
        List<int[][]> puzzles = new ArrayList<>();
        for (String raw : Files.readAllLines(path)) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.length() != Board.SIZE * Board.SIZE) {
                throw new IOException("Puzzle line is not 81 characters: " + line);
            }
            int[][] grid = new int[Board.SIZE][Board.SIZE];
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch < '0' || ch > '9') {
                    throw new IOException("Non-digit in puzzle: '" + ch + "'");
                }
                grid[i / Board.SIZE][i % Board.SIZE] = ch - '0';
            }
            puzzles.add(grid);
        }
        if (puzzles.isEmpty()) {
            throw new IOException("No puzzles found in " + path);
        }
        return puzzles;
    }
}
