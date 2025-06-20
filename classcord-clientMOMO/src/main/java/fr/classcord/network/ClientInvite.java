package fr.classcord.network;

import java.io.*;
import java.net.Socket;
import java.util.*;
import org.json.JSONObject;

/**
 * Classe client pour gérer la connexion au serveur Classcord.
 * Elle permet de se connecter, d'envoyer/recevoir des messages JSON, 
 * d'écouter les messages entrants, et de gérer un mode invité.
 */
public class ClientInvite {

    private Socket socket;                  // Socket TCP pour la connexion
    private BufferedReader in;              // Pour lire les messages reçus
    private BufferedWriter out;             // Pour envoyer les messages
    private final List<MessageListener> listeners = new ArrayList<>();  // Liste des listeners pour les messages reçus
    private boolean listening = false;      // Indicateur si l'écoute est active

    private String lastGuestName;           // Pseudo du dernier invité connecté

    /**
     * Connecte le client au serveur donné par IP et port.
     * Initialise les flux d'entrée/sortie et démarre l'écoute des messages.
     */
    public void connect(String ip, int port) throws IOException {
        // Si déjà connecté, on ne reconnecte pas
        if (socket != null && socket.isConnected()) return;

        socket = new Socket(ip, port);

        // Flux d'entrée en UTF-8
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        // Flux de sortie en UTF-8
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

        startListening();
    }

    /**
     * Démarre un thread d'écoute pour recevoir les messages entrants.
     * Chaque message reçu est envoyé à tous les MessageListener enregistrés.
     */
    private void startListening() {
        if (listening) return;  // Évite plusieurs threads d'écoute

        listening = true;

        Thread t = new Thread(() -> {
            try {
                String line;
                // Lecture ligne par ligne tant que le serveur envoie des messages
                while ((line = in.readLine()) != null) {
                    // Notifie tous les listeners avec le message reçu
                    for (MessageListener ml : listeners) {
                        ml.onMessage(line);
                    }
                }
            } catch (IOException e) {
                System.err.println("[ClientInvite] écoute échouée : " + e.getMessage());
            } finally {
                close();  // En cas d'erreur, ferme la connexion proprement
            }
        });
        t.setDaemon(true); // Thread daemon pour ne pas bloquer la fermeture JVM
        t.start();
    }

    /**
     * Connexion en tant qu'invité.
     * Envoie un message vide au serveur pour "annoncer" l'invité.
     * @param ip Adresse IP du serveur
     * @param port Port serveur
     * @param pseudo Pseudo choisi par l'invité
     */
    public void connectAsGuest(String ip, int port, String pseudo) throws IOException {
        connect(ip, port);
        this.lastGuestName = pseudo;

        // Message JSON vide de type "message" subtype "global" pour se faire accepter
        JSONObject msg = new JSONObject();
        msg.put("type", "message");
        msg.put("subtype", "global");
        msg.put("from", pseudo);
        msg.put("to", "global");
        msg.put("content", ""); // message vide
        send(msg.toString());
    }

    /**
     * Envoie un message global depuis un invité.
     * @param content Contenu du message à envoyer
     */
    public void sendGuestMessage(String content) throws IOException {
        if (out != null) {
            JSONObject msg = new JSONObject();
            msg.put("type", "message");
            msg.put("subtype", "global");
            msg.put("from", lastGuestName);
            msg.put("to", "global");
            msg.put("content", content);
            send(msg.toString());
        }
    }

    /**
     * Envoie un message brut au serveur.
     * @param msg Message JSON sous forme de chaîne
     */
    public void send(String msg) throws IOException {
        if (out != null) {
            out.write(msg);
            out.write("\n"); // Important : envoie une nouvelle ligne pour marquer la fin du message
            out.flush();      // Vide le buffer pour envoi immédiat
        }
    }

    /**
     * Ajoute un listener qui sera appelé à chaque message reçu.
     * @param ml Listener à ajouter
     */
    public void addMessageListener(MessageListener ml) {
        listeners.add(ml);
    }

    /**
     * Ferme proprement les flux et la socket.
     */
    public void close() {
        listening = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {
            // Ignorer les erreurs à la fermeture
        }
    }

    /**
     * Retourne le pseudo de l'invité actuel ou dernier invité.
     * @return pseudo invité
     */
    public String getLastGuestName() {
        return lastGuestName;
    }

    /**
     * Interface à implémenter pour recevoir les messages entrants.
     */
    public interface MessageListener {
        void onMessage(String message);
    }
}
