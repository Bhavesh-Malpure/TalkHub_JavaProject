import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatClientGUI extends JFrame {

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;

    private JTextPane chatPane;
    private StyledDocument doc;
    private JTextField messageField;
    private JButton sendButton;
    private JCheckBox autoScrollBox;

    private DefaultListModel<String> userListModel;
    private JList<String> usersList;

    private Map<String, PrivateChatWindow> privateChats = new HashMap<>();

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public ChatClientGUI(Socket socket, String username) {
        this.socket = socket;
        this.username = username;

        try {
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer.write(username);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            closeEverything();
        }

        setupGUI();
        listenForMessages();
    }

    private void setupGUI() {
        setTitle("Group Chat - " + username);
        setSize(800, 600);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel chatHeader = new JLabel("Welcome, " + username + "  |  Group Chat");
        chatHeader.setOpaque(true);
        chatHeader.setBackground(new Color(70, 130, 180));
        chatHeader.setForeground(Color.WHITE);
        chatHeader.setFont(new Font("Arial", Font.BOLD, 18));
        chatHeader.setHorizontalAlignment(SwingConstants.CENTER);
        chatHeader.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(chatHeader, BorderLayout.NORTH);

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("SansSerif", Font.PLAIN, 14));
        doc = chatPane.getStyledDocument();
        JScrollPane chatScrollPane = new JScrollPane(chatPane);

        userListModel = new DefaultListModel<>();
        usersList = new JList<>(userListModel);
        usersList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JScrollPane usersScrollPane = new JScrollPane(usersList);
        usersScrollPane.setPreferredSize(new Dimension(200, 0));
        usersScrollPane.setBorder(BorderFactory.createTitledBorder("Online Users"));

        usersList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String target = usersList.getSelectedValue();
                    if (target != null && !target.equals(username)) {
                        openPrivateChat(target);
                    }
                }
            }
        });

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatScrollPane, usersScrollPane);
        splitPane.setDividerLocation(580);
        splitPane.setResizeWeight(1.0);
        add(splitPane, BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new BorderLayout(10, 10));
        messageField = new JTextField();
        messageField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        messageField.setPreferredSize(new Dimension(400, 40));

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        sendButton.setPreferredSize(new Dimension(100, 40));

        autoScrollBox = new JCheckBox("Auto-scroll", true);
        autoScrollBox.setFont(new Font("SansSerif", Font.PLAIN, 12));

        messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        messagePanel.add(autoScrollBox, BorderLayout.SOUTH);
        add(messagePanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendGroupMessage());
        messageField.addActionListener(e -> sendGroupMessage());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                        null,
                        "Are you sure you want to exit the chat?",
                        "Exit Confirmation",
                        JOptionPane.YES_NO_OPTION
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    closeEverything();
                    System.exit(0);
                }
            }
        });

        setVisible(true);
    }

    private void sendGroupMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                String fullMessage = username + ": " + message;
                writer.write(fullMessage);
                writer.newLine();
                writer.flush();
                appendMessage(username, message, true);
                messageField.setText("");
            } catch (IOException e) {
                appendServerMessage("Server : Failed to send message.");
                closeEverything();
            }
        }
    }

    private void openPrivateChat(String targetUser) {
        PrivateChatWindow win = privateChats.get(targetUser);
        if (win == null) {
            win = new PrivateChatWindow(targetUser);
            privateChats.put(targetUser, win);
        }
        win.setVisible(true);
        win.toFront();
    }

    private void listenForMessages() {
        new Thread(() -> {
            String msg;
            try {
                while ((msg = reader.readLine()) != null) {
                    if (msg.startsWith("__USER_LIST__:")) {
                        updateUserList(msg.substring("__USER_LIST__:".length()));
                    } else if (msg.startsWith("__DM__:")) {
                        handleIncomingDM(msg);
                    } else if (msg.startsWith("Server")) {
                        appendServerMessage(msg);
                    } else {
                        int index = msg.indexOf(":");
                        if (index != -1) {
                            String sender = msg.substring(0, index);
                            String content = msg.substring(index + 1).trim();
                            appendMessage(sender, content, false);
                        } else {
                            appendMessage("Unknown", msg, false);
                        }
                    }
                }
            } catch (IOException e) {
                appendServerMessage("Server : Connection closed.");
                closeEverything();
            }
        }).start();
    }

    private void handleIncomingDM(String raw) {
        String body = raw.substring("__DM__:".length());
        int first = body.indexOf(':');
        if (first == -1) return;
        String otherUser = body.substring(0, first).trim();
        String message = body.substring(first + 1).trim();

        SwingUtilities.invokeLater(() -> {
            PrivateChatWindow win = privateChats.get(otherUser);
            if (win == null) {
                win = new PrivateChatWindow(otherUser);
                privateChats.put(otherUser, win);
            }
            win.appendIncomingOrEcho(otherUser, message);
            win.setVisible(true);
        });
    }

    private void updateUserList(String csvUsernames) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            if (!csvUsernames.trim().isEmpty()) {
                String[] users = csvUsernames.split(",");
                for (String u : users) {
                    userListModel.addElement(u.trim());
                }
            }
        });
    }

    private void appendMessage(String sender, String message, boolean isSelf) {
        try {
            Style userStyle = chatPane.addStyle("UserStyle", null);
            StyleConstants.setBold(userStyle, true);
            StyleConstants.setFontSize(userStyle, 14);
            if (isSelf) {
                StyleConstants.setForeground(userStyle, Color.BLUE);
                sender = "You";
            }

            Style msgStyle = chatPane.addStyle("MsgStyle", null);
            StyleConstants.setFontSize(msgStyle, 14);

            Style timeStyle = chatPane.addStyle("TimeStyle", null);
            StyleConstants.setFontSize(timeStyle, 10);
            StyleConstants.setForeground(timeStyle, Color.GRAY);

            String timestamp = " [" + timeFormat.format(new Date()) + "]";
            doc.insertString(doc.getLength(), sender + ": ", userStyle);
            doc.insertString(doc.getLength(), message, msgStyle);
            doc.insertString(doc.getLength(), timestamp + "\n", timeStyle);

            if (autoScrollBox.isSelected()) {
                chatPane.setCaretPosition(doc.getLength());
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void appendServerMessage(String msg) {
        try {
            Style serverStyle = chatPane.addStyle("ServerStyle", null);
            StyleConstants.setItalic(serverStyle, true);
            StyleConstants.setForeground(serverStyle, Color.MAGENTA);
            StyleConstants.setFontSize(serverStyle, 12);

            String timestamp = " [" + timeFormat.format(new Date()) + "]";
            doc.insertString(doc.getLength(), msg + timestamp + "\n", serverStyle);

            if (autoScrollBox.isSelected()) {
                chatPane.setCaretPosition(doc.getLength());
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void closeEverything() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    // ---------- Private Chat Window (peach themed with header) ----------
    private class PrivateChatWindow extends JFrame {
        private final String otherUser;
        private final JTextPane dmPane;
        private final StyledDocument dmDoc;
        private final JTextField dmField;
        private final JButton dmSendBtn;

        private final Deque<String> lastSent = new ArrayDeque<>(10);

        private final Color BG_PEACH     = new Color(0xFF, 0xE9, 0xDC); // #FFE9DC
        private final Color HEADER_PEACH = new Color(0xFF, 0xC7, 0xA9); // #FFC7A9
        private final Color YOU_COLOR    = new Color(0xE6, 0x72, 0x4C); // #E6724C
        private final Color OTHER_COLOR  = new Color(0xC7, 0x5B, 0x39); // #C75B39
        private final Color TIME_COLOR   = new Color(0x8E, 0x6E, 0x63); // #8E6E63

        PrivateChatWindow(String otherUser) {
            this.otherUser = otherUser;
            setTitle("Private Chat with " + otherUser);
            setSize(400, 400);
            setLayout(new BorderLayout());
            setDefaultCloseOperation(HIDE_ON_CLOSE);

            JLabel privateHeader = new JLabel(" Private Chat with " + otherUser);
            privateHeader.setOpaque(true);
            privateHeader.setBackground(HEADER_PEACH);
            privateHeader.setForeground(Color.DARK_GRAY);
            privateHeader.setFont(new Font("Arial", Font.BOLD, 15));
            privateHeader.setHorizontalAlignment(SwingConstants.LEFT);
            privateHeader.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            add(privateHeader, BorderLayout.NORTH);

            dmPane = new JTextPane();
            dmPane.setEditable(false);
            dmPane.setBackground(BG_PEACH);
            dmDoc = dmPane.getStyledDocument();
            JScrollPane scroll = new JScrollPane(dmPane);

            JPanel bottom = new JPanel(new BorderLayout(5, 5));
            dmField = new JTextField();
            dmField.setFont(new Font("SansSerif", Font.PLAIN, 14));
            dmSendBtn = new JButton("Send");
            dmSendBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

            bottom.add(dmField, BorderLayout.CENTER);
            bottom.add(dmSendBtn, BorderLayout.EAST);
            bottom.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

            dmSendBtn.addActionListener(e -> sendDM());
            dmField.addActionListener(e -> sendDM());

            add(scroll, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
        }

        private void sendDM() {
            String msg = dmField.getText().trim();
            if (msg.isEmpty()) return;
            try {
                writer.write("__DM__:" + otherUser + ":" + msg);
                writer.newLine();
                writer.flush();

                appendDM("You", msg, true);
                rememberSent(msg);
                dmField.setText("");
            } catch (IOException e) {
                appendDM("Server", "Failed to send DM.", false);
            }
        }

        void appendIncomingOrEcho(String serverOtherUser, String message) {
            if (wasJustSent(message)) {
                return;
            }
            appendDM(serverOtherUser, message, false);
        }

        private void appendDM(String sender, String message, boolean isSelf) {
            try {
                Style userStyle = dmPane.addStyle("dmUserStyle", null);
                StyleConstants.setBold(userStyle, true);
                StyleConstants.setFontSize(userStyle, 14);
                StyleConstants.setForeground(userStyle, isSelf ? YOU_COLOR : OTHER_COLOR);

                Style msgStyle = dmPane.addStyle("dmMsgStyle", null);
                StyleConstants.setFontSize(msgStyle, 14);

                Style timeStyle = dmPane.addStyle("dmTimeStyle", null);
                StyleConstants.setFontSize(timeStyle, 10);
                StyleConstants.setForeground(timeStyle, TIME_COLOR);

                String timestamp = " [" + timeFormat.format(new Date()) + "]";
                dmDoc.insertString(dmDoc.getLength(), sender + ": ", userStyle);
                dmDoc.insertString(dmDoc.getLength(), message, msgStyle);
                dmDoc.insertString(dmDoc.getLength(), timestamp + "\n", timeStyle);

                dmPane.setCaretPosition(dmDoc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        private void rememberSent(String msg) {
            if (lastSent.size() == 10) lastSent.removeFirst();
            lastSent.addLast(msg);
        }

        private boolean wasJustSent(String msg) {
            return lastSent.remove(msg);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String username = JOptionPane.showInputDialog(null, "Enter username:");
            if (username == null || username.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Username cannot be empty.");
                return;
            }
            try {
                Socket socket = new Socket("localhost", 1234);
                new ChatClientGUI(socket, username.trim());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Unable to connect to server.");
            }
        });
    }
}
