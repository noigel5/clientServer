package com.github.noigel5.server;

import com.github.noigel5.model.Envelope;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

class ClientSocketHandler implements Runnable {

    public static final Gson GSON = new Gson();
    private final Server server;
    final Socket clientSocket;
    String input;

    ClientSocketHandler(Server server, Socket clientSocket) {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        while (true) {
            try {
                input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())).readLine();
                Envelope envelope = GSON.fromJson(input, Envelope.class);

                // set who sent this envelope...
                envelope.setSenderHashCode(clientSocket.hashCode());

                switch (envelope.getCommand()) {
                    case "/clients" -> {
                        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());

                        // V1
                        Envelope v1Envelope = new Envelope();
                        v1Envelope.setCommand("/msg");
                        v1Envelope.setSenderHashCode(0);
                        v1Envelope.setSenderName("Server");
                        v1Envelope.setRecipientHashCode(envelope.getSenderHashCode());
                        v1Envelope.setText(getClientsResponseText());
                        printWriter.println(GSON.toJson(v1Envelope));
                        printWriter.flush();
                    }
                    case "/msg" -> {
                        envelope.setSenderName(this.server.clients.get(clientSocket.hashCode()).name);
                        System.out.printf("%d to %s: %s%n", clientSocket.hashCode(), envelope.getRecipientHashCode(), envelope.getText());
                        PrintWriter printWriter = new PrintWriter(server.clients.get(envelope.getRecipientHashCode()).socket.getOutputStream());
                        printWriter.println(GSON.toJson(envelope));
                        printWriter.flush();
                    }
                    case "/all" -> {
                        envelope.setSenderName(this.server.clients.get(clientSocket.hashCode()).name);
                        System.out.printf("%d to all: %s%n", clientSocket.hashCode(), envelope.getText());
                        for (ClientRef clientRef : server.clients.values()) {
                            if (clientRef.socket.hashCode() != clientSocket.hashCode()) {
                                PrintWriter printWriter = new PrintWriter(clientRef.socket.getOutputStream());
                                printWriter.println(GSON.toJson(envelope));
                                printWriter.flush();
                            }
                        }
                    }
                    case "/name" -> {
                        System.out.printf("%d set name to:(%s)%n", clientSocket.hashCode(), envelope.getSenderName());
                        server.clients.get(this.clientSocket.hashCode()).name = envelope.getSenderName();
                        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
                        Envelope response = new Envelope();
                        response.setSenderHashCode(0);
                        response.setCommand("/msg");
                        response.setRecipientHashCode(clientSocket.hashCode());
                        response.setSenderName("Server");
                        response.setText("Name change received. You are now: " + envelope.getSenderName());
                        printWriter.println(GSON.toJson(response));
                        printWriter.flush();
                    }
                }
            } catch (IOException e) {
                int clientId = this.clientSocket.hashCode();
                server.clients.remove(clientId);
                System.out.println("client " + clientId + " disconnected.");
                return;
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println(Arrays.toString(e.getStackTrace()));
            }
        }
    }

    private String getClientsResponseText() {
//        return "Clients:"
//                + System.lineSeparator()
//                + server.clients.values()
//                .stream()
//                .map(clt -> String.format("- %s (%s) %s",
//                        clt.socket.hashCode(),
//                        clt.name,
//                        clt.socket.hashCode() == clientSocket.hashCode() ? "<YOU>" : ""))
//                .collect(Collectors.joining("\n"));


        StringBuilder sb = new StringBuilder();

        sb.append("Clients:")
                .append(System.lineSeparator());

        for (ClientRef clt : server.clients.values()) {
            sb.append("- ");
            sb.append(clt.socket.hashCode());
            sb.append(" (");
            sb.append(clt.name);
            sb.append(") ");
            if (clt.socket.hashCode() == clientSocket.hashCode()) {
                sb.append(" <YOU>");
            }
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }
}
