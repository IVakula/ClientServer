package my.home.client;

import java.io.*;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {

    private static final String EXIT_COMMAND = "-exit";
    private static final String FILE_COMMAND = "-file";
    private Socket socket;
    private DataOutputStream out;

    private final AtomicBoolean exitCommandSent = new AtomicBoolean(false);


    public void start() {
        try (Socket socket = new Socket("127.0.0.1", 8080)) {
            this.socket = socket;
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(this::listenIncomingMessages).start();
            sendMessages();
            System.out.println("Good bye!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenIncomingMessages() {
        try {
            Scanner scanner = new Scanner(socket.getInputStream());
            while (!exitCommandSent.get()) {
                try {
                    System.out.println(scanner.nextLine());
                } catch (NoSuchElementException e) {
                    System.out.println("Disconnected from server");
                    exitCommandSent.set(true);
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessages() {
        try {
            Scanner scanner = new Scanner(System.in);
            while (!exitCommandSent.get()) {
                String messageToServer = scanner.nextLine();
                if (EXIT_COMMAND.equals(messageToServer)) {
                    exitCommandSent.set(true);
                    sendMessageToServer(messageToServer);
                } else if (messageToServer.startsWith(FILE_COMMAND)) {
                    String fileName = messageToServer.replace(FILE_COMMAND, "").trim();
                    processFileUpload(fileName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processFileUpload(String fileName) throws IOException {
        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        byte[] fileContent = fis.readAllBytes();

        sendMessageToServer(FILE_COMMAND);
        sendMessageToServer(String.valueOf(fileContent.length));
        sendMessageToServer(file.getName());
        this.out.write(fileContent);
        this.out.flush();
    }

    private void sendMessageToServer(String message) throws IOException {
        this.out.writeUTF(message);
        this.out.flush();
    }
}
