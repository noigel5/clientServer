package com.github.noigel5;

import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        final Socket clientSocket;
        final BufferedReader in;
        final PrintWriter out;

        try {
            clientSocket = new Socket("127.0.0.1", 5000); // TODO: parametrisieren
            OutputStream outputStream = clientSocket.getOutputStream();
            out = new PrintWriter(outputStream);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            Thread sender = new Thread(new Writer(out));
            sender.start();


            Thread receive = new Thread(new Receiver(clientSocket, in, out));
            receive.start();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
