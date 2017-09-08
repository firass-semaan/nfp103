/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Objets;

import java.io.Serializable;

/**
 *
 * @author Firass Semaan
 */
public class Message implements Serializable {

    public String command, type, sender, recipient;
    public Avion content;
    public int senderId, recipientId;

    public Message(String command, String type, String sender, Avion content, String recipient) {
        this.command = command;
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.recipient = recipient;
    }

    @Override
    public String toString() {
        return "{type='" + type + "', sender='" + sender + "', content='" + content.getInfo() + "', recipient='" + recipient + "'}";
    }

}
