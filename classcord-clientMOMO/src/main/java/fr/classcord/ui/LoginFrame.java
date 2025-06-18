package fr.classcord.ui;

import fr.classcord.network.ClientInvite;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {

    private JTextField ipField = new JTextField("10.0.108.56");
    private JTextField portField = new JTextField("12345");
    private JTextField usernameField = new JTextField("TEST");
    private JPasswordField passwordField = new JPasswordField("1234");
    private JButton loginButton = new JButton("Se connecter");
    private JButton registerButton = new JButton("S'inscrire");
    private JLabel statusLabel = new JLabel(" ");

    private ClientInvite client;
    private boolean listenerAdded = false; // Pour ne pas ajouter plusieurs fois

    public LoginFrame(ClientInvite client) {
        super("Connexion");

        this.client = client;

        setLayout(new GridLayout(7, 2, 5, 5));
        setSize(350, 250);
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
        add(statusLabel);

        loginButton.addActionListener(new LoginListener());
        registerButton.addActionListener(e -> {
            // Ouvre la fenêtre d'inscription sans fermer celle-ci
            new RegisterFrame(client);
        });

        setVisible(true);
    }

    private class LoginListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            handleConnection("login");
        }
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

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Veuillez remplir tous les champs");
            return;
        }

        loginButton.setEnabled(false);
        registerButton.setEnabled(false);
        statusLabel.setText("Connexion en cours...");

        new Thread(() -> {
            try {
                client.connect(ip, port);

                // Ajoute le listener une seule fois
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
                                }
                            } catch (Exception ex) {
                                statusLabel.setText("Erreur JSON");
                                loginButton.setEnabled(true);
                                registerButton.setEnabled(true);
                            }
                        });
                    });
                    listenerAdded = true;
                }

                JSONObject json = new JSONObject();
                json.put("type", actionType);
                json.put("username", username);
                json.put("password", password);
                client.send(json.toString());

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Erreur de connexion : " + ex.getMessage());
                    loginButton.setEnabled(true);
                    registerButton.setEnabled(true);
                });
            }
        }).start();
    }
}
