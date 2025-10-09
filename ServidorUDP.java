import java.net.*;
import java.io.*;
import java.util.*;

public class ServidorUDP {
    private static final int PORT = 5000;
    private static final int BUFFER_SIZE = 1024;

    // Mapa para almacenar la dirección (InetSocketAddress) y nombre de cada cliente
    private static Map<SocketAddress, String> clientes = new HashMap<>();

    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(PORT);
            byte[] buffer = new byte[BUFFER_SIZE];
            System.out.println("Servidor UDP escuchando en el puerto " + PORT);

            while (true) {
                DatagramPacket paqueteRecibido = new DatagramPacket(buffer, buffer.length);
                socket.receive(paqueteRecibido); // Espera datagramas de cualquier cliente

                String mensaje = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength(), "UTF-8").trim();
                SocketAddress direccionCliente = paqueteRecibido.getSocketAddress();

                if (!clientes.containsKey(direccionCliente)) {
                    // Primer mensaje del cliente = nombre de usuario
                    clientes.put(direccionCliente, mensaje);
                    System.out.println("[Nuevo cliente] " + mensaje + " desde " + direccionCliente);
                    broadcast(socket, "** " + mensaje + " se ha unido al chat **", direccionCliente);
                    continue;
                }

                String usuario = clientes.get(direccionCliente);

                if (mensaje.startsWith("/listar")) {
                    String listaUsuarios = String.join(", ", clientes.values());
                    enviar(socket, "Usuarios conectados: " + listaUsuarios, direccionCliente);
                } else if (mensaje.startsWith("/quitar")) {
                    clientes.remove(direccionCliente);
                    System.out.println("[Desconectado] " + usuario + " (" + direccionCliente + ")");
                    broadcast(socket, "** " + usuario + " ha abandonado el chat **", direccionCliente);
                } else {
                    String mensajeFormateado = usuario + ": " + mensaje;
                    broadcast(socket, mensajeFormateado, direccionCliente);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Enviar mensaje a todos los clientes menos el remitente
    private static void broadcast(DatagramSocket socket, String mensaje, SocketAddress origen) throws IOException {
        byte[] data = mensaje.getBytes("UTF-8");
        for (SocketAddress direccion : clientes.keySet()) {
            if (!direccion.equals(origen)) {
                DatagramPacket paquete = new DatagramPacket(data, data.length, direccion);
                socket.send(paquete);
            }
        }
    }

    // Enviar mensaje solo a un cliente
    private static void enviar(DatagramSocket socket, String mensaje, SocketAddress destino) throws IOException {
        byte[] data = mensaje.getBytes("UTF-8");
        DatagramPacket paquete = new DatagramPacket(data, data.length, destino);
        socket.send(paquete);
    }
}
