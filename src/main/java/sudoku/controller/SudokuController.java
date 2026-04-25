package sudoku.controller;

import sudoku.model.Model;
import sudoku.view.SudokuView;

/**
 * Wires the SudokuView's user interactions to the Model.
 *
 * The Controller:
 *   - forwards only valid requests to the Model (querying the Model first
 *     whether the target cell is editable, whether hints are enabled, etc.);
 *   - updates button enabled states in the View;
 *   - contains no GUI (Swing) code and no game logic.
 *
 * It is installed as an Observer so that every Model change refreshes the
 * View's button states and triggers completion detection.
 */
@SuppressWarnings("deprecation")
public class SudokuController implements java.util.Observer {

    private final Model model;
    private final SudokuView view;
    private boolean completionShown = false;

    public SudokuController(Model model, SudokuView view) {
        this.model = model;
        this.view = view;
        wireView();
        model.addObserver(this);
        model.addObserver(view);
        update(model, null);
    }

    private void wireView() {
        view.setCellSelectionListener((r, c) -> refreshButtonStates());
        view.setDigitListener(this::onDigit);
        view.setNumberPadAction(this::onDigit);
        view.setEraseAction(this::onErase);
        view.setUndoAction(this::onUndo);
        view.setHintAction(this::onHint);
        view.setResetAction(this::onReset);
        view.setNewGameAction(this::onNewGame);
        view.setValidationFlagListener(model::setValidationFeedbackEnabled);
        view.setHintFlagListener(model::setHintEnabled);
        view.setRandomFlagListener(model::setRandomPuzzleSelection);
    }

    // ---------- Button / digit handlers ------------------------------------

    private void onDigit(int value) {
        int r = view.getSelectedRow();
        int c = view.getSelectedCol();
        if (!model.isEditable(r, c)) {
            view.setStatus("Pre-filled cell cannot be changed.");
            return;
        }
        if (value == 0) {
            model.clearCell(r, c);
            return;
        }
        if (value < 1 || value > 9) return;
        model.setCell(r, c, value);
        if (model.isValidationFeedbackEnabled() && !model.isCellValid(r, c)) {
            view.setStatus("Value " + value + " conflicts with row, column or box.");
        } else {
            view.setStatus(" ");
        }
    }

    private void onErase() {
        int r = view.getSelectedRow();
        int c = view.getSelectedCol();
        if (!model.isEditable(r, c)) {
            view.setStatus("Pre-filled cell cannot be cleared.");
            return;
        }
        model.clearCell(r, c);
    }

    private void onUndo() {
        if (model.canUndo()) model.undo();
    }

    private void onHint() {
        if (!model.isHintEnabled()) {
            view.setStatus("Hints are disabled.");
            return;
        }
        if (!model.applyHint()) {
            view.setStatus("No cell available for a hint.");
        }
    }

    private void onReset() {
        completionShown = false;
        model.reset();
    }

    private void onNewGame() {
        completionShown = false;
        model.loadNewPuzzle();
    }

    // ---------- Model observer --------------------------------------------

    @Override
    public void update(java.util.Observable o, Object arg) {
        refreshButtonStates();
        if (model.isComplete() && !completionShown) {
            completionShown = true;
            view.showCompletionDialog();
        }
    }

    private void refreshButtonStates() {
        view.setUndoEnabled(model.canUndo());
        view.setHintEnabled(model.isHintEnabled() && hasEmptyCell());
        int r = view.getSelectedRow();
        int c = view.getSelectedCol();
        boolean canErase = model.isEditable(r, c) && model.getCell(r, c) != 0;
        view.setEraseEnabled(canErase);
    }

    private boolean hasEmptyCell() {
        for (int r = 0; r < model.getSize(); r++) {
            for (int c = 0; c < model.getSize(); c++) {
                if (model.isEditable(r, c) && model.getCell(r, c) == 0) return true;
            }
        }
        return false;
    }
}
