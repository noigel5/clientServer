package com.github.noigel5.server;

import com.github.noigel5.model.Envelope;
import com.github.noigel5.model.LoginEnvelope;
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
                    final Path path = Path.of("UserData.txt");
                    switch (envelope.getCommand()) {
                        case "/clients" -> {
                            PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());

                            // V1
                            Envelope response = getResponseEnvelope();
                            response.setText(getClientsResponseText());
                            printWriter.println(GSON.toJson(response));
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
                            List<String> lines = new ArrayList<>();
                            List<String> readLines = Files.readAllLines(path);
                            for (String line : readLines) {
                                LoginEnvelope loginEnvelope = GSON.fromJson(line, LoginEnvelope.class);
                                if (loginEnvelope.getUsername().equals(server.clients.get(clientSocket.hashCode()).name)) {
                                    loginEnvelope.setUsername(envelope.getUsername());
                                }
                                lines.add(GSON.toJson(loginEnvelope));
                            }
                            Files.write(path, lines);

                            log.info(String.format("%d set name to:(%s)%n", clientSocket.hashCode(), envelope.getUsername()));
                            server.clients.get(this.clientSocket.hashCode()).name = envelope.getUsername();
                            PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
                            Envelope response = getResponseEnvelope();
                            response.setText("Name change received. You are now: " + envelope.getUsername());

                            printWriter.println(GSON.toJson(response));
                            printWriter.flush();
                        }
                        case "/password" -> {
                            List<String> lines = new ArrayList<>();
                            List<String> readLines = Files.readAllLines(path);
                            for (String line : readLines) {
                                LoginEnvelope loginEnvelope = GSON.fromJson(line, LoginEnvelope.class);
                                if (loginEnvelope.getUsername().equals(server.clients.get(clientSocket.hashCode()).name)) {
                                    loginEnvelope.setPassword(envelope.getPassword());
                                }
                                lines.add(GSON.toJson(loginEnvelope));
                            }
                            Files.write(path, lines);

                            log.info("password set");
                            server.clients.get(this.clientSocket.hashCode()).name = envelope.getPassword();
                            PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream());
                            Envelope response = getResponseEnvelope();
                            response.setText("Password change received and set successfully");

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
                            LoginEnvelope loginEnvelope = new LoginEnvelope();
                            loginEnvelope.setUsername(envelope.getUsername());
                            loginEnvelope.setPassword(envelope.getPassword());
                            bufferedWriter.write(GSON.toJson(loginEnvelope));
                            bufferedWriter.newLine();
                            bufferedWriter.close();
                            envelope.setLogedIn(true);
                            log.debug("user %d (%s) registered".formatted(clientSocket.hashCode(), envelope.getUsername()));
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

    private Envelope getResponseEnvelope() {
        Envelope response = new Envelope();
        response.setSenderHashCode(0);
        response.setCommand("/msg");
        response.setRecipientHashCode(clientSocket.hashCode());
        response.setUsername("Server");
        response.setLogedIn(true);
        return response;
    }

    private boolean isUserKnown(String username) throws IOException {
        try (FileInputStream inputStream = new FileInputStream("UserData.txt");
             InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
             BufferedReader bufferedReader = new BufferedReader(reader)) {

            String line;

            while ((line = bufferedReader.readLine()) != null) {
                LoginEnvelope loginEnvelope = GSON.fromJson(line, LoginEnvelope.class);
                if (loginEnvelope.getUsername().equals(username)) {
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
                LoginEnvelope loginEnvelope = GSON.fromJson(line, LoginEnvelope.class);
                if (loginEnvelope.getUsername().equals(username)) {
                    if (loginEnvelope.getPassword().equals(password)) {
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
