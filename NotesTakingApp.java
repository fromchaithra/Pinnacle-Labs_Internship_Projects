
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;


public class NotesTakingApp extends JFrame {
    private DefaultListModel<String> notesModel = new DefaultListModel<>();
    private JList<String> notesList = new JList<>(notesModel);
    private JTextArea noteArea = new JTextArea();
    private JTextField searchField = new JTextField();
    private File notesFile = new File("notes_data.txt");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NotesTakingApp().setVisible(true));
    }

    public NotesTakingApp() {
        setTitle("📝 Notes Taking Application");
        setSize(900, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Label.font", new Font("Segoe UI", Font.BOLD, 13));
        initUI();
        loadNotes();
    }

    private void initUI() {
        getContentPane().setBackground(new Color(240, 247, 255));
        notesList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        noteArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        noteArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        leftPanel.setBackground(new Color(240, 247, 255));

        JLabel title = new JLabel("My Notes");
        title.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));
        leftPanel.add(title, BorderLayout.NORTH);

        JScrollPane listScroll = new JScrollPane(notesList);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        JButton addBtn = styledButton("➕ New");
        JButton deleteBtn = styledButton("🗑 Delete");
        JButton saveBtn = styledButton("💾 Save");
        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        btnPanel.setBackground(new Color(240, 247, 255));
        btnPanel.add(addBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(saveBtn);
        leftPanel.add(btnPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        rightPanel.setBackground(Color.WHITE);
        JLabel editorLbl = new JLabel("Note Editor");
        editorLbl.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));
        rightPanel.add(editorLbl, BorderLayout.NORTH);
        noteArea.setWrapStyleWord(true);
        noteArea.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(noteArea);
        scroll.setBorder(new LineBorder(new Color(200, 210, 230)));
        rightPanel.add(scroll, BorderLayout.CENTER);

        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.add(new JLabel("🔍 Search:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        rightPanel.add(searchPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(280);
        add(split);

        // Listeners
        addBtn.addActionListener(e -> addNote());
        deleteBtn.addActionListener(e -> deleteNote());
        saveBtn.addActionListener(e -> saveNotes());
        notesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = notesList.getSelectedValue();
                if (selected != null) loadNoteContent(selected);
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                searchNote();
            }
        });
    }

    private JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(new Color(95, 158, 250));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 12, 8, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // -------- Core Functionalities --------

    private void addNote() {
        String name = JOptionPane.showInputDialog(this, "Enter note title:");
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title cannot be empty!");
            return;
        }
        if (notesModel.contains(name.trim())) {
            JOptionPane.showMessageDialog(this, "Note with same title already exists!");
            return;
        }
        notesModel.addElement(name.trim());
        noteArea.setText("");
        notesList.setSelectedIndex(notesModel.size() - 1);
        saveNotes();
    }

    private void deleteNote() {
        int idx = notesList.getSelectedIndex();
        if (idx == -1) {
            JOptionPane.showMessageDialog(this, "Please select a note to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this note?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            notesModel.remove(idx);
            noteArea.setText("");
            saveNotes();
        }
    }

    private void searchNote() {
        String text = searchField.getText().trim().toLowerCase();
        if (text.isEmpty()) return;
        boolean found = false;
        for (int i = 0; i < notesModel.size(); i++) {
            if (notesModel.get(i).toLowerCase().contains(text)) {
                notesList.setSelectedIndex(i);
                found = true;
                break;
            }
        }
        if (!found) {
            JOptionPane.showMessageDialog(this, "Note not found!", "Search Result",
                    JOptionPane.INFORMATION_MESSAGE);
            notesList.clearSelection();
        }
    }

    private void saveNotes() {
        try (PrintWriter out = new PrintWriter(new FileWriter(notesFile))) {
            for (int i = 0; i < notesModel.size(); i++) {
                String title = notesModel.get(i);
                out.println("###" + title);
                if (notesList.getSelectedValue() != null &&
                        notesList.getSelectedValue().equals(title)) {
                    out.println(noteArea.getText());
                } else {
                    out.println(readNoteContent(title));
                }
                out.println("$$$");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving notes!");
        }
    }

    private void loadNotes() {
        if (!notesFile.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(notesFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("###")) {
                    notesModel.addElement(line.substring(3));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadNoteContent(String title) {
        noteArea.setText(readNoteContent(title));
    }

    private String readNoteContent(String title) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(notesFile))) {
            String line;
            boolean found = false;
            while ((line = br.readLine()) != null) {
                if (line.equals("###" + title)) {
                    found = true;
                    continue;
                }
                if (found) {
                    if (line.equals("$$$")) break;
                    content.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString().trim();
    }
}
