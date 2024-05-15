package es.ubu.lsi.server;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import es.ubu.lsi.common.ChatMessage;

public class ChatServerImpl implements ChatServer {
    private static final int DEFAULT_PORT = 1500;
    private int clientId = 0;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private int port;
    private boolean alive;
    private List<ServerThreadForClient> clients;

    public ChatServerImpl(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
    }

    public void startup() {
        alive = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (alive) {
                Socket socket = serverSocket.accept();
                ServerThreadForClient clientThread = new ServerThreadForClient(socket, clientId++);
                clients.add(clientThread);
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        alive = false;
        try {
            for (ServerThreadForClient client : clients) {
                client.close();
            }
            clients.clear();
            System.out.println("Server shut down.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(ChatMessage message) {
        for (ServerThreadForClient client : clients) {
            client.sendMessage(message);
        }
    }

    public void remove(int id) {
        clients.removeIf(client -> client.getId() == id);
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        ChatServerImpl server = new ChatServerImpl(port);
        server.startup();
    }

    private class ServerThreadForClient extends Thread {
        private Socket socket;
        private int id;
        private ObjectInputStream in;
        private ObjectOutputStream out;

        public ServerThreadForClient(Socket socket, int id) {
            this.socket = socket;
            this.id = id;
        }

        public long getId() {
            return id;
        }

        public void sendMessage(ChatMessage message) {
            try {
                out.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() throws IOException {
            socket.close();
            in.close();
            out.close();
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                while (true) {
                    ChatMessage message = (ChatMessage) in.readObject();
                    if (message.getType() == ChatMessage.MessageType.LOGOUT) {
                        remove(id);
                        close();
                        break;
                    }
                    broadcast(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
