package fr.classcord.ui;

import fr.classcord.network.ClientInvite;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ChatFrame extends JFrame {
    private String username;
    private JTextField messageField;
    private JButton sendButton;
    private JPanel messagePanel;
    private JScrollPane scrollPane;

    private ClientInvite client;

    // Map pour stocker la couleur de chaque utilisateur
    private Map<String, Color> userColors = new HashMap<>();

    public ChatFrame(ClientInvite client, String username) {
        this.username = username;
        this.client = client;

        setTitle("Classcord - Connecté en tant que " + username);
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // Zone messages
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(messagePanel);
        add(scrollPane, BorderLayout.CENTER);

        // Zone envoi
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Envoyer");

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        // Ajout du listener pour recevoir les messages
        client.addMessageListener(message -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    JSONObject json = new JSONObject(message);
                    if (json.getString("type").equals("message")) {
                        String from = json.getString("from");
                        String content = json.getString("content");
                        displayMessage(from, content);
                    }
                } catch (Exception ignored) {
                }
            });
        });

        setVisible(true);
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            try {
                JSONObject json = new JSONObject();
                json.put("type", "message");
                json.put("from", username);
                json.put("content", text);
                client.send(json.toString());

                displayMessage(username, text);
                messageField.setText("");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void displayMessage(String from, String content) {
        boolean isMine = from.equals(username);

        // Panel contenant le message, pour alignement à droite ou gauche
        JPanel messagePanelLine = new JPanel();
        messagePanelLine.setLayout(new BoxLayout(messagePanelLine, BoxLayout.X_AXIS));
        messagePanelLine.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Label avec pseudo + message, couleur noire pour toi, couleur unique pour les autres
        JLabel label = new JLabel(from + " : " + content);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        label.setForeground(isMine ? Color.BLACK : getUserColor(from));

        if (isMine) {
            // Alignement à droite: ajouter "glue" à gauche
            messagePanelLine.add(Box.createHorizontalGlue());
            messagePanelLine.add(label);
        } else {
            // Alignement à gauche: label à gauche + glue à droite
            messagePanelLine.add(label);
            messagePanelLine.add(Box.createHorizontalGlue());
        }

        messagePanelLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.add(messagePanelLine);
        messagePanel.revalidate();
        messagePanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    // Génère ou récupère une couleur unique par utilisateur
    private Color getUserColor(String username) {
        if (userColors.containsKey(username)) {
            return userColors.get(username);
        }
        int hash = username.hashCode();

        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = (hash & 0x0000FF);

        r = 50 + (r % 150);
        g = 50 + (g % 150);
        b = 50 + (b % 150);

        Color color = new Color(r, g, b);
        userColors.put(username, color);
        return color;
    }
}
