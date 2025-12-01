import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;

public class EcommerceCartEnhanced extends JFrame {
    // Storage files
    private final File cartFile = new File("cart_data.ser");
    private final File ordersFile = new File("orders.ser");

    // Data
    private final List<Product> products = new ArrayList<>();
    private Cart cart = new Cart();

    // UI
    private DefaultTableModel productModel;
    private DefaultTableModel cartModel;
    private JTable productTable;
    private JTable cartTable;
    private JLabel totalLabel = new JLabel("Total: ₹0.00");
    private JTextField searchField = new JTextField();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new EcommerceCartEnhanced().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to start app: " + e.getMessage());
            }
        });
    }

    public EcommerceCartEnhanced() {
        setTitle("🛒 E-Commerce Cart Application");
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // UI defaults
        UIManager.put("Button.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 13));

        loadProducts();
        loadCart();
        initUI();
        refreshCartTable();
    }

    private void loadProducts() {
        // Preload sample catalog. In real project you can load from file.
        products.add(new Product("P001", "Wireless Mouse", 599.0));
        products.add(new Product("P002", "Mechanical Keyboard", 1799.0));
        products.add(new Product("P003", "USB-C Charger 65W", 1299.0));
        products.add(new Product("P004", "Wireless Headphones", 2499.0));
        products.add(new Product("P005", "Laptop Stand", 899.0));
        products.add(new Product("P006", "External SSD 1TB", 5999.0));
    }

    private void initUI() {
        getContentPane().setBackground(new Color(240, 247, 255));
        setLayout(new BorderLayout(10, 10));

        // Top: product search and actions
        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setBorder(new EmptyBorder(10, 10, 0, 10));
        top.setBackground(new Color(240, 247, 255));

        JPanel leftTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTop.setBackground(new Color(240, 247, 255));
        leftTop.add(new JLabel("Products"));
        top.add(leftTop, BorderLayout.WEST);

        JPanel rightTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTop.setBackground(new Color(240, 247, 255));
        searchField.setColumns(30);
        searchField.setToolTipText("Search products by name or id");
        JButton searchBtn = styledButton("🔍 Search");
        JButton showAllBtn = styledButton("Show All");
        rightTop.add(searchField);
        rightTop.add(searchBtn);
        rightTop.add(showAllBtn);
        top.add(rightTop, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // Center split: products (left) | cart (right)
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createProductPanel(), createCartPanel());
        split.setDividerLocation(560);
        add(split, BorderLayout.CENTER);

        // Bottom: total + actions
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        bottom.setBackground(new Color(240, 247, 255));
        JButton exportCartBtn = styledButton("Export Cart (CSV)");
        JButton clearCartBtn = styledButton("Clear Cart");
        JButton checkoutBtn = styledButton("Checkout");
        JButton exportOrdersBtn = styledButton("Export All Orders (CSV)");
        bottom.add(totalLabel);
        bottom.add(exportCartBtn);
        bottom.add(clearCartBtn);
        bottom.add(checkoutBtn);
        bottom.add(exportOrdersBtn);
        add(bottom, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> searchProducts());
        showAllBtn.addActionListener(e -> { populateProductTable(products); status("Showing all products"); });
        exportCartBtn.addActionListener(e -> exportCartCSV());
        clearCartBtn.addActionListener(e -> { if(confirm("Clear the entire cart?")){ cart.clear(); saveCart(); refreshCartTable(); status("Cart cleared"); }});
        checkoutBtn.addActionListener(e -> doCheckout());
        exportOrdersBtn.addActionListener(e -> exportOrdersCSV());
    }
    private JButton styledButton(String text) {
    JButton btn = new JButton(text);
    btn.setBackground(new Color(70, 130, 180));
    btn.setForeground(Color.WHITE);
    btn.setFocusPainted(false);
    btn.setBorder(new EmptyBorder(6, 12, 6, 12));
    btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    return btn;
    }
    private JPanel createProductPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(10, 10, 10, 6));
        p.setBackground(new Color(240, 247, 255));
        productModel = new DefaultTableModel(new String[]{"ID", "Name", "Price (₹)", "Action"}, 0) {
            public boolean isCellEditable(int r, int c) { return c == 3; }
        };
        productTable = new JTable(productModel);
        productTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        productTable.getColumn("Action").setCellEditor(new ProductButtonEditor((id)-> onAddToCart(id)));
        productTable.setRowHeight(26);
        populateProductTable(products);

        p.add(new JScrollPane(productTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel createCartPanel() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(10, 6, 10, 10));
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(520, 0));

        JLabel lbl = new JLabel("Cart");
        lbl.setBorder(new EmptyBorder(4,4,4,4));
        lbl.setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
        p.add(lbl, BorderLayout.NORTH);

        cartModel = new DefaultTableModel(new String[]{"ID", "Name", "Qty", "Unit ₹", "Subtotal ₹", "Remove"}, 0) {
            public boolean isCellEditable(int r, int c) { return c == 2 || c == 5; }
        };
        cartTable = new JTable(cartModel);
        cartTable.getColumn("Remove").setCellRenderer(new ButtonRenderer());
        cartTable.getColumn("Remove").setCellEditor(new CartButtonEditor((id)-> { cart.removeItem(id); saveCart(); refreshCartTable(); }));
        cartTable.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 2) {
                int row = e.getFirstRow();
                try {
                    int qty = Integer.parseInt("" + cartModel.getValueAt(row, 2));
                    if (qty <= 0) { JOptionPane.showMessageDialog(this, "Quantity must be >= 1"); refreshCartTable(); return; }
                    String id = (String) cartModel.getValueAt(row, 0);
                    cart.updateQty(id, qty);
                    saveCart();
                    refreshCartTable();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid quantity. Please enter a numeric value.");
                    refreshCartTable();
                }
            }
        });

        JScrollPane sc = new JScrollPane(cartTable);
        sc.setBorder(new LineBorder(new Color(220, 230, 245)));
        p.add(sc, BorderLayout.CENTER);

        // Quick actions on right of cart
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setBackground(Color.WHITE);
        JButton saveCartBtn = styledButton("💾 Save Cart");
        JButton loadCartBtn = styledButton("📂 Load Cart");
        right.add(saveCartBtn);
        right.add(loadCartBtn);
        p.add(right, BorderLayout.SOUTH);

        saveCartBtn.addActionListener(e -> { saveCart(); JOptionPane.showMessageDialog(this, "Cart saved."); status("Cart saved."); });
        loadCartBtn.addActionListener(e -> { loadCart(); refreshCartTable(); status("Cart loaded."); });

        return p;
    }

    private void populateProductTable(List<Product> list) {
        productModel.setRowCount(0);
        for (Product p : list) {
            productModel.addRow(new Object[]{p.id, p.name, p.price, "Add"});
        }
    }

    private void onAddToCart(String id) {
        Product p = findProduct(id);
        if (p == null) { JOptionPane.showMessageDialog(this, "Product not found: " + id); return; }
        String qtyStr = JOptionPane.showInputDialog(this, "Quantity:", "1");
        if (qtyStr == null) return;
        qtyStr = qtyStr.trim();
        if (qtyStr.isEmpty()) qtyStr = "1";
        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
            if (qty <= 0) { JOptionPane.showMessageDialog(this, "Quantity must be at least 1."); return; }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity. Please enter a number.");
            return;
        }
        cart.addItem(p, qty); // if exists, increases qty
        saveCart();
        refreshCartTable();
        status("Added " + qty + " x " + p.name + " to cart");
    }

    private Product findProduct(String id) {
        for (Product p : products) if (p.id.equals(id)) return p;
        return null;
    }

    // ---------- Cart logic ----------
    private void refreshCartTable() {
        cartModel.setRowCount(0);
        double total = 0;
        for (CartItem it : cart.items) {
            double subtotal = it.product.price * it.qty;
            cartModel.addRow(new Object[]{it.product.id, it.product.name, it.qty, it.product.price, subtotal, "Remove"});
            total += subtotal;
        }
        double tax = round(total * 0.18); // 18% GST
        double shipping = total > 2000 ? 0.0 : 99.0;
        double grand = round(total + tax + shipping);
        totalLabel.setText(String.format("Total: ₹%.2f  (Tax: ₹%.2f, Ship: ₹%.2f) => Grand: ₹%.2f", total, tax, shipping, grand));
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ---------- Search ----------
    private void searchProducts() {
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter a search term (product name or id)."); return; }
        List<Product> found = new ArrayList<>();
        for (Product p : products) {
            if (p.name.toLowerCase().contains(q) || p.id.toLowerCase().contains(q)) found.add(p);
        }
        if (found.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No products found for: \"" + q + "\"", "Search Result", JOptionPane.INFORMATION_MESSAGE);
            populateProductTable(products);
            status("Search returned no products for: " + q);
        } else {
            populateProductTable(found);
            status("Showing search results for: " + q);
        }
    }

    // ---------- Checkout ----------
    private void doCheckout() {
        if (cart.items.isEmpty()) { JOptionPane.showMessageDialog(this, "Cart is empty. Add items before checkout."); return; }
        String buyer = JOptionPane.showInputDialog(this, "Enter buyer name:");
        if (buyer == null) return;
        buyer = buyer.trim();
        if (buyer.isEmpty()) { JOptionPane.showMessageDialog(this, "Buyer name is required for checkout."); return; }

        // Confirm summary
        double total = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("Order summary:\n");
        for (CartItem it : cart.items) {
            double sub = it.product.price * it.qty;
            sb.append(String.format("%s x %d = ₹%.2f\n", it.product.name, it.qty, sub));
            total += sub;
        }
        double tax = round(total * 0.18);
        double shipping = total > 2000 ? 0.0 : 99.0;
        double grand = round(total + tax + shipping);
        sb.append(String.format("\nSubtotal: ₹%.2f\nTax (18%%): ₹%.2f\nShipping: ₹%.2f\nGrand Total: ₹%.2f\n\nProceed to place order?", total, tax, shipping, grand));

        int conf = JOptionPane.showConfirmDialog(this, sb.toString(), "Confirm Order", JOptionPane.YES_NO_OPTION);
        if (conf != JOptionPane.YES_OPTION) return;

        Order o = new Order(buyer, new Date(), cart.items);
        saveOrder(o);
        cart.clear();
        saveCart();
        refreshCartTable();
        JOptionPane.showMessageDialog(this, "Order placed successfully. Order ID: " + o.id);
        status("Order placed: " + o.id);
        // Ask to export invoice
        int exp = JOptionPane.showConfirmDialog(this, "Export order invoice (CSV)?", "Export", JOptionPane.YES_NO_OPTION);
        if (exp == JOptionPane.YES_OPTION) exportOrderCSV(o);
    }

    // ---------- Persistence: cart ----------
    private void saveCart() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(cartFile))) {
            oos.writeObject(cart);
        } catch (Exception e) {
            e.printStackTrace();
            status("Failed to save cart: " + e.getMessage());
        }
    }

    private void loadCart() {
        if (!cartFile.exists()) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(cartFile))) {
            Object o = in.readObject();
            if (o instanceof Cart) {
                cart = (Cart) o;
                status("Cart loaded: " + cart.items.size() + " items");
            } else {
                status("Cart file incompatible. Starting empty cart.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            status("Failed to load cart: " + e.getMessage());
        }
    }

    // ---------- Persistence: orders ----------
    private void saveOrder(Order order) {
        try {
            ArrayList<Order> list = new ArrayList<>();
            if (ordersFile.exists()) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(ordersFile))) {
                    Object o = in.readObject();
                    if (o instanceof ArrayList) {
                        @SuppressWarnings("unchecked")
                        ArrayList<Order> existing = (ArrayList<Order>) o;
                        list.addAll(existing);
                    }
                } catch (Exception ex) {
                    // ignore and overwrite
                }
            }
            list.add(order);
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ordersFile))) {
                oos.writeObject(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
            status("Failed to save order: " + e.getMessage());
        }
    }

    // ---------- Export CSV ----------
    private void exportCartCSV() {
        if (cart.items.isEmpty()) { JOptionPane.showMessageDialog(this, "Cart is empty."); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("cart_export.csv"));
        int res = chooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("ID,Name,Qty,Unit,Subtotal");
            for (CartItem it : cart.items) {
                pw.printf("\"%s\",\"%s\",%d,%.2f,%.2f%n", esc(it.product.id), esc(it.product.name), it.qty, it.product.price, it.product.price * it.qty);
            }
            JOptionPane.showMessageDialog(this, "Saved cart to " + out.getAbsolutePath());
            status("Exported cart: " + out.getName());
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }

    private void exportOrderCSV(Order order) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(order.id + "_invoice.csv"));
        int res = chooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("OrderID,Buyer,Date,ItemID,ItemName,Qty,Unit,Subtotal");
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (CartItem it : order.items) {
                pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f,%.2f%n",
                        esc(order.id), esc(order.buyer), df.format(order.date),
                        esc(it.product.id), esc(it.product.name), it.qty, it.product.price, it.product.price * it.qty);
            }
            JOptionPane.showMessageDialog(this, "Order invoice saved to " + out.getAbsolutePath());
            status("Exported invoice: " + out.getName());
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }

    private void exportOrdersCSV() {
        if (!ordersFile.exists()) { JOptionPane.showMessageDialog(this, "No saved orders."); return; }
        ArrayList<Order> list = new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(ordersFile))) {
            Object o = in.readObject();
            if (o instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<Order> existing = (ArrayList<Order>) o;
                list.addAll(existing);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to read orders: " + e.getMessage());
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("orders_export.csv"));
        int res = chooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("OrderID,Buyer,Date,Total");
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (Order o : list) {
                double total = 0;
                for (CartItem it : o.items) total += it.product.price * it.qty;
                pw.printf("\"%s\",\"%s\",\"%s\",%.2f%n", esc(o.id), esc(o.buyer), df.format(o.date), total);
            }
            JOptionPane.showMessageDialog(this, "Exported orders to " + out.getAbsolutePath());
            status("Exported orders CSV: " + out.getName());
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
        }
    }

    // ---------- Utility ----------
    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private String esc(String s) { return s == null ? "" : s.replace("\"", "\"\""); }

    private void status(String s) { setTitle("🛒 E-Commerce Cart Application — " + s); }

    // ---------- Inner classes: Product, Cart, CartItem, Order ----------
    static class Product implements Serializable {
        String id, name;
        double price;
        Product(String id, String name, double price) { this.id = id; this.name = name; this.price = price; }
    }

    static class CartItem implements Serializable {
        Product product;
        int qty;
        CartItem(Product p, int q) { product = p; qty = q; }
    }

    static class Cart implements Serializable {
        List<CartItem> items = new ArrayList<>();
        void addItem(Product p, int qty) {
            for (CartItem it : items) {
                if (it.product.id.equals(p.id)) { it.qty += qty; return; }
            }
            items.add(new CartItem(p, qty));
        }
        void removeItem(String id) { items.removeIf(i -> i.product.id.equals(id)); }
        void updateQty(String id, int q) {
            for (CartItem it : items) if (it.product.id.equals(id)) { if (q <= 0) removeItem(id); else it.qty = q; }
        }
        void clear() { items.clear(); }
    }

    static class Order implements Serializable {
        String id, buyer;
        Date date;
        List<CartItem> items;
        Order(String buyer, Date date, List<CartItem> items) {
            this.id = "ORD" + System.currentTimeMillis();
            this.buyer = buyer; this.date = date;
            // deep copy
            this.items = new ArrayList<>();
            for (CartItem it : items) this.items.add(new CartItem(it.product, it.qty));
        }
    }

    // ---------- Table button renderers & editors ----------
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); setBackground(new Color(70,130,180)); setForeground(Color.WHITE); setBorder(new EmptyBorder(6,10,6,10)); }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    interface ProductAction { void run(String id); }

    class ProductButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private ProductAction action;
        private String selectedId;

        public ProductButtonEditor(ProductAction action) {
            super(new JCheckBox());
            this.action = action;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            selectedId = (String) table.getValueAt(row, 0);
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) action.run(selectedId);
            isPushed = false;
            return label;
        }
    }

    class CartButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private boolean isPushed;
        private ProductAction action;
        private String selectedId;

        public CartButtonEditor(ProductAction action) {
            super(new JCheckBox());
            this.action = action;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            button.setText(value == null ? "" : value.toString());
            isPushed = true;
            selectedId = (String) table.getValueAt(row, 0);
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) action.run(selectedId);
            isPushed = false;
            return button.getText();
        }
    }
}
