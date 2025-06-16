package fr.classcord.app;

import fr.classcord.network.ClientInvite;
import org.json.JSONObject;

import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Entrez votre pseudo : ");
        String pseudo = scanner.nextLine();

        System.out.print("Entrez l'IP du serveur : ");
        String ip = scanner.nextLine();

        System.out.print("Entrez le port : ");
        int port = Integer.parseInt(scanner.nextLine());

        ClientInvite client = new ClientInvite();
        client.connect(ip, port, pseudo);

        System.out.println(" Vous pouvez maintenant envoyer des messages (tapez 'exit' pour quitter)");

        while (true) {
            String content = scanner.nextLine();
            if (content.equalsIgnoreCase("exit")) break;

            JSONObject message = new JSONObject();
            message.put("type", "message");
            message.put("subtype", "global");
            message.put("to", "global");
            message.put("from", pseudo);
            message.put("content", content);

            client.send(message.toString());
        }

        client.disconnect();
        scanner.close();
    }
}
