package fr.classcord.network;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientInvite {

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private final List<MessageListener> listeners = new ArrayList<>();
    private boolean listening = false;

    // Connexion au serveur
    public void connect(String ip, int port) throws IOException {
        if (socket != null && socket.isConnected()) return;

        socket = new Socket(ip, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

        startListening();
    }

    // Démarrage du thread d'écoute des messages
    private void startListening() {
        if (listening) return;
        listening = true;

        Thread listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    for (MessageListener listener : listeners) {
                        listener.onMessage(line);
                    }
                }
            } catch (IOException e) {
                System.err.println("[ClientInvite] Erreur d'écoute : " + e.getMessage());
            } finally {
                close();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // Envoi d'un message (JSON en string)
    public void send(String message) throws IOException {
        if (out != null) {
            out.write(message);
            out.write("\n");
            out.flush();
        }
    }

    // Ajout d'un écouteur de message
    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    // Fermeture de la connexion
    public void close() {
        listening = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    // Interface de callback
    public interface MessageListener {
        void onMessage(String message);
    }
}
