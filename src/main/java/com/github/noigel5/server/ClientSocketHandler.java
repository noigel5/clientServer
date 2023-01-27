package com.github.noigel5.server;

import com.github.noigel5.model.Envelope;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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

                if (envelope.isLogedIn()) {
                    switch (envelope.getCommand()) {
                        case "/clients" -> {
                            PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());

                            // V1
                            Envelope v1Envelope = new Envelope();
                            v1Envelope.setCommand("/msg");
                            v1Envelope.setSenderHashCode(0);
                            v1Envelope.setUsername("Server");
                            v1Envelope.setRecipientHashCode(envelope.getSenderHashCode());
                            v1Envelope.setText(getClientsResponseText());
                            v1Envelope.setLogedIn(true);
                            printWriter.println(GSON.toJson(v1Envelope));
                            printWriter.flush();
                        }
                        case "/msg" -> {
                            envelope.setUsername(this.server.clients.get(clientSocket.hashCode()).name);
                            log.info(String.format("%d to %s: %s%n", clientSocket.hashCode(), envelope.getRecipientHashCode(), envelope.getText()));
                            PrintWriter printWriter = new PrintWriter(server.clients.get(envelope.getRecipientHashCode()).socket.getOutputStream());
                            printWriter.println(GSON.toJson(envelope));
                            printWriter.flush();
                        }
                        case "/all" -> {
                            envelope.setUsername(this.server.clients.get(clientSocket.hashCode()).name);
                            log.info(String.format("%d to all: %s%n", clientSocket.hashCode(), envelope.getText()));
                            for (ClientRef clientRef : server.clients.values()) {
                                if (clientRef.socket.hashCode() != clientSocket.hashCode()) {
                                    PrintWriter printWriter = new PrintWriter(clientRef.socket.getOutputStream());
                                    printWriter.println(GSON.toJson(envelope));
                                    printWriter.flush();
                                }
                            }
                        }
                        case "/name" -> {
                            Path filePath = Path.of("UserData.txt");
                            List<String> readLines = Files.readAllLines(filePath);

                            List<String> lines = new ArrayList<>();
                            for (String line : readLines) {
                                String[] split = line.split(";");
                                if (split[0].equals(server.clients.get(clientSocket.hashCode()).name)) {
                                    String split0 = split[0].replace(split[0], envelope.getUsername());
                                    line = "%s;%s".formatted(split0, split[1]);
                                }
                                lines.add(line);
                            }
                            Files.write(filePath, lines);

                            log.info(String.format("%d set name to:(%s)%n", clientSocket.hashCode(), envelope.getUsername()));
                            server.clients.get(this.clientSocket.hashCode()).name = envelope.getUsername();
                            PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
                            Envelope response = new Envelope();
                            response.setSenderHashCode(0);
                            response.setCommand("/msg");
                            response.setRecipientHashCode(clientSocket.hashCode());
                            response.setUsername("Server");
                            response.setText("Name change received. You are now: " + envelope.getUsername());
                            response.setLogedIn(true);

                            printWriter.println(GSON.toJson(response));
                            printWriter.flush();
                        }
                    }
                } else {
                    //is it a login or a registration
                    if (envelope.isLogin()) {
                        if (isUserKnown(envelope.getUsername())) {
                            if (isPasswordRight(envelope.getUsername(), envelope.getPassword())) {
                                server.clients.get(clientSocket.hashCode()).name = envelope.getUsername();
                                envelope.setLogedIn(true);
                                log.debug("client %d (%s) is now loged in".formatted(clientSocket.hashCode(), envelope.getUsername()));
                            } else {
                                envelope.setText("password wrong");
                            }
                        } else {
                            envelope.setText("username doesn't exist");
                        }
                    } else {
                        FileWriter writer = new FileWriter("UserData.txt", true);
                        BufferedWriter bufferedWriter = new BufferedWriter(writer);

                        if (!isUserKnown(envelope.getUsername())) {
                            server.clients.get(clientSocket.hashCode()).name = envelope.getUsername();
                            bufferedWriter.write("%s;%s".formatted(envelope.getUsername(), envelope.getPassword()));
                            bufferedWriter.newLine();
                            bufferedWriter.close();
                            envelope.setLogedIn(true);
                            log.debug("user registered");
                        } else {
                            log.debug("user already exists");
                            envelope.setText("user already exists");
                            envelope.setLogedIn(false);
                        }
                    }
                    envelope.setCommand("buffer");
                    PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
                    printWriter.println(GSON.toJson(envelope));
                    printWriter.flush();
                }
            } catch (IOException e) {
                int clientId = this.clientSocket.hashCode();
                server.clients.remove(clientId);
                log.warn(String.format("client %d disconnected.%n", clientId));
                return;
            } catch (Exception e) {
                log.error("Unexpected error: ", e);
            }
        }
    }

    private boolean isUserKnown(String username) throws IOException {
        try (FileInputStream inputStream = new FileInputStream("UserData.txt");
             InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
             BufferedReader bufferedReader = new BufferedReader(reader)) {

            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] split = line.split(";");
                if (split[0].equals(username)) {
                    log.debug("username found");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPasswordRight(String username, String password) throws IOException {
        try (FileInputStream inputStream = new FileInputStream("UserData.txt");
             InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
             BufferedReader bufferedReader = new BufferedReader(reader)) {

            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] split = line.split(";");
                if (split[0].equals(username)) {
                    if (split[1].equals(password)) {
                        log.debug("password right");
                        return true;
                    }
                }
            }
        }
        log.debug("password wrong");
        return false;
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
