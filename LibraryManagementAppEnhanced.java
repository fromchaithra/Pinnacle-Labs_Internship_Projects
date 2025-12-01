
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;

public class LibraryManagementAppEnhanced extends JFrame {
    // storage
    private final File storageFile = new File("library_data.ser");
    private BookTableModel tableModel = new BookTableModel();

    // UI components
    private JTable table;
    private JTextField searchField;
    private JLabel statusLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new LibraryManagementAppEnhanced().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to start application: " + e.getMessage());
            }
        });
    }

    public LibraryManagementAppEnhanced() {
        setTitle("📚 Library Management System");
        setSize(1000, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // UI defaults
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));

        initUI();
        loadData();
    }

    private void initUI() {
        getContentPane().setBackground(new Color(245, 248, 252));
        setLayout(new BorderLayout(10, 10));

        // Top toolbar
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(new EmptyBorder(10, 10, 0, 10));
        top.setBackground(new Color(245, 248, 252));

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTop.setBackground(new Color(245, 248, 252));
        JButton addBtn = styledButton("➕ Add Book");
        JButton editBtn = styledButton("✏️ Edit Book");
        JButton delBtn = styledButton("🗑 Delete Book");
        JButton borrowBtn = styledButton("📥 Borrow / Return");
        leftTop.add(addBtn); leftTop.add(editBtn); leftTop.add(delBtn); leftTop.add(borrowBtn);

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTop.setBackground(new Color(245, 248, 252));
        searchField = new JTextField(28);
        searchField.setToolTipText("Search by Title, Author or ISBN");
        JButton searchBtn = styledButton("🔍 Search");
        JButton showAllBtn = styledButton("Show All");
        JButton exportBtn = styledButton("Export CSV");
        JButton saveBtn = styledButton("Save");
        rightTop.add(searchField); rightTop.add(searchBtn); rightTop.add(showAllBtn); rightTop.add(exportBtn); rightTop.add(saveBtn);

        top.add(leftTop, BorderLayout.WEST);
        top.add(rightTop, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // Table
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        JScrollPane sc = new JScrollPane(table);
        sc.setBorder(new EmptyBorder(0, 10, 0, 10));
        add(sc, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(new EmptyBorder(6, 10, 10, 10));
        statusBar.setBackground(new Color(245, 248, 252));
        statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);

        // Button actions
        addBtn.addActionListener(e -> addBookDialog());
        editBtn.addActionListener(e -> editSelectedBook());
        delBtn.addActionListener(e -> deleteSelectedBook());
        borrowBtn.addActionListener(e -> borrowOrReturnSelected());
        searchBtn.addActionListener(e -> performSearch());
        showAllBtn.addActionListener(e -> { tableModel.resetFilter(); status("Showing all books"); });
        exportBtn.addActionListener(e -> exportCSV());
        saveBtn.addActionListener(e -> {
            saveData();
            status("Saved to " + storageFile.getName());
            JOptionPane.showMessageDialog(this, "Saved successfully.");
        });

        // Enter key in search
        searchField.addActionListener(e -> performSearch());

        // double-click row -> edit
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedBook();
            }
        });
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(70, 130, 180));
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(6, 12, 6, 12));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------- Book Operations ----------
    private void addBookDialog() {
        BookDialog.Result res = BookDialog.showDialog(this, null, tableModel.getAllISBNs());
        if (res == null) return;
        Book b = new Book(res.title, res.author, res.isbn);
        tableModel.addBook(b);
        saveData();
        status("Book added: " + b.title);
    }

    private void editSelectedBook() {
        int r = table.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Please select a book to edit."); return; }
        int modelRow = table.convertRowIndexToModel(r);
        Book existing = tableModel.getBookAt(modelRow);
        BookDialog.Result res = BookDialog.showDialog(this, existing, tableModel.getAllISBNsExcept(existing.isbn));
        if (res == null) return;
        existing.title = res.title;
        existing.author = res.author;
        existing.isbn = res.isbn;
        tableModel.fireTableRowsUpdated(modelRow, modelRow);
        saveData();
        status("Book updated: " + existing.title);
    }

    private void deleteSelectedBook() {
        int r = table.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Please select a book to delete."); return; }
        int modelRow = table.convertRowIndexToModel(r);
        Book b = tableModel.getBookAt(modelRow);
        int c = JOptionPane.showConfirmDialog(this, "Delete \"" + b.title + "\"? This cannot be undone.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            tableModel.removeBook(modelRow);
            saveData();
            status("Book deleted: " + b.title);
        }
    }

    private void borrowOrReturnSelected() {
        int r = table.getSelectedRow();
        if (r == -1) { JOptionPane.showMessageDialog(this, "Please select a book to borrow/return."); return; }
        int modelRow = table.convertRowIndexToModel(r);
        Book b = tableModel.getBookAt(modelRow);
        if (b.isBorrowed()) {
            int c = JOptionPane.showConfirmDialog(this, "Return \"" + b.title + "\" borrowed by " + b.borrower + "?", "Return Book", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                b.borrower = null; b.borrowedOn = null;
                tableModel.fireTableRowsUpdated(modelRow, modelRow);
                saveData();
                status("Book returned: " + b.title);
            }
        } else {
            String name = JOptionPane.showInputDialog(this, "Enter borrower name:");
            if (name == null) return;
            name = name.trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Borrower name cannot be empty."); return; }
            b.borrower = name;
            b.borrowedOn = new Date();
            tableModel.fireTableRowsUpdated(modelRow, modelRow);
            saveData();
            status("Book borrowed: " + b.title + " by " + name);
        }
    }

    // ---------- Search ----------
    private void performSearch() {
        String q = searchField.getText().trim();
        if (q.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter a search value (Title, Author or ISBN)."); return; }
        tableModel.filter(q);
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No books found for: \"" + q + "\"", "Search Result", JOptionPane.INFORMATION_MESSAGE);
            tableModel.resetFilter();
            status("Search returned no results for: " + q);
        } else {
            status("Showing search results for: " + q);
        }
    }

    // ---------- Persistence ----------
    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storageFile))) {
            oos.writeObject(tableModel.getAllBooks());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        if (!storageFile.exists()) { status("No saved data — start by adding books."); return; }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(storageFile))) {
            Object o = in.readObject();
            if (o instanceof List) {
                List<Book> list = (List<Book>) o;
                tableModel.setBooks(list);
                status("Loaded " + list.size() + " books from " + storageFile.getName());
            } else {
                status("Saved data corrupted or incompatible — starting empty.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            status("Failed to load data — starting empty (error: " + e.getMessage() + ")");
        }
    }

    // ---------- Export CSV ----------
    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("library_export.csv"));
        int res = chooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("Title,Author,ISBN,Status,Borrower,BorrowedOn");
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (Book b : tableModel.getAllBooks()) {
                String status = b.isBorrowed() ? "Borrowed" : "Available";
                String borrower = b.borrower == null ? "" : b.borrower.replace(",", " ");
                String date = b.borrowedOn == null ? "" : df.format(b.borrowedOn);
                pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        escapeCsv(b.title), escapeCsv(b.author), escapeCsv(b.isbn), status, escapeCsv(borrower), date);
            }
            JOptionPane.showMessageDialog(this, "Exported to " + out.getAbsolutePath());
            status("Exported CSV: " + out.getName());
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage());
        }
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }

    private void status(String s) {
        statusLabel.setText(s);
    }

    // ---------- Data Model ----------
    static class Book implements Serializable {
        String title, author, isbn;
        String borrower; Date borrowedOn;

        public Book(String title, String author, String isbn) {
            this.title = title.trim();
            this.author = author.trim();
            this.isbn = isbn.trim();
        }

        boolean isBorrowed() { return borrower != null && !borrower.isEmpty(); }
    }

    static class BookTableModel extends AbstractTableModel {
        private List<Book> books = new ArrayList<>();
        private List<Book> view = books;
        private final String[] cols = {"Title", "Author", "ISBN", "Status", "Borrower", "Borrowed On"};
        private SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

        public int getRowCount() { return view.size(); }
        public int getColumnCount() { return cols.length; }
        public String getColumnName(int c) { return cols[c]; }
        public Object getValueAt(int r, int c) {
            Book b = view.get(r);
            switch (c) {
                case 0: return b.title;
                case 1: return b.author;
                case 2: return b.isbn;
                case 3: return b.isBorrowed() ? "Borrowed" : "Available";
                case 4: return b.borrower == null ? "-" : b.borrower;
                case 5: return b.borrowedOn == null ? "-" : df.format(b.borrowedOn);
                default: return "";
            }
        }

        // CRUD
        public void addBook(Book b) { books.add(b); resetFilter(); fireTableDataChanged(); }
        public void setBooks(List<Book> list) { books = new ArrayList<>(list); view = books; fireTableDataChanged(); }
        public List<Book> getAllBooks() { return new ArrayList<>(books); }
        public Book getBookAt(int idx) { return view.get(idx); }
        public void removeBook(int idx) { Book real = view.get(idx); books.remove(real); resetFilter(); fireTableDataChanged(); }

        // filter
        public void filter(String q) {
            String low = q.toLowerCase();
            view = new ArrayList<>();
            for (Book b : books) {
                if (b.title.toLowerCase().contains(low) || b.author.toLowerCase().contains(low) || b.isbn.toLowerCase().contains(low)) {
                    view.add(b);
                }
            }
            fireTableDataChanged();
        }

        public void resetFilter() { view = books; fireTableDataChanged(); }

        public Set<String> getAllISBNs() {
            Set<String> s = new HashSet<>();
            for (Book b : books) s.add(b.isbn);
            return s;
        }
        public Set<String> getAllISBNsExcept(String isbn) {
            Set<String> s = getAllISBNs(); s.remove(isbn); return s;
        }

        public int getRowCountFull() { return books.size(); }
    }

    // ---------- Book Dialog ----------
    static class BookDialog {
        static class Result {
            String title, author, isbn;
            Result(String t, String a, String i) { title = t; author = a; isbn = i; }
        }

        static Result showDialog(Component parent, Book existing, Set<String> blockedIsbns) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setBorder(new EmptyBorder(10, 10, 10, 10));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; gbc.gridy = 0;
            p.add(new JLabel("Title:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            JTextField titleField = new JTextField(30);
            p.add(titleField, gbc);

            gbc.gridx = 0; gbc.gridy++;
            gbc.weightx = 0;
            p.add(new JLabel("Author:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            JTextField authorField = new JTextField(30);
            p.add(authorField, gbc);

            gbc.gridx = 0; gbc.gridy++;
            gbc.weightx = 0;
            p.add(new JLabel("ISBN:"), gbc);
            gbc.gridx = 1; gbc.weightx = 1;
            JTextField isbnField = new JTextField(20);
            p.add(isbnField, gbc);

            if (existing != null) {
                titleField.setText(existing.title);
                authorField.setText(existing.author);
                isbnField.setText(existing.isbn);
            }

            while (true) {
                int res = JOptionPane.showConfirmDialog(parent, p, existing == null ? "Add Book" : "Edit Book", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (res != JOptionPane.OK_OPTION) return null;
                String title = titleField.getText().trim();
                String author = authorField.getText().trim();
                String isbn = isbnField.getText().trim();
                if (title.isEmpty()) { JOptionPane.showMessageDialog(parent, "Title is required."); continue; }
                if (author.isEmpty()) { JOptionPane.showMessageDialog(parent, "Author is required."); continue; }
                if (isbn.isEmpty()) { isbn = "N/A"; } // allow missing ISBN but mark N/A
                if (blockedIsbns != null && blockedIsbns.contains(isbn)) {
                    JOptionPane.showMessageDialog(parent, "ISBN must be unique. Another book uses this ISBN."); continue;
                }
                return new Result(title, author, isbn);
            }
        }
    }
}
