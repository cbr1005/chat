package es.ubu.lsi.client;

import java.io.*;
import java.net.Socket;
import es.ubu.lsi.common.ChatMessage;

public class ChatClientImpl implements ChatClient {
    private String server;
    private String username;
    private int port;
    private boolean carryOn = true;
    private int id;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public ChatClientImpl(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    @Override
    public boolean start() {
        try {
            socket = new Socket(server, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            new Thread(new ChatClientListener()).start();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void sendMessage(ChatMessage msg) {
        try {
            out.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        try {
            carryOn = false;
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String server = "localhost";
        String username = args.length > 1 ? args[1] : "defaultUser";
        if (args.length > 0) {
            server = args[0];
        }
        ChatClientImpl client = new ChatClientImpl(server, 1500, username);
        if (client.start()) {
            client.handleConsoleInput();
        }
    }

    private void handleConsoleInput() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while (carryOn && (line = br.readLine()) != null) {
                if (line.equalsIgnoreCase("logout")) {
                    sendMessage(new ChatMessage(id, ChatMessage.MessageType.LOGOUT, ""));
                    disconnect();
                } else {
                    sendMessage(new ChatMessage(id, ChatMessage.MessageType.MESSAGE, line));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ChatClientListener implements Runnable {
        @Override
        public void run() {
            try {
                while (carryOn) {
                    ChatMessage msg = (ChatMessage) in.readObject();
                    System.out.println(msg.getMessage());
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
