package com.github.noigel5.server;

import java.net.Socket;

class ClientRef {
    Socket socket;
    String name;

    public ClientRef(Socket socket) {
        this.socket = socket;
    }
}
