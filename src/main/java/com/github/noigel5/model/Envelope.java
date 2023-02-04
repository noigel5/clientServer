package com.github.noigel5.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Envelope {
    private String username;
    private String password;
    private boolean logedIn = false;
    private boolean login;
    private boolean waiting = false;

    private int senderHashCode;
    private int recipientHashCode;
    private String command;
    private String text;
}
