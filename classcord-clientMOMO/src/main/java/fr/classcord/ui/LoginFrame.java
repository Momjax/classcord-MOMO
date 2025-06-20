package fr.classcord.ui;

import fr.classcord.network.ClientInvite;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField ipField = new JTextField("10.0.108.133");
    private JTextField portField = new JTextField("12345");
    private JTextField usernameField = new JTextField("TEST");
    private JPasswordField passwordField = new JPasswordField("1234");
    private JButton loginButton = new JButton("Se connecter");
    private JButton registerButton = new JButton("S'inscrire");
    private JButton guestButton = new JButton("Continuer en invité");
    private JLabel statusLabel = new JLabel(" ");

    private ClientInvite client;
    private boolean listenerAdded = false;

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

        loginButton.addActionListener(e -> handleConnection());
        registerButton.addActionListener(e -> new RegisterFrame(client));
        guestButton.addActionListener(e -> handleGuestConnection());

        setVisible(true);
    }

    private void handleConnection() {
        String ip = ipField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Port invalide");
            return;
        }

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
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

                                if ("login".equals(type) && "ok".equals(status)) {
                                    new ChatFrame(client, username);
                                    dispose();
                                } else if ("error".equals(type)) {
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
                json.put("type", "login");
                json.put("username", username);
                json.put("password", password);
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

    private void handleGuestConnection() {
        String pseudo = JOptionPane.showInputDialog(this, "Pseudo invité :", "Invité" + System.currentTimeMillis());
        if (pseudo == null || pseudo.trim().isEmpty()) return;

        String ip = ipField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Port invalide");
            return;
        }

        guestButton.setEnabled(false);
        statusLabel.setText("Connexion invité...");

        new Thread(() -> {
            try {
                client.connectAsGuest(ip, port, pseudo.trim());
                SwingUtilities.invokeLater(() -> {
                    new ChatFrame(client, pseudo.trim());
                    dispose();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Erreur invité : " + ex.getMessage());
                    guestButton.setEnabled(true);
                });
            }
        }).start();
    }
}
