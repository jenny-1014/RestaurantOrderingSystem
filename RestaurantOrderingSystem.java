import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestaurantOrderingSystem {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}

// 菜單項目類
record MenuItem(String name, BigDecimal price) {
    @Override
    public String toString() {
        return name + " - $" + price.setScale(2, RoundingMode.HALF_UP);
    }
}

// 訂單項目（支援數量）
record OrderItem(MenuItem menuItem, int quantity) {
    BigDecimal totalPrice() {
        return menuItem.price().multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String toString() {
        return menuItem.name() + " x" + quantity + " - $" + totalPrice().setScale(2, RoundingMode.HALF_UP);
    }
}

// 訂單管理器
class OrderManager {
    private final List<OrderItem> currentOrder = new ArrayList<>();
    private final List<String> orderHistory = new ArrayList<>();
    private int dineInCount = 1;
    private int takeOutCount = 1;

    void addItem(MenuItem item, int quantity) {
        if (quantity <= 0) return;
        // 合併相同品項
        for (OrderItem existing : currentOrder) {
            if (existing.menuItem().equals(item)) {
                currentOrder.remove(existing);
                currentOrder.add(new OrderItem(item, existing.quantity() + quantity));
                return;
            }
        }
        currentOrder.add(new OrderItem(item, quantity));
    }

    void clearOrder() {
        currentOrder.clear();
    }

    boolean isEmpty() {
        return currentOrder.isEmpty();
    }

    BigDecimal calculateTotal() {
        return currentOrder.stream()
                .map(OrderItem::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    String generateOrderNumber(boolean dineIn) {
        int num = dineIn ? dineInCount++ : takeOutCount++;
        return (dineIn ? "A" : "B") + String.format("%03d", num);
    }

    void completeOrder(String orderNumber, String table, String discountDesc, BigDecimal discountAmount, BigDecimal finalTotal) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order Number: ").append(orderNumber).append("\n");
        if (table != null) sb.append("Table: ").append(table).append("\n");
        sb.append("Items:\n");
        for (OrderItem item : currentOrder) {
            sb.append("  ").append(item).append("\n");
        }
        sb.append("\nOriginal Total: $").append(String.format("%.2f", calculateTotal()))
          .append("\n").append(discountDesc).append(": -$").append(String.format("%.2f", discountAmount))
          .append("\nFinal Total: $").append(String.format("%.2f", finalTotal));

        orderHistory.add(sb.toString());
        clearOrder();
    }

    void showHistory(JFrame parent) {
        if (orderHistory.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No order history yet.");
            return;
        }

        JFrame frame = new JFrame("Order History");
        frame.setSize(500, 600);
        frame.setLocationRelativeTo(parent);

        JTextArea textArea = new JTextArea(String.join("\n\n" + "─".repeat(50) + "\n\n", orderHistory));
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setEditable(false);

        frame.add(new JScrollPane(textArea));
        frame.setVisible(true);
    }

    List<OrderItem> getCurrentOrder() {
        return Collections.unmodifiableList(currentOrder);
    }
}

// 主視窗
class MainWindow extends JFrame {
    private final OrderManager orderManager = new OrderManager();
    private final DefaultListModel<OrderItem> orderListModel = new DefaultListModel<>();

    public MainWindow() {
        setTitle("Restaurant Ordering System");
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 左側：分類
        JPanel leftPanel = createCategoryPanel();

        // 右側：目前訂單
        JPanel rightPanel = createOrderPanel();

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Menu Categories"));

        DefaultListModel<String> model = new DefaultListModel<>();
        Arrays.stream(MenuCategory.values()).forEach(c -> model.addElement(c.displayName));

        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = list.getSelectedValue();
                if (selected != null) {
                    MenuCategory category = MenuCategory.fromDisplayName(selected);
                    new MenuWindow(this, category, orderManager, orderListModel);
                }
            }
        });

        panel.add(new JScrollPane(list));
        return panel;
    }

    private JPanel createOrderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Your Order"));

        JList<OrderItem> orderList = new JList<>(orderListModel);
        panel.add(new JScrollPane(orderList), BorderLayout.CENTER);

        JLabel totalLabel = new JLabel("Total: $0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        // 即時更新總額
        orderListModel.addListDataListener(new javax.swing.event.ListDataListener() {
            @Override public void intervalAdded(javax.swing.event.ListDataEvent e) { updateTotal(totalLabel); }
            @Override public void intervalRemoved(javax.swing.event.ListDataEvent e) { updateTotal(totalLabel); }
            @Override public void contentsChanged(javax.swing.event.ListDataEvent e) { updateTotal(totalLabel); }
        });

        panel.add(totalLabel, BorderLayout.SOUTH);
        return panel;
    }

    private void updateTotal(JLabel label) {
        BigDecimal total = orderManager.calculateTotal();
        label.setText("Total: $" + total.setScale(2, RoundingMode.HALF_UP));
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout());

        JButton finishBtn = new JButton("Finish Order");
        JButton clearBtn = new JButton("Clear Order");
        JButton historyBtn = new JButton("View History");

        finishBtn.addActionListener(e -> finishOrder());
        clearBtn.addActionListener(e -> {
            orderListModel.clear();
            orderManager.clearOrder();
        });
        historyBtn.addActionListener(e -> orderManager.showHistory(this));

        panel.add(finishBtn);
        panel.add(clearBtn);
        panel.add(historyBtn);
        return panel;
    }

    private void finishOrder() {
        if (orderManager.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your order is empty!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 選擇內用/外帶
        String[] options = {"Dine-In", "Take-Out"};
        int typeChoice = JOptionPane.showOptionDialog(this, "Choose dining option:", "Order Type",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (typeChoice == -1) return;

        String table = null;
        if (typeChoice == 0) {
            String[] tables = {"Table 1", "Table 2", "Table 3", "Table 4", "Table 5"};
            table = (String) JOptionPane.showInputDialog(this, "Select table:", "Table Selection",
                    JOptionPane.QUESTION_MESSAGE, null, tables, tables[0]);
            if (table == null) return;
        }

        // 折扣
        String[] discounts = {"No Discount", "10% Off", "20% Off"};
        int discountChoice = JOptionPane.showOptionDialog(this, "Apply discount?", "Discount",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, discounts, discounts[0]);

        double rate = switch (discountChoice) {
            case 1 -> 0.9;
            case 2 -> 0.8;
            default -> 1.0;
        };
        String discountDesc = discounts[Math.max(0, discountChoice)];

        BigDecimal original = orderManager.calculateTotal();
        BigDecimal finalTotal = original.multiply(BigDecimal.valueOf(rate)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal saved = original.subtract(finalTotal);

        // 先產生訂單編號（但不要清空目前訂單）
        String orderNumber = orderManager.generateOrderNumber(typeChoice == 0);

        // 先產生收據（使用目前 orderManager 的項目）
        String receipt = generateReceipt(orderNumber, table, original, discountDesc, saved, finalTotal);

        // 完成訂單（此方法會將目前訂單清空並存入歷史）
        orderManager.completeOrder(orderNumber, table, discountDesc, saved, finalTotal);

        // 顯示收據
        JOptionPane.showMessageDialog(this, receipt, "Order Completed - " + orderNumber, JOptionPane.INFORMATION_MESSAGE);

        orderListModel.clear();
    }

    private String generateReceipt(String orderNumber, String table, BigDecimal original, String discountDesc, BigDecimal saved, BigDecimal finalTotal) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ORDER RECEIPT ===\n");
        sb.append("Order: ").append(orderNumber);
        if (table != null) sb.append(" | ").append(table);
        sb.append("\n\n");
        for (OrderItem item : orderManager.getCurrentOrder()) {
            sb.append(item).append("\n");
        }
        sb.append("\n─────────────────────\n");
        sb.append("Subtotal:     $").append(String.format("%.2f", original)).append("\n");
        sb.append(discountDesc).append("   -$").append(String.format("%.2f", saved)).append("\n");
        sb.append("TOTAL:        $").append(String.format("%.2f", finalTotal)).append("\n");
        sb.append("Thank you!");
        return sb.toString();
    }
}

// 菜單分類與項目
enum MenuCategory {
    MAIN_COURSE("Main Course", List.of(
            new MenuItem("Steak", new BigDecimal("25.0")),
            new MenuItem("Pasta", new BigDecimal("20.0")),
            new MenuItem("Pizza", new BigDecimal("20.0")),
            new MenuItem("Burger", new BigDecimal("18.0")),
            new MenuItem("Grilled Chicken", new BigDecimal("22.0")),
            new MenuItem("Salmon", new BigDecimal("28.0"))
    )),
    DESSERT("Dessert", List.of(
            new MenuItem("Ice Cream", new BigDecimal("5.0")),
            new MenuItem("Cheesecake", new BigDecimal("7.5")),
            new MenuItem("Brownie", new BigDecimal("6.5")),
            new MenuItem("Waffle", new BigDecimal("8.0"))
    )),
    DRINK("Drink", List.of(
            new MenuItem("Latte", new BigDecimal("4.5")),
            new MenuItem("Milk Tea", new BigDecimal("4.0")),
            new MenuItem("Smoothie", new BigDecimal("5.0")),
            new MenuItem("Lemonade", new BigDecimal("3.5"))
    ));

    final String displayName;
    final List<MenuItem> items;

    MenuCategory(String displayName, List<MenuItem> items) {
        this.displayName = displayName;
        this.items = items;
    }

    static MenuCategory fromDisplayName(String name) {
        return Arrays.stream(values())
                .filter(c -> c.displayName.equals(name))
                .findFirst()
                .orElseThrow();
    }
}

// 菜單視窗（可選擇數量）
class MenuWindow extends JDialog {
    public MenuWindow(JFrame parent, MenuCategory category, OrderManager orderManager, DefaultListModel<OrderItem> orderListModel) {
        super(parent, category.displayName + " Menu", true);
        setSize(400, 500);
        setLocationRelativeTo(parent);

        DefaultListModel<MenuItem> model = new DefaultListModel<>();
        category.items.forEach(model::addElement);

        JList<MenuItem> list = new JList<>(model);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MenuItem item) {
                    setText("<html><b>" + item.name() + "</b><br><font color=gray>$" + item.price() + "</font></html>");
                }
                return this;
            }
        });

        JPanel bottom = new JPanel();
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        JButton addBtn = new JButton("Add to Order");
        JButton cancelBtn = new JButton("Close");

        addBtn.addActionListener(e -> {
            MenuItem selected = list.getSelectedValue();
            if (selected != null) {
                int qty = (Integer) spinner.getValue();
                orderManager.addItem(selected, qty);
                OrderItem orderItem = new OrderItem(selected, qty);
                // 更新主視窗訂單列表
                if (!orderListModel.contains(orderItem)) {
                    // 移除舊的同品項
                    for (int i = 0; i < orderListModel.size(); i++) {
                        if (orderListModel.get(i).menuItem().equals(selected)) {
                            orderListModel.remove(i);
                            break;
                        }
                    }
                }
                orderListModel.addElement(orderItem);
                JOptionPane.showMessageDialog(this, "Added: " + selected.name() + " x" + qty);
            } else {
                JOptionPane.showMessageDialog(this, "Please select an item!");
            }
        });

        cancelBtn.addActionListener(e -> dispose());

        bottom.add(new JLabel("Quantity:"));
        bottom.add(spinner);
        bottom.add(addBtn);
        bottom.add(cancelBtn);

        add(new JScrollPane(list), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        setVisible(true);
    }
}