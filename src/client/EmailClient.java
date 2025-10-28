package client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

public class EmailClient extends JFrame {
    private String user;
    private String currentPassword;
    private String currentFolder = "inbox";
    private String SERVER_IP = "localhost";
    private int lastEmailCount = 0;
    private JLabel loadingLabel;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"", "Từ", "Tiêu đề", "Thời gian"}, 0) {
        @Override public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextPane preview = new JTextPane();
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");

    public EmailClient() {
        setTitle("Mail App");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        askForServerIP();
        showLogin();
    }

    private void askForServerIP() {
        String input = JOptionPane.showInputDialog(
                this,
                "<html><h3>Kết nối đến máy chủ</h3>" +
                        "<p>Nhập IP máy chủ (ví dụ: 192.168.1.100)</p>" +
                        "<p><i>Để trống = dùng localhost (cùng máy)</i></p></html>",
                "Nhập IP máy chủ",
                JOptionPane.QUESTION_MESSAGE
        );

        if (input != null && !input.trim().isEmpty() && input.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            SERVER_IP = input.trim();
            JOptionPane.showMessageDialog(this, "Đã kết nối đến: " + SERVER_IP, "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else {
            SERVER_IP = "localhost";
            JOptionPane.showMessageDialog(this, "Dùng localhost (cùng máy)", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // MÀN HÌNH ĐĂNG NHẬP + GIF LÀM NỀN + CHỮ CÓ BÓNG
    private void showLogin() {
        getContentPane().removeAll();

        BackgroundPanel background = new BackgroundPanel("/resource/PlaneGif.gif");
        background.setLayout(new GridBagLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2, true),
                new EmptyBorder(25, 35, 25, 35)
        ));
        formPanel.setPreferredSize(new Dimension(380, 260));

        JTextField userField = new JTextField(18);
        userField.setBackground(new Color(255, 255, 255, 240));
        userField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                new EmptyBorder(10, 10, 10, 10)
        ));
        userField.setFont(userField.getFont().deriveFont(14f));

        JPasswordField passField = new JPasswordField(18);
        passField.setBackground(new Color(255, 255, 255, 240));
        passField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                new EmptyBorder(10, 10, 10, 10)
        ));
        passField.setFont(passField.getFont().deriveFont(14f));

        JButton loginBtn = new JButton("Đăng nhập");
        loginBtn.setBackground(new Color(30, 144, 255));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(loginBtn.getFont().deriveFont(Font.BOLD, 14f));
        loginBtn.setFocusPainted(false);
        loginBtn.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        JButton regBtn = new JButton("Đăng ký");
        regBtn.setBackground(new Color(34, 139, 34));
        regBtn.setForeground(Color.WHITE);
        regBtn.setFont(regBtn.getFont().deriveFont(Font.BOLD, 14f));
        regBtn.setFocusPainted(false);
        regBtn.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // DÒNG 1
        gbc.gridx = 0; gbc.gridy = 0;
        addShadowLabel(formPanel, "Tên người dùng:", gbc);
        gbc.gridx = 1; formPanel.add(userField, gbc);

        // DÒNG 2
        gbc.gridx = 0; gbc.gridy = 1;
        addShadowLabel(formPanel, "Mật khẩu:", gbc);
        gbc.gridx = 1; formPanel.add(passField, gbc);

        // DÒNG 3
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(loginBtn, gbc);
        gbc.gridx = 1; formPanel.add(regBtn, gbc);

        loginBtn.addActionListener(e -> login(userField.getText(), new String(passField.getPassword())));
        regBtn.addActionListener(e -> register(userField.getText(), new String(passField.getPassword())));

        background.add(formPanel);
        add(background);

        revalidate();
        repaint();
    }

    private void addShadowLabel(JPanel panel, String text, GridBagConstraints gbc) {
        JLabel shadow = new JLabel(text);
        shadow.setForeground(new Color(255, 255, 255, 200));
        shadow.setFont(shadow.getFont().deriveFont(Font.BOLD, 15f));
        gbc.insets = new Insets(10, 11, 10, 9);
        panel.add(shadow, gbc);

        JLabel main = new JLabel(text);
        main.setForeground(Color.BLACK);
        main.setFont(main.getFont().deriveFont(Font.BOLD, 15f));
        gbc.insets = new Insets(10, 10, 10, 10);
        panel.add(main, gbc);
    }

    private void register(String username, String password) {
        if (username.isBlank() || password.isBlank()) {
            JOptionPane.showMessageDialog(this, "Nhập đầy đủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (new server.UserManager().register(username, password)) {
            JOptionPane.showMessageDialog(this, "Đăng ký thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Tên đã tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void login(String username, String password) {
        String savedPass = getPasswordFromFile(username);
        if (savedPass != null && savedPass.equals(password)) {
            user = username;
            currentPassword = password;
            getContentPane().removeAll();
            setupUI();
            revalidate();
            repaint();
        } else {
            JOptionPane.showMessageDialog(this, "Sai tên hoặc mật khẩu!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getPasswordFromFile(String username) {
        try {
            Path path = Path.of("server_users", username, "info.txt");
            if (Files.exists(path)) {
                String line = Files.readString(path).trim();
                String[] parts = line.split(",", 3);
                return parts.length >= 2 ? parts[1].trim() : null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // GIAO DIỆN CHÍNH + BẢNG EMAIL TO + CLICK ĐƯỢC
    private void setupUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

        JLabel welcome = new JLabel("Welcome, " + user + "!");
        welcome.setFont(welcome.getFont().deriveFont(18f));
        welcome.setBorder(new EmptyBorder(10, 15, 10, 0));
        top.add(welcome, BorderLayout.WEST);

        JTextField search = new JTextField(40);
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY),
                new EmptyBorder(8, 8, 8, 8)));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchEmails(search.getText()); }
            public void removeUpdate(DocumentEvent e) { searchEmails(search.getText()); }
            public void changedUpdate(DocumentEvent e) { searchEmails(search.getText()); }
        });
        top.add(search, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // GIF LOADING
        ImageIcon loadingGif = new ImageIcon(getClass().getResource("/resource/PlaneGif.gif"));
        Image scaledLoading = loadingGif.getImage().getScaledInstance(80, 80, Image.SCALE_DEFAULT);
        ImageIcon smallLoading = new ImageIcon(scaledLoading);
        loadingLabel = smallLoading.getIconWidth() == -1 ? new JLabel("Loading...") : new JLabel(smallLoading);
        loadingLabel.setHorizontalAlignment(JLabel.CENTER);
        loadingLabel.setVisible(false);

        // BẢNG EMAIL – CHỮ TO + CLICK ĐƯỢC
        JScrollPane tableScroll = new JScrollPane(table);
        table.setRowHeight(64);

        // TIÊU ĐỀ BẢNG
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, 16f));

        // RENDERER MỚI – GIỮ LẠI CHỨC NĂNG CLICK
        TableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setFont(label.getFont().deriveFont(16f));
                label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                label.setHorizontalAlignment(column == 3 ? JLabel.CENTER : JLabel.LEFT);
                return label;
            }
        };
        table.setDefaultRenderer(Object.class, customRenderer);
        table.setDefaultRenderer(Boolean.class, table.getDefaultRenderer(Boolean.class)); // Giữ checkbox

        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(3).setMaxWidth(100);

        // CLICK ĐƯỢC EMAIL
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    loadEmailContent(row);
                }
            }
        });

        JScrollPane previewScroll = new JScrollPane(preview);
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.add(previewScroll, BorderLayout.CENTER);
        previewPanel.add(loadingLabel, BorderLayout.SOUTH);

        JSplitPane contentSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, previewPanel);
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createSidebar(), contentSplit);
        main.setDividerLocation(200);
        contentSplit.setDividerLocation(500);
        add(main);

        loadEmailsList();
        startSmartTimer();
    }

    private JPanel createSidebar() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(250, 250, 250));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));

        JButton compose = new JButton("Soạn thư");
        compose.setBackground(new Color(220, 53, 69));
        compose.setForeground(Color.BLACK);
        compose.setFont(compose.getFont().deriveFont(16f));
        compose.setFocusPainted(false);
        compose.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        compose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        compose.setOpaque(true);
        compose.addActionListener(e -> showComposeDialog());
        p.add(compose);

        p.add(createFolderItem("Hộp thư đến", "inbox", true));
        p.add(createFolderItem("Đã gửi", "sent", false));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createFolderItem(String text, String folder, boolean active) {
        JPanel item = new JPanel(new BorderLayout());
        item.setMaximumSize(new Dimension(180, 56));
        item.setBackground(active ? new Color(66, 133, 244) : new Color(250, 250, 250));
        item.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel label = new JLabel(text);
        label.setForeground(active ? Color.WHITE : Color.DARK_GRAY);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));

        item.add(label);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentFolder = folder;
                reloadSidebar();
                loadEmailsList();
            }
        });
        return item;
    }

    private void reloadSidebar() {
        JPanel sidebar = (JPanel) ((JSplitPane) getContentPane().getComponent(1)).getLeftComponent();
        sidebar.removeAll();
        JButton compose = new JButton("Soạn thư");
        compose.setBackground(new Color(220, 53, 69));
        compose.setForeground(Color.BLACK);
        compose.setFont(compose.getFont().deriveFont(16f));
        compose.setFocusPainted(false);
        compose.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        compose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        compose.setOpaque(true);
        compose.addActionListener(e -> showComposeDialog());
        sidebar.add(compose);
        sidebar.add(createFolderItem("Hộp thư đến", "inbox", currentFolder.equals("inbox")));
        sidebar.add(createFolderItem("Đã gửi", "sent", currentFolder.equals("sent")));
        sidebar.add(Box.createVerticalGlue());
        sidebar.revalidate();
        sidebar.repaint();
    }

    private void startSmartTimer() {
        new Timer(3000, e -> {
            int current = getEmailCount();
            if (current != lastEmailCount) {
                loadEmailsList();
                lastEmailCount = current;
            }
        }).start();
    }

    private int getEmailCount() {
        try {
            Path folder = Path.of("server_users", user, currentFolder);
            if (!Files.exists(folder)) return 0;
            return (int) Files.list(folder).filter(f -> f.toString().endsWith(".eml")).count();
        } catch (Exception e) {
            return lastEmailCount;
        }
    }

    private void loadEmailsList() {
        SwingUtilities.invokeLater(() -> loadingLabel.setVisible(true));
        searchEmails("");
    }

    private void searchEmails(String keyword) {
        tableModel.setRowCount(0);
        if (keyword == null || keyword.trim().isEmpty()) {
            loadAllEmails();
            return;
        }

        final String finalKeyword = keyword.toLowerCase();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    Path folder = Path.of("server_users", user, currentFolder);
                    if (!Files.exists(folder)) return null;

                    var files = Files.list(folder)
                            .filter(f -> f.toString().endsWith(".eml"))
                            .sorted(Comparator.comparingLong(f -> {
                                try { return Files.getLastModifiedTime(f).toMillis(); }
                                catch (Exception e) { return 0; }
                            })).toList();

                    for (int i = 0; i < files.size(); i++) {
                        String content = Files.readString(files.get(i)).toLowerCase();
                        String from = "Unknown", subj = "No Subject";

                        for (String line : content.split("\n")) {
                            if (line.startsWith("from: ")) from = line.substring(6).trim();
                            if (line.startsWith("subject: ")) subj = line.substring(9).trim();
                            if (line.trim().isEmpty()) break;
                        }

                        if (from.toLowerCase().contains(finalKeyword) ||
                                subj.toLowerCase().contains(finalKeyword) ||
                                content.contains(finalKeyword)) {

                            final String f = from, s = subj;
                            SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{
                                    false, f, s, timeFmt.format(new Date())
                            }));
                        }
                    }
                } catch (Exception ignored) {}
                return null;
            }
        }.execute();
    }

    private void loadAllEmails() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    Path folder = Path.of("server_users", user, currentFolder);
                    if (!Files.exists(folder)) return null;

                    var files = Files.list(folder)
                            .filter(f -> f.toString().endsWith(".eml"))
                            .sorted(Comparator.comparingLong(f -> {
                                try { return Files.getLastModifiedTime(f).toMillis(); }
                                catch (Exception e) { return 0; }
                            })).toList();

                    for (int i = 0; i < files.size(); i++) {
                        String content = Files.readString(files.get(i));
                        String from = "Unknown", subj = "No Subject";
                        for (String line : content.split("\n")) {
                            if (line.startsWith("From: ")) from = line.substring(6).trim();
                            if (line.startsWith("Subject: ")) subj = line.substring(9).trim();
                            if (line.trim().isEmpty()) break;
                        }
                        final String f = from, s = subj;
                        SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{
                                false, f, s, timeFmt.format(new Date())
                        }));
                    }
                } catch (Exception ignored) {}
                return null;
            }
        }.execute();
    }

    private void loadEmailContent(int row) {
        SwingUtilities.invokeLater(() -> {
            preview.setText("");
            preview.setContentType("text/html");
            loadingLabel.setVisible(true);
        });

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    Path folder = Path.of("server_users", user, currentFolder);
                    var files = Files.list(folder)
                            .filter(f -> f.toString().endsWith(".eml"))
                            .sorted(Comparator.comparingLong(f -> {
                                try { return Files.getLastModifiedTime(f).toMillis(); }
                                catch (Exception e) { return 0; }
                            })).toList();
                    if (row >= files.size()) return "Lỗi.";
                    return Files.readString(files.get(row));
                } catch (Exception e) { return "Lỗi tải email."; }
            }
            @Override
            protected void done() {
                try {
                    String fullEmail = get();
                    String[] lines = fullEmail.split("\n");
                    StringBuilder header = new StringBuilder();
                    StringBuilder body = new StringBuilder();
                    boolean inBody = false;

                    for (String line : lines) {
                        if (line.trim().isEmpty()) { inBody = true; continue; }
                        if (!inBody) header.append(line).append("\n");
                        else body.append(line).append("\n");
                    }

                    String html = "<!DOCTYPE html>" +
                            "<html><body style='font-family: Arial, sans-serif; padding:20px;'>" +
                            "<h2 style='margin:0 0 12px; font-size:20px'>" + escapeHtml(extractSubject(header.toString())) + "</h2>" +
                            "<p style='margin:5px 0; color:#555; font-size:15px'><b>Từ:</b> " + escapeHtml(extractFrom(header.toString())) + "</p>" +
                            "<p style='margin:5px 0; color:#555; font-size:15px'><b>Đến:</b> " + escapeHtml(extractTo(header.toString())) + "</p>" +
                            "<hr style='border:1px solid #eee; margin:20px 0'>" +
                            "<pre style='white-space: pre-wrap; font-size:15px; margin:0'>" + escapeHtml(body.toString().trim()) + "</pre>" +
                            "</body></html>";

                    SwingUtilities.invokeLater(() -> preview.setText(html));
                } catch (Exception ignored) {}
                finally {
                    SwingUtilities.invokeLater(() -> loadingLabel.setVisible(false));
                }
            }
        }.execute();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\'", "&#39;");
    }

    private void showComposeDialog() {
        JDialog d = new JDialog(this, "Soạn thư", true);
        d.setSize(600, 500);
        d.setLocationRelativeTo(this);

        JTextField to = new JTextField(30);
        JTextField subj = new JTextField(30);
        JTextArea body = new JTextArea(15, 50);
        JButton send = new JButton("Gửi");

        send.setFont(send.getFont().deriveFont(16f));

        send.addActionListener(e -> {
            String toText = to.getText().trim();
            String subjText = subj.getText().trim();
            String bodyText = body.getText();

            if (toText.isEmpty() || subjText.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Nhập người nhận và tiêu đề!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            new Thread(() -> {
                boolean sent = connect(SERVER_IP, 2525,
                        "HELO " + SERVER_IP,
                        "MAIL FROM:<" + user + ">",
                        "RCPT TO:<" + toText + ">",
                        "DATA",
                        "From: " + user + "\nTo: " + toText + "\nSubject: " + subjText + "\n\n" + bodyText,
                        "."
                );

                SwingUtilities.invokeLater(() -> {
                    d.dispose();
                    if (sent) {
                        JOptionPane.showMessageDialog(EmailClient.this, "Đã gửi!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        loadEmailsList();
                    } else {
                        JOptionPane.showMessageDialog(EmailClient.this, "Gửi thất bại! Kiểm tra IP/server.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        });

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; p.add(new JLabel("Đến:"), gbc);
        gbc.gridx = 1; p.add(to, gbc);
        gbc.gridx = 0; gbc.gridy = 1; p.add(new JLabel("Chủ đề:"), gbc);
        gbc.gridx = 1; p.add(subj, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; p.add(new JScrollPane(body), gbc);
        gbc.gridy = 3; gbc.gridwidth = 1; p.add(send, gbc);
        d.add(p);
        d.setVisible(true);
    }

    private boolean connect(String host, int port, String... cmds) {
        Socket s = null;
        try {
            s = new Socket();
            s.connect(new InetSocketAddress(host, port), 5000);
            s.setSoTimeout(5000);

            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            String resp = in.readLine();
            if (resp == null || !resp.startsWith("2")) return false;

            for (int i = 0; i < cmds.length; i++) {
                String cmd = cmds[i];
                out.println(cmd);

                if (cmd.equals(".")) break;

                if (cmd.equals("DATA")) {
                    resp = in.readLine();
                    if (resp == null || !resp.startsWith("354")) return false;
                    continue;
                }

                if (i < cmds.length - 1 && !cmds[i + 1].equals(".")) {
                    resp = in.readLine();
                    if (resp == null || resp.startsWith("5")) return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (s != null) try { s.close(); } catch (Exception ignored) {}
        }
    }

    private String extractFrom(String header) {
        for (String line : header.split("\n")) {
            if (line.startsWith("From: ")) return line.substring(6).trim();
        }
        return "Không rõ";
    }

    private String extractTo(String header) {
        for (String line : header.split("\n")) {
            if (line.startsWith("To: ")) return line.substring(4).trim();
        }
        return "Không rõ";
    }

    private String extractSubject(String header) {
        for (String line : header.split("\n")) {
            if (line.startsWith("Subject: ")) return line.substring(9).trim();
        }
        return "Không có tiêu đề";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EmailClient().setVisible(true));
    }
}