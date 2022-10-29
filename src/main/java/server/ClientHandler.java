package server;

import common.LevelLog;
import connection.TcpConnection;
import connection.TransportStringConnection;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public class ClientHandler extends Thread {
    private final Server server;
    private TcpConnection<String> connection;
    private String nickName = null;

    public ClientHandler(Socket socket, Server server) {
        this.server = server;
        try {
            this.connection = new TransportStringConnection(socket);
            start();
        } catch (Exception e) {
            e.printStackTrace();
            this.server.log.log(LevelLog.ERROR, "{" + e.getMessage() + "}");
        }
    }

    @Override
    public void run() {
        auth();
        while (this.connection.isConnected()) {
            String receive = getReceive();
            if (receive != null) {
                if (!isExit(receive)) {
                    break;
                }
                this.server.sendMessageAllClientsFromClient(this, receive);
            } else {
                break;
            }
        }
        this.connection.close();
    }

    private boolean isExit(String command) {
        if (command.equalsIgnoreCase("exit")) {
            String nickname = getNickName();
            if (this.server.removeFromGroup(this)) {
                this.server.serverNotification(null, nickname + " покинул чат.");
            }
            return false;
        }
        return true;
    }

    private void auth() {
        if (this.nickName == null) {
            this.server.serverSendMessageToClient(this, "Введите свой ник!");
            while (this.connection.isConnected()) {
                if (this.nickName == null) {
                    String receive = getReceive();
                    if (receive != null) {
                        if (!this.server.check(receive)) {
                            this.nickName = receive;
                            this.server.addToGoup(this);
                            this.server.serverSendMessageToClient(this,
                                    this.nickName + ", добро пожаловать в чат!!!");
                            this.server.log.log(LevelLog.INFO, this.nickName + " joined the chat!");
                            this.server.serverNotification(this,
                                    "SERVER: " + this.nickName + " подключился к чату!");
                        } else {
                            this.server.serverSendMessageToClient(this,
                                    "ник " + receive + " уже занят, напишите другой!");
                            this.server.log.log(LevelLog.WRONGAUTH, "The client has chosen a busy login!");
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
    }

    public void sendMessage(String message) {
        if (checkMessage(message)) {
            this.connection.send(message);
        }
    }

    private boolean checkMessage(String message) {
        return message != null && !message.equals("") && !message.equalsIgnoreCase("null");
    }

    public String getIp() {
        return this.connection.getIp();
    }

    private String getReceive() {
        try {
            return this.connection.receive();
        } catch (IOException e) {
            this.server.log.log(LevelLog.ERROR, "Error receive, client " + getNickName() + " : " + e.getMessage());
            this.connection.close();
        }
        return null;
    }

    public String getNickName() {
        return this.nickName != null ? this.nickName : getIp();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientHandler that = (ClientHandler) o;
        return nickName.equals(that.nickName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickName);
    }

}
