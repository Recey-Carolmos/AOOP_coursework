package sudoku.cli;

import sudoku.GUIMain;
import sudoku.model.Board;
import sudoku.model.Model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * Command-line interface for Sudoku (NFR1, NFR3). The CLI class itself is
 * responsible for user interaction and directly invokes Model methods; it
 * neither defines nor uses a separate View/Controller.
 *
 * Usage: java sudoku.cli.SudokuCLI [path/to/puzzles.txt]
 *
 * Commands:
 *   set r c v      set cell at row r, column c (1..9) to digit v (1..9)
 *   clear r c      clear an editable cell
 *   undo           revert last move
 *   hint           fill one correct empty cell
 *   reset          restore puzzle to initial state
 *   new            load a new puzzle (respects the Puzzle Selection Flag)
 *   show           redraw the board
 *   flags          show flag states
 *   quit / exit    leave the program
 */
public final class SudokuCLI {

    private final Model model;
    private final Scanner in;

    public SudokuCLI(Model model, Scanner in) {
        this.model = model;
        this.in = in;
    }

    public static void main(String[] args) throws IOException {
        Path puzzlesPath = GUIMain.resolvePuzzlePath(args);
        Model model = new Model(puzzlesPath);
        new SudokuCLI(model, new Scanner(System.in)).run();
    }

    public void run() {
        System.out.println("Sudoku — type 'help' for commands.");
        render();
        while (true) {
            System.out.print("> ");
            if (!in.hasNextLine()) break;
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();
            try {
                if (cmd.equals("quit") || cmd.equals("exit")) {
                    System.out.println("Goodbye.");
                    return;
                }
                if (cmd.equals("help")) {
                    printHelp();
                } else if (cmd.equals("show")) {
                    render();
                } else if (cmd.equals("flags")) {
                    printFlags();
                } else if (cmd.equals("set") && parts.length == 4) {
                    handleSet(parts);
                } else if (cmd.equals("clear") && parts.length == 3) {
                    handleClear(parts);
                } else if (cmd.equals("undo")) {
                    handleUndo();
                } else if (cmd.equals("hint")) {
                    handleHint();
                } else if (cmd.equals("reset")) {
                    model.reset();
                    System.out.println("Puzzle reset.");
                    render();
                } else if (cmd.equals("new")) {
                    model.loadNewPuzzle();
                    System.out.println("New puzzle loaded.");
                    render();
                } else {
                    System.out.println("Unknown command. Type 'help'.");
                }
                if (model.isComplete()) {
                    System.out.println("*** Congratulations — the puzzle is complete! ***");
                }
            } catch (NumberFormatException e) {
                System.out.println("Numeric arguments required.");
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // ---------- Command handlers ------------------------------------------

    private void handleSet(String[] parts) {
        int r = Integer.parseInt(parts[1]) - 1;
        int c = Integer.parseInt(parts[2]) - 1;
        int v = Integer.parseInt(parts[3]);
        if (!Board.inRange(r, c)) {
            System.out.println("Row and column must be in 1..9.");
            return;
        }
        if (v < 1 || v > 9) {
            System.out.println("Value must be in 1..9.");
            return;
        }
        if (!model.isEditable(r, c)) {
            System.out.println("Cell (" + (r + 1) + "," + (c + 1) + ") is pre-filled.");
            return;
        }
        if (!model.setCell(r, c, v)) {
            System.out.println("Move rejected.");
            return;
        }
        if (model.isValidationFeedbackEnabled() && !model.isCellValid(r, c)) {
            System.out.println("Warning: value " + v + " conflicts with row/column/box.");
        }
        render();
    }

    private void handleClear(String[] parts) {
        int r = Integer.parseInt(parts[1]) - 1;
        int c = Integer.parseInt(parts[2]) - 1;
        if (!Board.inRange(r, c)) {
            System.out.println("Row and column must be in 1..9.");
            return;
        }
        if (!model.isEditable(r, c)) {
            System.out.println("Cannot clear a pre-filled cell.");
            return;
        }
        if (!model.clearCell(r, c)) {
            System.out.println("Cell is already empty.");
            return;
        }
        render();
    }

    private void handleUndo() {
        if (!model.canUndo()) {
            System.out.println("Nothing to undo.");
            return;
        }
        model.undo();
        render();
    }

    private void handleHint() {
        if (!model.isHintEnabled()) {
            System.out.println("Hints are disabled.");
            return;
        }
        int[] hint = model.findHint();
        if (hint == null) {
            System.out.println("No empty cell available for a hint.");
            return;
        }
        model.applyHint();
        System.out.println("Hint: placed " + hint[2] + " at (" + (hint[0] + 1) + "," + (hint[1] + 1) + ").");
        render();
    }

    // ---------- Rendering --------------------------------------------------

    void render() {
        StringBuilder sb = new StringBuilder();
        sb.append("     1 2 3   4 5 6   7 8 9\n");
        sb.append("   +-------+-------+-------+\n");
        for (int r = 0; r < Board.SIZE; r++) {
            sb.append(" ").append(r + 1).append(" |");
            for (int c = 0; c < Board.SIZE; c++) {
                int v = model.getCell(r, c);
                sb.append(' ');
                sb.append(v == 0 ? '.' : (char) ('0' + v));
                if (c % 3 == 2) sb.append(" |");
            }
            sb.append('\n');
            if (r % 3 == 2) sb.append("   +-------+-------+-------+\n");
        }
        System.out.print(sb);
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("  set R C V   set cell at row R col C (1-9) to digit V (1-9)");
        System.out.println("  clear R C   clear an editable cell");
        System.out.println("  undo        revert last move");
        System.out.println("  hint        fill one correct empty cell");
        System.out.println("  reset       restore puzzle to initial state");
        System.out.println("  new         load another puzzle from puzzles.txt");
        System.out.println("  show        redraw the board");
        System.out.println("  flags       show flag states");
        System.out.println("  quit        exit");
    }

    private void printFlags() {
        System.out.println("validation_feedback = " + model.isValidationFeedbackEnabled());
        System.out.println("hint_enabled        = " + model.isHintEnabled());
        System.out.println("random_selection    = " + model.isRandomPuzzleSelection());
    }
}
