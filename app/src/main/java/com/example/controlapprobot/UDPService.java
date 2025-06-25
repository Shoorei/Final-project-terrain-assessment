package com.example.controlapprobot;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPService {

    private static final String TAG = "UDPService";
    private final int portSend;     // Port to send control commands (to ESP32)
    private final int portReceive;  // Port to receive sensor data (from ESP32)
    private final int packetSize;
    private DatagramSocket receiveSocket;
    private DatagramSocket sendSocket;
    private ExecutorService executor;
    private volatile boolean running = false;
    private InetAddress esp32Address = null;

    public interface OnDataReceivedListener {
        void onDataReceived(String data);
    }

    private OnDataReceivedListener listener;

    public UDPService(int portSend, int portReceive, int packetSize) {
        this.portSend = portSend;
        this.portReceive = portReceive;
        this.packetSize = packetSize;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void setListener(OnDataReceivedListener listener) {
        this.listener = listener;
    }

    public void startReceiving() {
        if (running) return;
        running = true;

        executor.execute(() -> {
            try {
                receiveSocket = new DatagramSocket(portReceive);
                byte[] buffer = new byte[packetSize];

                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    receiveSocket.receive(packet);

                    String data = new String(packet.getData(), 0, packet.getLength());
                    esp32Address = packet.getAddress(); // Store ESP32 address
//                    Log.d(TAG, "ESP32 IP dynamically assigned: " + esp32Address.getHostAddress());
//                    Log.d(TAG, "Received: " + data + " from " + esp32Address.getHostAddress());

                    if (listener != null) {
                        listener.onDataReceived(data);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    Log.e(TAG, "Receiving error: ", e);
                }
            }
        });
    }

    public void stopReceiving() {
        running = false;
        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
        }
        receiveSocket = null;
    }

    public void sendCommand(String command) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Thread started for command: " + command);

                if (esp32Address == null) {
                    Log.w(TAG, "ESP32 address not yet known, can't send command.");
                    return;
                }

                DatagramSocket socket = new DatagramSocket();
                byte[] data = command.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, esp32Address, portSend);
                socket.send(packet);
                socket.close();

                Log.d(TAG, "Sent: " + command + " to " + esp32Address.getHostAddress());
            } catch (Exception e) {
                Log.e(TAG, "Thread sending error: ", e);
            }
        }).start();
    }

    public void shutdown() {
        stopReceiving();

        if (sendSocket != null && !sendSocket.isClosed()) {
            sendSocket.close();
        }
        sendSocket = null;

        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        Log.d(TAG, "UDPService shut down.");
    }
}
