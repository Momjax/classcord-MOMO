package fr.classcord.ui;

import fr.classcord.network.ClientInvite;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Fenêtre d'inscription pour Classcord.
 * Permet à l'utilisateur de créer un nouveau compte.
 */
public class RegisterFrame extends JFrame {

    // Champs pour saisir IP, port, nom d'utilisateur et mot de passe
    private JTextField ipField = new JTextField("10.0.108.133");
    private JTextField portField = new JTextField("12345");
    private JTextField usernameField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();

    // Bouton pour lancer l'inscription
    private JButton registerButton = new JButton("S'inscrire");
    private JLabel statusLabel = new JLabel(" "); // Label pour afficher le statut ou erreurs

    private ClientInvite client;  // Client réseau pour la communication avec le serveur
    private boolean listenerAdded = false; // Pour éviter de ré-ajouter le listener plusieurs fois

    /**
     * Constructeur de la fenêtre d'inscription.
     * @param client Instance du client réseau
     */
    public RegisterFrame(ClientInvite client) {
        super("Inscription");

        this.client = client;

        // Configuration de la fenêtre avec une grille 6x2, espacements 5px
        setLayout(new GridLayout(6, 2, 5, 5));
        setSize(350, 220);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Ferme uniquement cette fenêtre
        setLocationRelativeTo(null); // Centre la fenêtre

        // Ajout des composants dans l'ordre
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

        // Assignation de l'action du bouton à la classe interne RegisterListener
        registerButton.addActionListener(new RegisterListener());

        setVisible(true);
    }

    /**
     * Classe interne gérant le clic sur le bouton d'inscription.
     */
    private class RegisterListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            handleRegistration();
        }
    }

    /**
     * Méthode qui gère l'inscription en envoyant les données au serveur.
     * Elle valide les champs, envoie la requête, et traite la réponse.
     */
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

        // Vérification que tous les champs sont remplis
        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Veuillez remplir tous les champs");
            return;
        }

        // Désactivation du bouton pendant le traitement
        registerButton.setEnabled(false);
        statusLabel.setText("Inscription en cours...");

        // Traitement en arrière-plan pour ne pas bloquer l'interface
        new Thread(() -> {
            try {
                // Connexion au serveur avec IP et port fournis
                client.connect(ip, port);

                // Ajout du listener une seule fois pour recevoir les réponses serveur
                if (!listenerAdded) {
                    client.addMessageListener(message -> {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                JSONObject response = new JSONObject(message);
                                String type = response.optString("type");
                                String status = response.optString("status");

                                if (status.equals("ok") && type.equals("register")) {
                                    // Inscription réussie : message, fermeture, et ouverture fenêtre login
                                    JOptionPane.showMessageDialog(RegisterFrame.this, "Inscription réussie !");
                                    dispose();
                                    new LoginFrame(client);
                                } else if (type.equals("error")) {
                                    // Affiche le message d'erreur reçu
                                    statusLabel.setText("Erreur : " + response.optString("message", "Inconnue"));
                                    registerButton.setEnabled(true);
                                }
                            } catch (Exception ex) {
                                // Erreur JSON reçue ou mal formée
                                statusLabel.setText("Erreur JSON");
                                registerButton.setEnabled(true);
                            }
                        });
                    });
                    listenerAdded = true;
                }

                // Construction et envoi du JSON d'inscription au serveur
                JSONObject json = new JSONObject();
                json.put("type", "register");
                json.put("username", username);
                json.put("password", password);
                client.send(json.toString());

            } catch (Exception ex) {
                // En cas d'erreur de connexion, affiche message et réactive bouton
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Erreur de connexion : " + ex.getMessage());
                    registerButton.setEnabled(true);
                });
            }
        }).start();
    }
}
