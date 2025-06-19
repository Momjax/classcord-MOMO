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
    private ClientInvite client;
    private JTextField messageField;
    private JPanel messagePanel;
    private JScrollPane scrollPane;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JLabel conversationLabel;
    private JComboBox<String> statusCombo;

    // suivi des statuts en ligne
    private Map<String, String> userStatus = new HashMap<>();

    public ChatFrame(ClientInvite client, String username) {
        this.client = client;
        this.username = username;
        setTitle("Classcord – " + username);
        setSize(700, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top panel: label de discussion + statut
        JPanel top = new JPanel(new BorderLayout());
        conversationLabel = new JLabel("Discussion : Global");
        conversationLabel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        top.add(conversationLabel, BorderLayout.WEST);

        statusCombo = new JComboBox<>(new String[]{"disponible","absent","invisible"});
        statusCombo.addActionListener(e -> sendStatusChange());
        top.add(statusCombo, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Center: messages
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        scrollPane = new JScrollPane(messagePanel);
        add(scrollPane, BorderLayout.CENTER);

        // East: liste utilisateurs
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150,0));
        userList.addListSelectionListener(e -> updateConversationLabel());
        add(userScroll, BorderLayout.EAST);

        // Bottom: saisie + send
        JPanel bottom = new JPanel(new BorderLayout());
        messageField = new JTextField();
        JButton sendBtn = new JButton("Envoyer");
        sendBtn.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        bottom.add(messageField, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // Listener socket
        client.addMessageListener(msg -> SwingUtilities.invokeLater(() -> onSocketMessage(msg)));

        // initialisation
        userStatus.put(username, "disponible");
        refreshUserList();
        requestUsersList();
        setVisible(true);
    }

    private void onSocketMessage(String msg) {
        try {
            JSONObject j = new JSONObject(msg);
            String type = j.getString("type");
            switch (type) {
                case "message": handleIncomingMessage(j); break;
                case "status": handleIncomingStatus(j); break;
                case "users": handleUsersList(j); break;
            }
        } catch (Exception ignored) {}
    }

    private void handleIncomingMessage(JSONObject j) {
        String from = j.getString("from");
        String content = j.getString("content");
        String subtype = j.optString("subtype","global");
        String to = j.optString("to","");
        if ("private".equals(subtype)) {
            if (from.equals(username) || to.equals(username))
                displayMessage("MP de " + from, content, "private");
        } else {
            displayMessage(from, content, "global");
        }
    }

    private void handleIncomingStatus(JSONObject j) {
        String user = j.getString("user");
        String state = j.getString("state");
        if ("offline".equals(state)) userStatus.remove(user);
        else userStatus.put(user, state);
        refreshUserList();
    }

    private void handleUsersList(JSONObject j) {
        userStatus.clear();
        for (Object o : j.getJSONArray("users")) {
            userStatus.put((String)o, "disponible");
        }
        userStatus.put(username, (String)statusCombo.getSelectedItem());
        refreshUserList();
    }

    private void requestUsersList() {
        try {
            client.send(new JSONObject().put("type","users").toString());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void sendStatusChange() {
        String state = (String) statusCombo.getSelectedItem();
        try {
            client.send(new JSONObject()
                .put("type","status")
                .put("state",state)
                .toString());
        } catch (IOException e) { e.printStackTrace(); }
        userStatus.put(username,state);
        refreshUserList();
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        JSONObject json = new JSONObject()
            .put("type","message")
            .put("from",username)
            .put("content",text);
        String sel = userList.getSelectedValue();
        if (sel!=null && !sel.equals("Global") && !sel.equals(username)) {
            json.put("subtype","private").put("to",sel);
            displayMessage("Moi → "+sel,text,"private");
        } else {
            json.put("subtype","global");
            displayMessage(username,text,"global");
        }

        try {
            client.send(json.toString());
            messageField.setText("");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,"Erreur envoi","Erreur",JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayMessage(String from, String content, String subtype) {
        JPanel line = new JPanel();
        line.setLayout(new BoxLayout(line,BoxLayout.X_AXIS));
        line.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        boolean mine = from.startsWith("Moi")||from.equals(username);
        JLabel lbl = new JLabel(
            ("private".equals(subtype)
             ? "<html><i><font color=magenta>[MP] "+from+" :</font></i> "+content+"</html>"
             : from+" : "+content)
        );
        lbl.setFont(new Font("Arial", Font.PLAIN, 14));
        lbl.setForeground(mine? Color.BLACK: Color.BLUE);
        if (mine) { line.add(Box.createHorizontalGlue()); line.add(lbl);}
        else { line.add(lbl); line.add(Box.createHorizontalGlue()); }
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.add(line);
        messagePanel.revalidate(); messagePanel.repaint();
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar()
            .setValue(scrollPane.getVerticalScrollBar().getMaximum()));
    }

    private void refreshUserList() {
        userListModel.clear();
        userListModel.addElement("Global");
        List<String> users = new ArrayList<>(userStatus.keySet());
        Collections.sort(users);
        for (String u : users) {
            if (u.equals(username)) {
                userListModel.addElement(u + " (" + userStatus.get(u)+")");
            } else {
                userListModel.addElement(u + " (" + userStatus.get(u)+")");
            }
        }
        if (userList.getSelectedValue() == null) userList.setSelectedIndex(0);
        updateConversationLabel();
    }

    private void updateConversationLabel() {
        String sel = userList.getSelectedValue();
        if (sel == null || sel.startsWith("Global")) {
            conversationLabel.setText("Discussion : Global");
        } else {
            conversationLabel.setText("Discussion : MP avec " + sel.split(" ")[0]);
        }
    }
}
