package fr.classcord.network;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientInvite {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private final List<MessageListener> listeners = new ArrayList<>();
    private boolean listening = false;

    public void connect(String ip, int port) throws IOException {
        if (socket != null && socket.isConnected()) return;
        socket = new Socket(ip, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        startListening();
    }

    private void startListening() {
        if (listening) return;
        listening = true;
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    for (MessageListener ml : listeners) {
                        ml.onMessage(line);
                    }
                }
            } catch (IOException e) {
                System.err.println("[ClientInvite] écoute échouée : " + e.getMessage());
            } finally {
                close();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void send(String msg) throws IOException {
        if (out != null) {
            out.write(msg);
            out.write("\n");
            out.flush();
        }
    }

    public void addMessageListener(MessageListener ml) {
        listeners.add(ml);
    }

    public void close() {
        listening = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public interface MessageListener {
        void onMessage(String message);
    }
}
