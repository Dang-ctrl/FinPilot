import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * A graphical user interface (GUI) personal finance tracker application.
 * This program uses Java Swing to provide a visual interface for users to
 * manage their income and expenses.
 */
public class FinanceTrackerGUI extends JFrame {

    private static final String DATA_FILE = "transactions.csv";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final List<Transaction> transactions = new ArrayList<>();
    private final DefaultTableModel tableModel;
    private final JTable transactionTable;

    private final JTextField descriptionField;
    private final JTextField amountField;
    private final JComboBox<String> typeComboBox;
    private final JTextField categoryField;
    private final JTextField dateField;

    /**
     * Main entry point of the application.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new FinanceTrackerGUI().setVisible(true);
        });
    }

    /**
     * Constructor for the GUI. Initializes the components and sets up the layout.
     */
    public FinanceTrackerGUI() {
        // Set up the main window (JFrame)
        setTitle("Personal Finance Tracker");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Load data from file on startup
        loadTransactionsFromFile();
        
        // Main content panel with a border layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create and set up the table for displaying transactions
        String[] columnNames = {"Date", "Description", "Amount ($)", "Type", "Category"};
        tableModel = new DefaultTableModel(columnNames, 0);
        transactionTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(transactionTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Input form panel at the top
        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        
        inputPanel.add(new JLabel("Description:"));
        descriptionField = new JTextField();
        inputPanel.add(descriptionField);

        inputPanel.add(new JLabel("Amount:"));
        amountField = new JTextField();
        inputPanel.add(amountField);

        inputPanel.add(new JLabel("Type:"));
        String[] transactionTypes = {"income", "expense"};
        typeComboBox = new JComboBox<>(transactionTypes);
        inputPanel.add(typeComboBox);

        inputPanel.add(new JLabel("Category:"));
        categoryField = new JTextField();
        inputPanel.add(categoryField);
        
        inputPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        dateField = new JTextField(LocalDate.now().format(DATE_FORMATTER));
        inputPanel.add(dateField);

        JButton addButton = new JButton("Add Transaction");
        addButton.addActionListener(new AddTransactionListener());
        inputPanel.add(addButton);
        
        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // Control panel for buttons at the bottom
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton summaryButton = new JButton("View Summary");
        summaryButton.addActionListener(e -> viewSummary());
        controlPanel.add(summaryButton);

        JButton saveButton = new JButton("Save & Exit");
        saveButton.addActionListener(e -> {
            saveTransactionsToFile();
            System.exit(0);
        });
        controlPanel.add(saveButton);
        
        JButton filterCategoryButton = new JButton("Filter by Category");
        filterCategoryButton.addActionListener(e -> filterByCategory());
        controlPanel.add(filterCategoryButton);
        
        JButton filterDateButton = new JButton("Filter by Date Range");
        filterDateButton.addActionListener(e -> filterByDateRange());
        controlPanel.add(filterDateButton);
        
        JButton clearFilterButton = new JButton("Clear Filters");
        clearFilterButton.addActionListener(e -> updateTableData(transactions));
        controlPanel.add(clearFilterButton);

        mainPanel.add(controlPanel, BorderLayout.SOUTH);

        // Add the main panel to the frame
        add(mainPanel);
        
        // Initially populate the table with loaded data
        updateTableData(transactions);
    }

    /**
     * ActionListener for the "Add Transaction" button.
     */
    private class AddTransactionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                String description = descriptionField.getText().trim();
                double amount = Double.parseDouble(amountField.getText());
                String type = (String) typeComboBox.getSelectedItem();
                String category = categoryField.getText().trim();
                LocalDate date = LocalDate.parse(dateField.getText(), DATE_FORMATTER);

                if (description.isEmpty() || category.isEmpty()) {
                    JOptionPane.showMessageDialog(FinanceTrackerGUI.this,
                            "Description and Category cannot be empty.",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                Transaction newTransaction = new Transaction(description, amount, type, category, date);
                transactions.add(newTransaction);
                updateTableData(transactions); // Refresh the table
                
                // Clear input fields
                descriptionField.setText("");
                amountField.setText("");
                categoryField.setText("");
                dateField.setText(LocalDate.now().format(DATE_FORMATTER));
                
            } catch (NumberFormatException | DateTimeParseException ex) {
                JOptionPane.showMessageDialog(FinanceTrackerGUI.this,
                        "Invalid amount or date format. Please check your input.",
                        "Input Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Updates the JTable with a new list of transactions.
     *
     * @param transactionList The list of transactions to display.
     */
    private void updateTableData(List<Transaction> transactionList) {
        tableModel.setRowCount(0); // Clear existing data
        for (Transaction t : transactionList) {
            Vector<Object> row = new Vector<>();
            row.add(t.getDate().format(DATE_FORMATTER));
            row.add(t.getDescription());
            row.add(String.format("%.2f", t.getAmount()));
            row.add(t.getType());
            row.add(t.getCategory());
            tableModel.addRow(row);
        }
    }

    /**
     * Calculates and displays a financial summary in a message dialog.
     */
    private void viewSummary() {
        double totalIncome = transactions.stream()
                .filter(t -> t.getType().equals("income"))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double totalExpenses = transactions.stream()
                .filter(t -> t.getType().equals("expense"))
                .mapToDouble(Transaction::getAmount)
                .sum();

        double balance = totalIncome - totalExpenses;

        String summary = String.format("<html><body><h2>Financial Summary</h2>" +
                "<p>Total Income: <b>$%.2f</b></p>" +
                "<p>Total Expenses: <b>$%.2f</b></p>" +
                "<p>Current Balance: <b>$%.2f</b></p></body></html>",
                totalIncome, totalExpenses, balance);

        JOptionPane.showMessageDialog(this, summary, "Financial Summary", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Prompts the user for a category and filters the table.
     */
    private void filterByCategory() {
        String category = JOptionPane.showInputDialog(this, "Enter category to filter by:");
        if (category != null && !category.trim().isEmpty()) {
            List<Transaction> filtered = transactions.stream()
                    .filter(t -> t.getCategory().equalsIgnoreCase(category.trim()))
                    .collect(Collectors.toList());
            updateTableData(filtered);
        }
    }
    
    /**
     * Prompts the user for a date range and filters the table.
     */
    private void filterByDateRange() {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField startDateField = new JTextField(LocalDate.now().format(DATE_FORMATTER));
        JTextField endDateField = new JTextField(LocalDate.now().format(DATE_FORMATTER));
        panel.add(new JLabel("Start Date (YYYY-MM-DD):"));
        panel.add(startDateField);
        panel.add(new JLabel("End Date (YYYY-MM-DD):"));
        panel.add(endDateField);
        
        int result = JOptionPane.showConfirmDialog(this, panel, "Enter Date Range", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                LocalDate startDate = LocalDate.parse(startDateField.getText(), DATE_FORMATTER);
                LocalDate endDate = LocalDate.parse(endDateField.getText(), DATE_FORMATTER);
                
                List<Transaction> filtered = transactions.stream()
                    .filter(t -> !t.getDate().isBefore(startDate) && !t.getDate().isAfter(endDate))
                    .collect(Collectors.toList());
                
                updateTableData(filtered);
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Please use YYYY-MM-DD.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Saves all transactions to a CSV file.
     */
    private void saveTransactionsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE))) {
            // Write header
            writer.write("date,description,amount,type,category\n");
            for (Transaction t : transactions) {
                writer.write(t.toCsvString() + "\n");
            }
            JOptionPane.showMessageDialog(this, "Transactions saved successfully!", "Save Status", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving transactions to file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads transactions from a CSV file.
     */
    private void loadTransactionsFromFile() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            System.out.println("No data file found. Starting with an empty tracker.");
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    try {
                        LocalDate date = LocalDate.parse(parts[0], DATE_FORMATTER);
                        String description = parts[1];
                        double amount = Double.parseDouble(parts[2]);
                        String type = parts[3];
                        String category = parts[4];
                        transactions.add(new Transaction(description, amount, type, category, date));
                    } catch (NumberFormatException | DateTimeParseException e) {
                        System.err.println("Skipping malformed line in data file: " + line);
                    }
                }
            }
            System.out.println("Loaded " + transactions.size() + " transactions from file.");
        } catch (IOException e) {
            System.err.println("Error loading transactions from file: " + e.getMessage());
        }
    }

    /**
     * Represents a single financial transaction.
     */
    private static class Transaction {
        private final String description;
        private final double amount;
        private final String type; // "income" or "expense"
        private final String category;
        private final LocalDate date;

        public Transaction(String description, double amount, String type, String category, LocalDate date) {
            this.description = description;
            this.amount = amount;
            this.type = type;
            this.category = category;
            this.date = date;
        }

        // Getter for the description field, which was missing
        public String getDescription() {
            return description;
        }

        public double getAmount() {
            return amount;
        }

        public String getType() {
            return type;
        }

        public String getCategory() {
            return category;
        }
        
        public LocalDate getDate() {
            return date;
        }

        public String toCsvString() {
            return String.join(",", date.format(DATE_FORMATTER), description, String.valueOf(amount), type, category);
        }
    }
}