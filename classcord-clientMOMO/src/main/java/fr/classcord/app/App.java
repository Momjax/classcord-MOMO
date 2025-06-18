package fr.classcord.app;

import fr.classcord.network.ClientInvite;
import fr.classcord.ui.LoginFrame;

import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientInvite client = new ClientInvite();
            new LoginFrame(client);
        });
    }
}