package com.github.noigel5.server;

import java.net.Socket;

class Client {
    Socket socket;
    String name;

    public Client(Socket socket) {
        this.socket = socket;
    }
}
