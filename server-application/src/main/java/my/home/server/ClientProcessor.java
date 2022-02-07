package my.home.server;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

public class ClientProcessor {
    private static final String EXIT_COMMAND = "-exit";
    private static final String FILE_COMMAND = "-file";
    private static final String SERVER_PREFIX = "[SERVER] ";

    private final Socket socket;
    private final LocalDateTime registrationTime;
    private final String clientName;
    private ClientDisconnectedListener clientDisconnectListener;

    public ClientProcessor(String clientName, Socket socket) {
        this.clientName = clientName;
        this.socket = socket;
        registrationTime = LocalDateTime.now();

        initThread();
    }

    private void initThread() {
        System.out.println(clientName + " connected");
        new Thread(() -> {
            boolean exitCommandReceived = false;
            try {

                //Hello message to client
                String helloMessage = SERVER_PREFIX +"You connected to server with name \"" + clientName
                        + "\"; Time: " + registrationTime;
                sendMessageToClient(helloMessage);
                DataInputStream in = new DataInputStream(socket.getInputStream());

                while (!exitCommandReceived) {
                    String messageFromClient = in.readUTF().trim();
                    if (EXIT_COMMAND.equals(messageFromClient)) {
                        exitCommandReceived = true;
                        System.out.println(clientName + " disconnected");

                    } else if (FILE_COMMAND.equals(messageFromClient)) {
                        String strLength = in.readUTF();
                        int contentLength = Integer.parseInt(strLength);
                        String fileName = in.readUTF();
                        FileOutputStream fos = new FileOutputStream("/home/ivakula/server/"+ fileName);
                        for (int i = 0; i < contentLength; i++) {
                            fos.write(in.readByte());
                        }
                        sendMessageToClient(SERVER_PREFIX + "File received.");
                    }
                }
            } catch (EOFException e) {
                System.out.println("Connection error. Disconnect " + clientName);
                clientDisconnectListener.onClientDisconnected(clientName);
            } catch (IOException e) {
                System.out.println("Connection error. " + e.getMessage());
                clientDisconnectListener.onClientDisconnected(clientName);
            }
            if (exitCommandReceived) {
                clientDisconnectListener.onClientDisconnected(clientName);
            }
        }).start();
    }

    public void sendClientDisconnectedMessage(String clientName) throws IOException {
        sendMessageToClient(SERVER_PREFIX + clientName + " disconnected.");
    }

    public void sendClientConnectedMessage(String clientName) throws IOException {
        sendMessageToClient(SERVER_PREFIX + clientName + " connected.");
    }

    private void sendMessageToClient(String message) throws IOException {
        socket.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().write(System.lineSeparator().getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
    }

    public void setClientDisconnectionListener(ClientDisconnectedListener clientDisconnectListener) {
        this.clientDisconnectListener = clientDisconnectListener;
    }

    public String getClientName() {
        return clientName;
    }
}
