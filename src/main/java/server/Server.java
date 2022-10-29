package server;

import common.LevelLog;
import common.Log;
import service.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private ServerSocket serverSocket;
    private final Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    public Log log = Log.getInstance();

    public Server(Service service) {
        try {
            this.serverSocket = new ServerSocket(service.getPort());
            log.log(LevelLog.INFO, "Server start");
        } catch (IOException e) {
            e.printStackTrace();
            log.log(LevelLog.ERROR, "Error initialization serverSocket {" + e.getMessage() + "}");
        }
    }

    public boolean check(String nickName) {
        return this.clientHandlers.stream()
                .anyMatch(e -> e.getNickName() != null && e.getNickName().equalsIgnoreCase(nickName));
    }

    public void sendMessageAllClientsFromClient(ClientHandler client, String message) {
        for (ClientHandler clientHandler : clientHandlers) {
            if (!client.getNickName().equals(clientHandler.getNickName())) {
                clientHandler.sendMessage(getLocalTimeNow() + " " + client.getNickName() + ": " + message);
            }
        }
        log.log(LevelLog.INFO, client.getNickName() + " send to all clients message: {" + message + "}");
    }

    public void serverNotification(ClientHandler client, String message) {
        if (client != null) {
            for (ClientHandler clientHandler : clientHandlers) {
                if (!client.getNickName().equals(clientHandler.getNickName())) {
                    clientHandler.sendMessage(getLocalTimeNow() + " " + message);
                }
            }
        } else {
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.sendMessage(getLocalTimeNow() + " SERVER: " + message);
            }
        }
        log.log(LevelLog.INFO, "SERVER send to all clients: {" + message + "}");
    }

    public void serverSendMessageToClient(ClientHandler clientHandler, String message) {
        if (clientHandler != null) {
            clientHandler.sendMessage(getLocalTimeNow() + " SERVER: " + message);
            log.log(LevelLog.INFO,
                    clientHandler.getNickName() != null ?
                            clientHandler.getNickName() : "SERVER send message to client " + clientHandler.getIp()
                            + " {" + message + "}");
        }
    }

    private String getLocalTimeNow() {
        return "[" + LocalTime.now().format(timeFormatter) + "]";
    }

    public void serverStart() throws IOException {
        try {
            while (true) {
                Socket socket = this.serverSocket.accept();
                System.out.println("Новое подключение...");

                new ClientHandler(socket, this);
                log.log(LevelLog.INFO, "Add new Connection");
            }
        } finally {
            log.log(LevelLog.INFO, "Server stop and close");
            this.serverSocket.close();
        }
    }

    public boolean removeFromGroup(ClientHandler clientHandler) {
        if (this.clientHandlers.contains(clientHandler)) {
            clientHandler.interrupt();
            this.clientHandlers.remove(clientHandler);
            return true;
        }
        return false;
    }

    public void addToGoup(ClientHandler clientHandler) {
        this.clientHandlers.add(clientHandler);
    }
}
