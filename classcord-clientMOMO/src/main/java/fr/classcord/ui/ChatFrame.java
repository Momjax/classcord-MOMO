package fr.classcord.ui;

import fr.classcord.network.ClientInvite;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ChatFrame extends JFrame {

    private String username;
    private JTextField messageField;
    private JButton sendButton;
    private JPanel messagePanel;
    private JScrollPane scrollPane;

    private ClientInvite client;

    private Set<String> onlineUsers = new HashSet<>();

    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JList<String> userList = new JList<>(userListModel);

    private JLabel conversationLabel;

    public ChatFrame(ClientInvite client, String username) {
        this.client = client;
        this.username = username;

        setTitle("Classcord - Connecté en tant que " + username);
        setSize(700, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Zone affichage des messages
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(messagePanel);
        add(scrollPane, BorderLayout.CENTER);

        // Liste utilisateurs
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane userListScroll = new JScrollPane(userList);
        userListScroll.setPreferredSize(new Dimension(150, 0));
        add(userListScroll, BorderLayout.EAST);

        // Panel du bas (saisie + bouton)
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Envoyer");
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Label au-dessus des messages
        conversationLabel = new JLabel("Discussion : Global");
        conversationLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(conversationLabel, BorderLayout.NORTH);

        // Action envoyer message
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        // Listener changement de sélection
        userList.addListSelectionListener(e -> updateConversationLabel());

        // Écoute des messages reçus
        client.addMessageListener(message -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");

                    switch (type) {
                        case "message":
                            handleMessage(json);
                            break;
                        case "status":
                            handleStatus(json);
                            break;
                        case "users":
                            handleListUsersResponse(json);
                            break;
                    }
                } catch (Exception ignored) {}
            });
        });

        // Ajoute l'utilisateur lui-même
        onlineUsers.add(username);
        refreshUserList();

        // Demande la liste des utilisateurs connectés
        requestOnlineUsersList();

        setVisible(true);
    }

    private void requestOnlineUsersList() {
        try {
            JSONObject request = new JSONObject();
            request.put("type", "users");
            client.send(request.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleListUsersResponse(JSONObject json) {
        onlineUsers.clear();
        onlineUsers.add(username); // sécurité
        for (Object obj : json.getJSONArray("users")) {
            String user = (String) obj;
            onlineUsers.add(user);
        }
        refreshUserList();
    }

    private void handleMessage(JSONObject json) {
        String from = json.getString("from");
        String content = json.getString("content");
        String subtype = json.optString("subtype", "global");

        if ("private".equals(subtype)) {
            String to = json.optString("to", "");
            if (from.equals(username) || to.equals(username)) {
                displayMessage("MP de " + from, content, "private");
            }
        } else {
            displayMessage(from, content, "global");
        }
    }

    private void handleStatus(JSONObject json) {
        String user = json.getString("user");
        String state = json.getString("state");

        if ("online".equals(state)) {
            onlineUsers.add(user);
        } else if ("offline".equals(state)) {
            onlineUsers.remove(user);
        }
        refreshUserList();
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        JSONObject json = new JSONObject();
        json.put("type", "message");
        json.put("from", username);
        json.put("content", text);

        String selectedUser = userList.getSelectedValue();

        if (selectedUser != null && !selectedUser.equals("Global") && !selectedUser.equals(username)) {
            json.put("subtype", "private");
            json.put("to", selectedUser);
            displayMessage("Moi → " + selectedUser, text, "private");
        } else {
            json.put("subtype", "global");
            displayMessage(username, text, "global");
        }

        try {
            client.send(json.toString());
            messageField.setText("");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors de l'envoi du message", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayMessage(String from, String content, String subtype) {
        JPanel messageLine = new JPanel();
        messageLine.setLayout(new BoxLayout(messageLine, BoxLayout.X_AXIS));
        messageLine.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        boolean isMine = from.startsWith("Moi") || from.equals(username);
        JLabel label;

        if ("private".equals(subtype)) {
            label = new JLabel("**[MP] " + from + ": " + content);
            label.setForeground(Color.MAGENTA);
            label.setFont(new Font("Arial", Font.ITALIC, 14));
        } else {
            label = new JLabel(from + " : " + content);
            label.setForeground(isMine ? Color.BLACK : Color.BLUE);
            label.setFont(new Font("Arial", Font.PLAIN, 14));
        }

        if (isMine) {
            messageLine.add(Box.createHorizontalGlue());
            messageLine.add(label);
        } else {
            messageLine.add(label);
            messageLine.add(Box.createHorizontalGlue());
        }

        messageLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.add(messageLine);
        messagePanel.revalidate();
        messagePanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void refreshUserList() {
        userListModel.clear();

        // Ajoute "Global" tout en haut
        userListModel.addElement("Global");

        List<String> sortedUsers = new ArrayList<>(onlineUsers);
        Collections.sort(sortedUsers);

        for (String user : sortedUsers) {
            userListModel.addElement(user);
        }

        // Sélectionne "Global" par défaut
        if (userList.getSelectedValue() == null) {
            userList.setSelectedIndex(0);
        }

        updateConversationLabel();
    }

    private void updateConversationLabel() {
        String selected = userList.getSelectedValue();
        if (selected == null || selected.equals("Global")) {
            conversationLabel.setText("Discussion : Global");
        } else {
            conversationLabel.setText("Discussion : MP avec " + selected);
        }
    }
}
