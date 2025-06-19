package fr.classcord.ui;

import fr.classcord.network.ClientInvite;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RegisterFrame extends JFrame {

    private JTextField ipField = new JTextField("10.0.108.5");
    private JTextField portField = new JTextField("12345");
    private JTextField usernameField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();
    private JButton registerButton = new JButton("S'inscrire");
    private JLabel statusLabel = new JLabel(" ");

    private ClientInvite client;
    private boolean listenerAdded = false;

    public RegisterFrame(ClientInvite client) {
        super("Inscription");

        this.client = client;

        setLayout(new GridLayout(6, 2, 5, 5));
        setSize(350, 220);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        add(new JLabel("IP serveur :"));
        add(ipField);
        add(new JLabel("Port :"));
        add(portField);
        add(new JLabel("Nom d'utilisateur :"));
        add(usernameField);
        add(new JLabel("Mot de passe :"));
        add(passwordField);
        add(registerButton);
        add(statusLabel);

        registerButton.addActionListener(new RegisterListener());

        setVisible(true);
    }

    private class RegisterListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            handleRegistration();
        }
    }

    private void handleRegistration() {
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

        registerButton.setEnabled(false);
        statusLabel.setText("Inscription en cours...");

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

                                if (status.equals("ok") && type.equals("register")) {
                                    JOptionPane.showMessageDialog(RegisterFrame.this, "Inscription réussie !");
                                    dispose();  // ferme cette fenêtre
                                    new LoginFrame(client); // ouvre la fenêtre de connexion
                                } else if (type.equals("error")) {
                                    statusLabel.setText("Erreur : " + response.optString("message", "Inconnue"));
                                    registerButton.setEnabled(true);
                                }
                            } catch (Exception ex) {
                                statusLabel.setText("Erreur JSON");
                                registerButton.setEnabled(true);
                            }
                        });
                    });
                    listenerAdded = true;
                }

                JSONObject json = new JSONObject();
                json.put("type", "register");
                json.put("username", username);
                json.put("password", password);
                client.send(json.toString());

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Erreur de connexion : " + ex.getMessage());
                    registerButton.setEnabled(true);
                });
            }
        }).start();
    }
}
