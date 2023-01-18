package com.github.noigel5.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Envelope {
    private String command;
    private int senderHashCode;
    private int recipientHashCode;
    private String senderName;
    private String text;
}
