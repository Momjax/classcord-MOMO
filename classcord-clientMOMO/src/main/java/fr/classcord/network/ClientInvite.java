package fr.classcord.network;

import java.io.*;
import java.net.Socket;

public class ClientInvite {

    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private String pseudo;

    public void connect(String ip, int port, String pseudo) {
        try {
            this.pseudo = pseudo;
            socket = new Socket(ip, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println(" Connecté au serveur " + ip + ":" + port);

            // Thread pour écouter les messages entrants
            new Thread(() -> {
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        System.out.println("📨 Message reçu : " + line);
                    }
                } catch (IOException e) {
                    System.out.println(" Erreur de lecture : " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            System.out.println(" Impossible de se connecter : " + e.getMessage());
        }
    }

    public void send(String messageText) {
        if (writer != null) {
            writer.println(messageText); // JSON déjà formé
        }
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println(" Erreur à la déconnexion.");
        }
    }
}
