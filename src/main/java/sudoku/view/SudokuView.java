package sudoku.view;

import sudoku.model.Board;
import sudoku.model.Model;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Swing-based Sudoku view. Purely visual: it reads state from the Model via
 * its update method and forwards every user interaction (cell selection,
 * digit entry, button press, flag toggle) to the Controller through the
 * listener hooks registered on this view.
 *
 * Implements Observer (FR2 feedback, FR6 display refresh, etc.).
 */
@SuppressWarnings("deprecation")
public class SudokuView extends JFrame implements Observer {

    private static final Color COLOR_BG           = Color.WHITE;
    private static final Color COLOR_PEER_BG      = new Color(0xE4ECF7);
    private static final Color COLOR_SELECTED_BG  = new Color(0xBBDEFB);
    private static final Color COLOR_INVALID_BG   = new Color(0xFFCDD2);
    private static final Color COLOR_USER_FG      = new Color(0x1565C0);
    private static final Color COLOR_FIXED_FG     = Color.BLACK;

    private final Model model;

    private final JLabel[][] cells = new JLabel[Board.SIZE][Board.SIZE];
    private final JButton eraseBtn   = new JButton("Erase");
    private final JButton undoBtn    = new JButton("Undo");
    private final JButton hintBtn    = new JButton("Hint");
    private final JButton resetBtn   = new JButton("Reset");
    private final JButton newGameBtn = new JButton("New Game");
    private final JButton[] numberBtns = new JButton[9];
    private final JCheckBox validationFlagCb =
            new JCheckBox("Validation feedback", true);
    private final JCheckBox hintFlagCb =
            new JCheckBox("Hint enabled", true);
    private final JCheckBox randomFlagCb =
            new JCheckBox("Random puzzle", true);
    private final JLabel statusLabel = new JLabel(" ");

    private int selRow = 0;
    private int selCol = 0;

    public SudokuView(Model model) {
        super("Sudoku");
        this.model = model;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        add(buildGrid(), BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);
        add(statusLabel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        refreshCells();
    }

    // ---------- Construction ------------------------------------------------

    private JComponent buildGrid() {
        JPanel grid = new JPanel(new GridLayout(Board.SIZE, Board.SIZE));
        grid.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                JLabel cell = new JLabel("", SwingConstants.CENTER);
                cell.setOpaque(true);
                cell.setFont(cell.getFont().deriveFont(Font.PLAIN, 22f));
                cell.setPreferredSize(new Dimension(50, 50));
                cell.setBorder(buildCellBorder(r, c, false));
                final int rr = r, cc = c;
                cell.addMouseListener(new MouseAdapter() {
                    @Override public void mousePressed(MouseEvent e) {
                        selectCell(rr, cc);
                    }
                });
                cells[r][c] = cell;
                grid.add(cell);
            }
        }
        installKeyboardNavigation(grid);
        return grid;
    }

    private Border buildCellBorder(int r, int c, boolean selected) {
        int top    = (r % 3 == 0) ? 2 : 1;
        int left   = (c % 3 == 0) ? 2 : 1;
        int bottom = (r == 8) ? 2 : 1;
        int right  = (c == 8) ? 2 : 1;
        Border outer = new MatteBorder(top, left, bottom, right, Color.BLACK);
        if (selected) {
            return BorderFactory.createCompoundBorder(outer,
                    BorderFactory.createLineBorder(COLOR_USER_FG, 2));
        }
        return outer;
    }

    private JComponent buildRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel numberPad = new JPanel(new GridLayout(3, 3, 4, 4));
        numberPad.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        for (int i = 0; i < 9; i++) {
            numberBtns[i] = new JButton(String.valueOf(i + 1));
            numberBtns[i].setFont(numberBtns[i].getFont().deriveFont(Font.BOLD, 18f));
            numberBtns[i].setFocusable(false);
            numberPad.add(numberBtns[i]);
        }
        panel.add(numberPad);
        panel.add(Box.createVerticalStrut(12));

        for (JButton b : new JButton[] { eraseBtn, undoBtn, hintBtn, resetBtn, newGameBtn }) {
            b.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            b.setFocusable(false);
            panel.add(b);
            panel.add(Box.createVerticalStrut(4));
        }

        panel.add(Box.createVerticalStrut(8));
        for (JCheckBox cb : new JCheckBox[] { validationFlagCb, hintFlagCb, randomFlagCb }) {
            cb.setAlignmentX(JComponent.LEFT_ALIGNMENT);
            cb.setFocusable(false);
            panel.add(cb);
        }
        return panel;
    }

    private void installKeyboardNavigation(JPanel grid) {
        grid.setFocusable(true);
        grid.requestFocusInWindow();
        bindKey(grid, KeyEvent.VK_LEFT,  () -> selectCell(selRow, Math.max(0, selCol - 1)));
        bindKey(grid, KeyEvent.VK_RIGHT, () -> selectCell(selRow, Math.min(8, selCol + 1)));
        bindKey(grid, KeyEvent.VK_UP,    () -> selectCell(Math.max(0, selRow - 1), selCol));
        bindKey(grid, KeyEvent.VK_DOWN,  () -> selectCell(Math.min(8, selRow + 1), selCol));
        for (int v = 1; v <= 9; v++) {
            final int value = v;
            bindKey(grid, KeyEvent.VK_0 + v, () -> fireDigit(value));
            bindKey(grid, KeyEvent.VK_NUMPAD0 + v, () -> fireDigit(value));
        }
        bindKey(grid, KeyEvent.VK_BACK_SPACE, () -> fireDigit(0));
        bindKey(grid, KeyEvent.VK_DELETE,     () -> fireDigit(0));
    }

    private void bindKey(JComponent c, int keyCode, Runnable action) {
        Object key = "act-" + keyCode;
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyCode, 0), key);
        c.getActionMap().put(key, new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                action.run();
            }
        });
    }

    private void fireDigit(int value) {
        if (digitListener != null) digitListener.accept(value);
    }

    // ---------- Listener hooks (wired by Controller) ------------------------

    private IntConsumer digitListener;
    private CellSelectionListener cellListener;

    public interface CellSelectionListener { void onSelected(int row, int col); }

    public void setDigitListener(IntConsumer listener)        { this.digitListener = listener; }
    public void setCellSelectionListener(CellSelectionListener l) { this.cellListener = l; }

    public void setEraseAction(Runnable r)   { eraseBtn.addActionListener(e -> r.run()); }
    public void setUndoAction(Runnable r)    { undoBtn.addActionListener(e -> r.run()); }
    public void setHintAction(Runnable r)    { hintBtn.addActionListener(e -> r.run()); }
    public void setResetAction(Runnable r)   { resetBtn.addActionListener(e -> r.run()); }
    public void setNewGameAction(Runnable r) { newGameBtn.addActionListener(e -> r.run()); }

    public void setNumberPadAction(IntConsumer r) {
        for (int i = 0; i < 9; i++) {
            final int value = i + 1;
            numberBtns[i].addActionListener(e -> r.accept(value));
        }
    }

    public void setValidationFlagListener(Consumer<Boolean> l) {
        validationFlagCb.addActionListener(e -> l.accept(validationFlagCb.isSelected()));
    }
    public void setHintFlagListener(Consumer<Boolean> l) {
        hintFlagCb.addActionListener(e -> l.accept(hintFlagCb.isSelected()));
    }
    public void setRandomFlagListener(Consumer<Boolean> l) {
        randomFlagCb.addActionListener(e -> l.accept(randomFlagCb.isSelected()));
    }

    // ---------- Button state setters (called by Controller) ----------------

    public void setUndoEnabled(boolean enabled)  { undoBtn.setEnabled(enabled); }
    public void setHintEnabled(boolean enabled)  { hintBtn.setEnabled(enabled); }
    public void setEraseEnabled(boolean enabled) { eraseBtn.setEnabled(enabled); }

    public int getSelectedRow() { return selRow; }
    public int getSelectedCol() { return selCol; }

    public void showCompletionDialog() {
        JOptionPane.showMessageDialog(this, "Congratulations! Puzzle solved.",
                "Puzzle Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    public void setStatus(String text) { statusLabel.setText(text == null ? " " : text); }

    // ---------- Observer implementation ------------------------------------

    @Override
    public void update(Observable o, Object arg) {
        refreshCells();
        validationFlagCb.setSelected(model.isValidationFeedbackEnabled());
        hintFlagCb.setSelected(model.isHintEnabled());
        randomFlagCb.setSelected(model.isRandomPuzzleSelection());
    }

    // ---------- Internal rendering -----------------------------------------

    private void selectCell(int row, int col) {
        selRow = row;
        selCol = col;
        refreshCells();
        if (cellListener != null) cellListener.onSelected(row, col);
    }

    private void refreshCells() {
        int selectedValue = model.getCell(selRow, selCol);
        boolean feedback = model.isValidationFeedbackEnabled();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                JLabel label = cells[r][c];
                int v = model.getCell(r, c);
                label.setText(v == 0 ? "" : String.valueOf(v));
                boolean fixed = model.isFixed(r, c);
                label.setFont(label.getFont().deriveFont(
                        fixed ? Font.BOLD : Font.PLAIN, 22f));
                label.setForeground(fixed ? COLOR_FIXED_FG : COLOR_USER_FG);
                Color bg = COLOR_BG;
                if (isPeer(r, c, selRow, selCol)) bg = COLOR_PEER_BG;
                if (selectedValue != 0 && v == selectedValue && !(r == selRow && c == selCol)) {
                    bg = COLOR_PEER_BG;
                }
                if (feedback && !model.isCellValid(r, c)) bg = COLOR_INVALID_BG;
                if (r == selRow && c == selCol) bg = COLOR_SELECTED_BG;
                label.setBackground(bg);
                label.setBorder(buildCellBorder(r, c, r == selRow && c == selCol));
            }
        }
    }

    private static boolean isPeer(int r, int c, int sr, int sc) {
        if (r == sr || c == sc) return true;
        return (r / 3 == sr / 3) && (c / 3 == sc / 3);
    }
}
