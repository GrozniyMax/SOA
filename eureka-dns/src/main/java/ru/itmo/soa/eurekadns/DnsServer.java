package ru.itmo.soa.eurekadns;

import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Section;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.function.UnaryOperator;

/**
 * DNS-сервер поверх DiscoveryResolver. Слушает TCP (length-prefix, как это делает HAProxy)
 * и UDP (truncation с флагом TC, как того ждёт Nginx) на одном порту.
 */
public final class DnsServer {
    /** Максимальный UDP-ответ в байтах; больший усекается и помечается TC. */
    private static final int UDP_MAX = 512;

    private final UnaryOperator<Message> resolver;
    private final int requestedPort;

    private ServerSocket tcpServer;
    private DatagramSocket udpServer;
    private Thread tcpThread;
    private Thread udpThread;

    public DnsServer(UnaryOperator<Message> resolver, int port) {
        this.resolver = resolver;
        this.requestedPort = port;
    }

    public void start() throws IOException {
        tcpServer = new ServerSocket(requestedPort);
        udpServer = new DatagramSocket(tcpServer.getLocalPort());
        tcpThread = new Thread(this::serveTcp, "dns-tcp");
        udpThread = new Thread(this::serveUdp, "dns-udp");
        tcpThread.setDaemon(true);
        udpThread.setDaemon(true);
        tcpThread.start();
        udpThread.start();
    }

    public int getPort() {
        return tcpServer.getLocalPort();
    }

    public void stop() {
        close(tcpServer);
        close(udpServer);
        interrupt(tcpThread);
        interrupt(udpThread);
    }

    private void serveTcp() {
        while (!Thread.currentThread().isInterrupted()) {
            try (Socket socket = tcpServer.accept()) {
                socket.setSoTimeout(10_000);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                // HAProxy шлёт несколько length-prefixed запросов на одном соединении.
                while (true) {
                    int size = in.readUnsignedShort();
                    byte[] wire = new byte[size];
                    in.readFully(wire);
                    byte[] response = resolver.apply(new Message(wire)).toWire();
                    out.writeShort(response.length);
                    out.write(response);
                    out.flush();
                }
            } catch (SocketException | EOFException e) {
                // клиент закрыл соединение — ждём следующее
            } catch (IOException | RuntimeException e) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
            }
        }
    }

    private void serveUdp() {
        byte[] buffer = new byte[UDP_MAX * 8];
        while (!Thread.currentThread().isInterrupted()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpServer.receive(packet);
                byte[] query = java.util.Arrays.copyOf(packet.getData(), packet.getLength());
                byte[] response = resolver.apply(new Message(query)).toWire();
                if (response.length > UDP_MAX) {
                    // Усечённый ответ: только вопрос + флаг TC, клиент переспросит по TCP.
                    Message truncated = new Message(query);
                    truncated.getHeader().setFlag(Flags.TC);
                    truncated.removeAllRecords(Section.ANSWER);
                    truncated.removeAllRecords(Section.AUTHORITY);
                    truncated.removeAllRecords(Section.ADDITIONAL);
                    response = truncated.toWire();
                }
                DatagramPacket reply = new DatagramPacket(response, response.length,
                        packet.getAddress(), packet.getPort());
                udpServer.send(reply);
            } catch (IOException e) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
            }
        }
    }

    private static void close(ServerSocket socket) {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
            // уже закрыт
        }
    }

    private static void close(DatagramSocket socket) {
        if (socket != null) {
            socket.close();
        }
    }

    private static void interrupt(Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }
}