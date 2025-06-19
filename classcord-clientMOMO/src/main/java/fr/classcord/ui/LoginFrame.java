package fr.classcord.ui;

import fr.classcord.network.ClientInvite;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {

    private JTextField ipField = new JTextField("10.0.108.55");
    private JTextField portField = new JTextField("12345");
    private JTextField usernameField = new JTextField("TEST");
    private JPasswordField passwordField = new JPasswordField("1234");
    private JButton loginButton = new JButton("Se connecter");
    private JButton registerButton = new JButton("S'inscrire");
    private JButton guestButton = new JButton("Invité");
    private JLabel statusLabel = new JLabel(" ");

    private ClientInvite client;
    private boolean listenerAdded = false;

    // mode invité ou non
    private boolean isGuestMode = false;

    public LoginFrame(ClientInvite client) {
        super("Connexion");
        this.client = client;

        setLayout(new GridLayout(8, 2, 5, 5));
        setSize(350, 280);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        add(new JLabel("IP serveur :"));
        add(ipField);
        add(new JLabel("Port :"));
        add(portField);
        add(new JLabel("Nom d'utilisateur :"));
        add(usernameField);
        add(new JLabel("Mot de passe :"));
        add(passwordField);

        add(loginButton);
        add(registerButton);
        add(guestButton);
        add(statusLabel);

        // Action boutons
        loginButton.addActionListener(e -> handleConnection("login"));
        registerButton.addActionListener(e -> new RegisterFrame(client));
        guestButton.addActionListener(e -> toggleGuestMode());

        setVisible(true);
    }

    private void toggleGuestMode() {
        isGuestMode = !isGuestMode;
        if (isGuestMode) {
            usernameField.setVisible(false);
            passwordField.setVisible(false);
            // Supprimer les labels qui précèdent username et password
            ((Container)usernameField.getParent()).getComponent(4).setVisible(false); // label "Nom d'utilisateur"
            ((Container)passwordField.getParent()).getComponent(6).setVisible(false); // label "Mot de passe"
            loginButton.setText("Se connecter en invité");
        } else {
            usernameField.setVisible(true);
            passwordField.setVisible(true);
            ((Container)usernameField.getParent()).getComponent(4).setVisible(true);
            ((Container)passwordField.getParent()).getComponent(6).setVisible(true);
            loginButton.setText("Se connecter");
        }
        // Réorganiser la fenêtre
        revalidate();
        repaint();
    }

    private void handleConnection(String actionType) {
        String ip = ipField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Port invalide");
            return;
        }

        String username = isGuestMode ? "Invité" + System.currentTimeMillis() : usernameField.getText().trim();
        String password = isGuestMode ? "" : new String(passwordField.getPassword());

        if (!isGuestMode && (username.isEmpty() || password.isEmpty())) {
            statusLabel.setText("Veuillez remplir tous les champs");
            return;
        }

        loginButton.setEnabled(false);
        registerButton.setEnabled(false);
        guestButton.setEnabled(false);
        statusLabel.setText("Connexion en cours...");

        new Thread(() -> {
            try {
                client.connect(ip, port);

                if (!listenerAdded) {
                    client.addMessageListener(message -> {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                JSONObject response = new JSONObject(message);
                                String type = response.optString("type");
                                String status = response.optString("status");

                                if (status.equals("ok") && type.equals("login")) {
                                    new ChatFrame(client, username);
                                    dispose();
                                } else if (type.equals("error")) {
                                    statusLabel.setText("Erreur : " + response.optString("message", "Inconnue"));
                                    loginButton.setEnabled(true);
                                    registerButton.setEnabled(true);
                                    guestButton.setEnabled(true);
                                }
                            } catch (Exception ex) {
                                statusLabel.setText("Erreur JSON");
                                loginButton.setEnabled(true);
                                registerButton.setEnabled(true);
                                guestButton.setEnabled(true);
                            }
                        });
                    });
                    listenerAdded = true;
                }

                JSONObject json = new JSONObject();
                json.put("type", actionType);
                json.put("username", username);
                json.put("password", password); // toujours présent, même vide
                client.send(json.toString());


            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Erreur de connexion : " + ex.getMessage());
                    loginButton.setEnabled(true);
                    registerButton.setEnabled(true);
                    guestButton.setEnabled(true);
                });
            }
        }).start();
    }
}
