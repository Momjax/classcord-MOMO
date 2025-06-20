package fr.classcord.ui;

import fr.classcord.network.ClientInvite;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;

/**
 * Fenêtre de connexion pour Classcord.
 * Permet de se connecter, s'inscrire, ou continuer en invité.
 */
public class LoginFrame extends JFrame {

    // Champs pour saisir l'adresse IP, le port, le nom d'utilisateur et le mot de passe
    private JTextField ipField = new JTextField("10.0.108.133");
    private JTextField portField = new JTextField("12345");
    private JTextField usernameField = new JTextField("TEST");
    private JPasswordField passwordField = new JPasswordField("1234");
    
    // Boutons pour les actions principales
    private JButton loginButton = new JButton("Se connecter");
    private JButton registerButton = new JButton("S'inscrire");
    private JButton guestButton = new JButton("Continuer en invité");
    
    // Label pour afficher le statut ou les erreurs
    private JLabel statusLabel = new JLabel(" ");

    private ClientInvite client;  // Client réseau gérant la communication
    private boolean listenerAdded = false; // Pour éviter d'ajouter plusieurs fois le listener sur messages

    /**
     * Constructeur de la fenêtre de connexion.
     * @param client Instance du client réseau
     */
    public LoginFrame(ClientInvite client) {
        super("Connexion");
        this.client = client;

        // Configuration de la fenêtre avec une grille 8x2, espaces 5px entre composants
        setLayout(new GridLayout(8, 2, 5, 5));
        setSize(350, 280);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centrer à l'écran

        // Ajout des composants dans l'ordre
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

        // Actions des boutons
        loginButton.addActionListener(e -> handleConnection());
        registerButton.addActionListener(e -> new RegisterFrame(client)); // Ouvre la fenêtre d'inscription
        guestButton.addActionListener(e -> handleGuestConnection());

        setVisible(true);
    }

    /**
     * Tente une connexion avec les infos saisies.
     * Gère la communication réseau et les retours serveur.
     */
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

        // Désactivation des boutons pendant la connexion
        loginButton.setEnabled(false);
        registerButton.setEnabled(false);
        guestButton.setEnabled(false);
        statusLabel.setText("Connexion en cours...");

        // Connexion dans un thread séparé pour ne pas bloquer l'interface
        new Thread(() -> {
            try {
                client.connect(ip, port); // Connexion au serveur

                // Ajout du listener sur les messages reçus du serveur (une seule fois)
                if (!listenerAdded) {
                    client.addMessageListener(message -> {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                JSONObject response = new JSONObject(message);
                                String type = response.optString("type");
                                String status = response.optString("status");

                                if ("login".equals(type) && "ok".equals(status)) {
                                    // Connexion réussie : ouvrir la fenêtre de chat et fermer celle-ci
                                    new ChatFrame(client, username);
                                    dispose();
                                } else if ("error".equals(type)) {
                                    // Afficher le message d'erreur reçu du serveur
                                    statusLabel.setText("Erreur : " + response.optString("message", "Inconnue"));
                                    // Réactiver les boutons
                                    loginButton.setEnabled(true);
                                    registerButton.setEnabled(true);
                                    guestButton.setEnabled(true);
                                }
                            } catch (Exception ex) {
                                // Erreur lors de la lecture JSON
                                statusLabel.setText("Erreur JSON");
                                loginButton.setEnabled(true);
                                registerButton.setEnabled(true);
                                guestButton.setEnabled(true);
                            }
                        });
                    });
                    listenerAdded = true;
                }

                // Envoi de la requête de login au serveur
                JSONObject json = new JSONObject();
                json.put("type", "login");
                json.put("username", username);
                json.put("password", password);
                client.send(json.toString());

            } catch (Exception ex) {
                // En cas d'erreur réseau, afficher et réactiver les boutons
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Erreur de connexion : " + ex.getMessage());
                    loginButton.setEnabled(true);
                    registerButton.setEnabled(true);
                    guestButton.setEnabled(true);
                });
            }
        }).start();
    }

    /**
     * Connexion en mode invité.
     * Demande un pseudo et établit une connexion spécifique.
     */
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

        // Connexion invité en thread séparé
        new Thread(() -> {
            try {
                client.connectAsGuest(ip, port, pseudo.trim());
                SwingUtilities.invokeLater(() -> {
                    // Ouvre la fenêtre chat en mode invité
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
