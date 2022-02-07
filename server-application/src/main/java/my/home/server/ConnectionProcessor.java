package my.home.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConnectionProcessor implements ClientDisconnectedListener {
    private static final int PORT = 8080;
    private final LinkedList<ClientProcessor> clientList = new LinkedList<>();
    private final AtomicInteger clientNumber = new AtomicInteger();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public void start() {
        System.out.println("Server started");

        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = server.accept();
                registerClient(socket);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void registerClient(Socket socket) {
        lock.writeLock().lock();
        String clientName = "client-" + (clientNumber.incrementAndGet());
        ClientProcessor clientProcessor = new ClientProcessor(clientName, socket);
        clientProcessor.setClientDisconnectionListener(this);
        broadcastClientConnectedMessage(clientName);
        clientList.add(clientProcessor);

        lock.writeLock().unlock();
    }


    @Override
    public void onClientDisconnected(String clientName) {
        lock.writeLock().lock();
        Iterator<ClientProcessor> itr = clientList.listIterator();
        while (itr.hasNext()) {
            ClientProcessor clientProcessor = itr.next();
            if (clientProcessor.getClientName().equals(clientName)) {
                itr.remove();
            } else {
                try {
                    clientProcessor.sendClientDisconnectedMessage(clientName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        lock.writeLock().unlock();
    }


    public void broadcastClientConnectedMessage(String clientName) {
        lock.writeLock().lock();
        for (ClientProcessor clientProcessor : clientList) {
            try {
                clientProcessor.sendClientConnectedMessage(clientName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        lock.writeLock().unlock();
    }
}
