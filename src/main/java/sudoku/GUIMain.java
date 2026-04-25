package sudoku;

import sudoku.controller.SudokuController;
import sudoku.model.Model;
import sudoku.view.SudokuView;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entry point for the GUI version (NFR1: separate main method).
 *
 * Usage: java sudoku.GUIMain [path/to/puzzles.txt]
 */
public final class GUIMain {

    private GUIMain() { }

    public static void main(String[] args) throws IOException {
        Path puzzlesPath = resolvePuzzlePath(args);
        Model model = new Model(puzzlesPath);
        SwingUtilities.invokeLater(() -> {
            SudokuView view = new SudokuView(model);
            new SudokuController(model, view);
            view.setVisible(true);
        });
    }

    public static Path resolvePuzzlePath(String[] args) throws IOException {
        if (args.length > 0) return Paths.get(args[0]);
        for (String candidate : new String[] { "puzzles.txt", "../puzzles.txt" }) {
            Path p = Paths.get(candidate);
            if (Files.isReadable(p)) return p;
        }
        throw new IOException("Cannot find puzzles.txt (pass its path as the first argument)");
    }
}
