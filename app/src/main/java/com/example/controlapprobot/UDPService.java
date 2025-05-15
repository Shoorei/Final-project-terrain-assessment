package com.example.controlapprobot;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPService {
    public interface UDPListener {
        void onSensorDataReceived(String data);
    }

    private final int portSend, portReceive, packetSize;
    private final String ipAddress;
    private boolean running = false;
    private UDPListener listener;
    private DatagramSocket sendSocket;
    private ExecutorService executor;
    private AsyncTask<Void, Void, Void> receiverTask;

    public UDPService(String ipAddress, int portSend, int portReceive, int packetSize) {
        this.ipAddress = ipAddress;
        this.portSend = portSend;
        this.portReceive = portReceive;
        this.packetSize = packetSize;
        executor = Executors.newSingleThreadExecutor();
    }

    public void setListener(UDPListener listener) {
        this.listener = listener;
    }

    public void startReceiving() {
        running = true;
        receiverTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try (DatagramSocket socket = new DatagramSocket(portReceive)) {
                    socket.setSoTimeout(2000);
                    byte[] buffer = new byte[packetSize];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    while (running) {
                        try {
                            socket.receive(packet);
                            String data = new String(packet.getData(), 0, packet.getLength());
                            if (listener != null) listener.onSensorDataReceived(data);
                        } catch (SocketTimeoutException ignored) {}
                    }
                } catch (IOException e) {
                    Log.e("UDPService", "Receiver error", e);
                }
                return null;
            }
        }.execute();
    }

    public void stopReceiving() {
        running = false;
        if (receiverTask != null) receiverTask.cancel(true);
    }

    public void sendCommand(final String command) {
        executor.submit(() -> {
            try {
                InetAddress address = InetAddress.getByName(ipAddress);
                DatagramSocket socket = new DatagramSocket();
                byte[] data = command.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, address, portSend);
                socket.send(packet);
                socket.close();
            } catch (IOException e) {
                Log.e("UDPService", "Send error", e);
            }
        });
    }

    public void shutdown() {
        stopReceiving();
        executor.shutdownNow();
    }
}