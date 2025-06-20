package fr.classcord.ui;

import fr.classcord.network.ClientInvite;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ChatFrame extends JFrame {
    private String username;  // Pseudo de l'utilisateur connecté
    private ClientInvite client;  // Objet réseau pour communiquer avec le serveur
    private JTextField messageField;  // Champ pour écrire un message
    private JPanel messagePanel;  // Panel contenant les messages affichés
    private JScrollPane scrollPane;  // Conteneur avec ascenseur pour messages
    private JList<String> userList;  // Liste des utilisateurs connectés
    private DefaultListModel<String> userListModel;  // Modèle pour gérer la liste des utilisateurs
    private JLabel conversationLabel;  // Label indiquant la conversation en cours
    private JComboBox<String> statusCombo;  // ComboBox pour choisir le statut utilisateur

    private Map<String, String> userStatus = new HashMap<>();  // Stocke le statut (online, away...) des utilisateurs

    // Constructeur principal : initialise la fenêtre de chat
    public ChatFrame(ClientInvite client, String username) {
        this.client = client;
        this.username = username;
        setTitle("Classcord – " + username);  // Titre de la fenêtre avec pseudo
        setSize(700, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);  // Ferme l'appli à la fermeture de la fenêtre
        setLocationRelativeTo(null);  // Centre la fenêtre à l'écran
        setLayout(new BorderLayout());  // Layout global BorderLayout (Nord, Sud, Centre, Est, Ouest)

        // --- Partie haute (NORD) : Label conversation et statut ---
        JPanel top = new JPanel(new BorderLayout());
        conversationLabel = new JLabel("Discussion : Global"); // Indique la conversation active (par défaut globale)
        conversationLabel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));  // Marge interne

        top.add(conversationLabel, BorderLayout.WEST);  // Label à gauche du panneau supérieur

        statusCombo = new JComboBox<>(new String[]{"online", "away", "dnd", "invisible"});  // Choix du statut utilisateur
        statusCombo.addActionListener(e -> sendStatusChange());  // Envoi du changement de statut au serveur lors d'une sélection
        top.add(statusCombo, BorderLayout.EAST);  // Combo à droite du panneau supérieur

        add(top, BorderLayout.NORTH);  // Ajoute ce panneau en haut de la fenêtre

        // --- Partie centrale (CENTRE) : zone d'affichage des messages ---
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));  // Messages empilés verticalement
        scrollPane = new JScrollPane(messagePanel);  // ScrollPane pour pouvoir faire défiler les messages si trop nombreux
        add(scrollPane, BorderLayout.CENTER);

        // --- Partie droite (EST) : liste des utilisateurs ---
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);  // Une seule sélection possible
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(180, 0));  // Largeur fixe de la liste

        // Écouteur sur sélection d'un utilisateur pour changer le label de conversation
        userList.addListSelectionListener(e -> updateConversationLabel());

        // Personnalisation du rendu de chaque élément de la liste (pseudo + icône couleur selon statut)
        userList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String userLine = (String) value;

                if (userLine.equals("Global")) { // Cas de la conversation globale
                    label.setText("🌐 Global");
                    label.setIcon(null);
                } else {
                    // Extraction du pseudo et du statut
                    String name = userLine.substring(userLine.indexOf(" ") + 1);
                    String status = userStatus.getOrDefault(name, "online");
                    // Couleur selon le statut
                    Color color = switch (status) {
                        case "online" -> Color.GREEN;
                        case "away" -> Color.ORANGE;
                        case "dnd" -> Color.RED;
                        case "invisible" -> Color.LIGHT_GRAY;
                        default -> Color.GRAY;
                    };
                    label.setText(name);
                    label.setIcon(new StatusIcon(color));  // Icône personnalisée (rond coloré)
                }
                return label;
            }
        });

        add(userScroll, BorderLayout.EAST);

        // --- Partie basse (SUD) : champ pour écrire + bouton envoyer ---
        JPanel bottom = new JPanel(new BorderLayout());
        messageField = new JTextField();
        JButton sendBtn = new JButton("Envoyer");

        // Envoi message sur clic bouton ou touche Entrée dans le champ texte
        sendBtn.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

        bottom.add(messageField, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // Ajout d'un listener pour recevoir les messages du serveur en arrière-plan
        client.addMessageListener(msg -> SwingUtilities.invokeLater(() -> onSocketMessage(msg)));

        // Initialisation du statut local
        userStatus.put(username, "online");
        refreshUserList();  // Mise à jour de la liste utilisateur
        requestUsersList();  // Demande au serveur la liste actuelle des utilisateurs connectés
        setVisible(true);  // Affiche la fenêtre
    }

    // Méthode appelée à la réception d'un message JSON via socket
    private void onSocketMessage(String msg) {
        try {
            JSONObject j = new JSONObject(msg);
            String type = j.getString("type");
            // Traitement selon le type de message reçu
            switch (type) {
                case "message" -> handleIncomingMessage(j);
                case "status" -> handleIncomingStatus(j);
                case "users" -> handleUsersList(j);
            }
        } catch (Exception ignored) {}  // Erreurs ignorées pour éviter plantage UI
    }

    // Traitement d'un message reçu (global ou privé)
    private void handleIncomingMessage(JSONObject j) {
        String from = j.getString("from");
        String content = j.getString("content");
        String subtype = j.optString("subtype", "global");
        String to = j.optString("to", "");
        if ("private".equals(subtype)) {
            // Affiche uniquement les messages privés impliquant l'utilisateur
            if (from.equals(username) || to.equals(username))
                displayMessage("MP de " + from, content, "private");
        } else {
            // Message global affiché à tous
            displayMessage(from, content, "global");
        }
    }

    // Mise à jour du statut d'un utilisateur à partir d'un message reçu
    private void handleIncomingStatus(JSONObject j) {
        String user = j.getString("user");
        String state = j.getString("state");
        if ("offline".equals(state)) {
            userStatus.remove(user);  // Suppression si déconnecté
        } else if ("invisible".equals(state) && !user.equals(username)) {
            userStatus.remove(user);  // Invisibles non listés sauf soi-même
        } else {
            userStatus.put(user, state);  // Mise à jour du statut
        }
        refreshUserList();  // Mise à jour visuelle
    }

    // Réception de la liste complète des utilisateurs connectés
    private void handleUsersList(JSONObject j) {
        userStatus.clear();
        for (Object o : j.getJSONArray("users")) {
            String user = (String) o;
            userStatus.put(user, "online");  // Par défaut statut online
        }
        userStatus.put(username, (String) statusCombo.getSelectedItem());  // Statut personnel exact
        refreshUserList();
    }

    // Demande au serveur la liste des utilisateurs connectés
    private void requestUsersList() {
        try {
            client.send(new JSONObject().put("type", "users").toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Envoi au serveur du changement de statut sélectionné
    private void sendStatusChange() {
        String state = (String) statusCombo.getSelectedItem();
        try {
            client.send(new JSONObject()
                    .put("type", "status")
                    .put("state", state)
                    .toString());
            userStatus.put(username, state);
            refreshUserList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Envoi d'un message tapé dans le champ texte
    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        JSONObject json = new JSONObject()
                .put("type", "message")
                .put("from", username)
                .put("content", text);

        String sel = userList.getSelectedValue();
        if (sel != null && !sel.equals("Global") && !sel.startsWith("🌐 Global")) {
            // Message privé à un utilisateur spécifique
            String destUser = sel.substring(sel.indexOf(" ") + 1);
            if (!destUser.equals(username)) {
                json.put("subtype", "private").put("to", destUser);
                displayMessage("Moi → " + destUser, text, "private");
            }
        } else {
            // Message global
            json.put("subtype", "global");
            displayMessage(username, text, "global");
        }

        try {
            client.send(json.toString());
            messageField.setText("");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erreur envoi", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Affichage d'un message dans la fenêtre chat
    private void displayMessage(String from, String content, String subtype) {
        JPanel line = new JPanel();
        line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
        line.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        boolean mine = from.startsWith("Moi") || from.equals(username);
        JLabel lbl = new JLabel(
                ("private".equals(subtype)
                        ? "<html><i><font color=magenta>[MP] " + from + " :</font></i> " + content + "</html>"
                        : from + " : " + content)
        );
        lbl.setFont(new Font("Arial", Font.PLAIN, 14));
        lbl.setForeground(mine ? Color.BLACK : Color.BLUE);
        if (mine) {
            line.add(Box.createHorizontalGlue());  // Aligne message à droite si c'est moi
            line.add(lbl);
        } else {
            line.add(lbl);  // Aligne message à gauche si c'est un autre utilisateur
            line.add(Box.createHorizontalGlue());
        }
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagePanel.add(line);
        messagePanel.revalidate();
        messagePanel.repaint();
        // Scroll automatique en bas pour voir le dernier message
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar()
                .setValue(scrollPane.getVerticalScrollBar().getMaximum()));
    }

    // Mise à jour de la liste des utilisateurs affichée
    private void refreshUserList() {
        userListModel.clear();
        userListModel.addElement("Global");  // Toujours la conversation globale en haut
        List<String> users = new ArrayList<>(userStatus.keySet());
        Collections.sort(users);  // Tri alphabétique
        for (String u : users) {
            String state = userStatus.get(u);
            // On ne montre pas les invisibles sauf soi-même
            if ("invisible".equals(state) && !u.equals(username)) continue;
            String iconText = switch (state) {
                case "online" -> "🟢";
                case "away" -> "🟠";
                case "dnd" -> "🔴";
                case "invisible" -> "⚪";
                default -> "⚪";
            };
            userListModel.addElement(iconText + " " + u);  // Ajoute avec icône couleur devant
        }
        if (userList.getSelectedValue() == null) userList.setSelectedIndex(0);  // Sélection par défaut Global
        updateConversationLabel();  // Met à jour le label de la conversation active
    }

    // Met à jour le label en fonction de l'utilisateur sélectionné
    private void updateConversationLabel() {
        String sel = userList.getSelectedValue();
        if (sel == null || sel.startsWith("Global")) {
            conversationLabel.setText("Discussion : Global");
        } else {
            conversationLabel.setText("Discussion : MP avec " + sel.substring(sel.indexOf(" ") + 1));
        }
    }
}
