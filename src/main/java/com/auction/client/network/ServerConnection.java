package com.auction.client.network;

import com.auction.network.Message;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

public class ServerConnection implements Closeable {

    private final Socket socket;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;

    public ServerConnection(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.inputStream = new ObjectInputStream(socket.getInputStream());
    }

    public Message send(Message.Type type, Map<String, Object> payload) throws IOException, ClassNotFoundException {
        return send(new Message(type, payload));
    }

    public Message send(Message message) throws IOException, ClassNotFoundException {
        outputStream.writeObject(message);
        outputStream.flush();

        Object response = inputStream.readObject();
        if (!(response instanceof Message serverMessage)) {
            throw new IOException("Unexpected response type from server");
        }
        return serverMessage;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        outputStream.close();
        socket.close();
    }
}
