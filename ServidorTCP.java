import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServidorTCP {
    private static final int PORT = 5000;
    private static final ExecutorService pool = Executors.newCachedThreadPool(); // Manejo de hilos
    private static final List<ClientHandler> clientes = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor TCP escuchando en el puerto " + PORT);

            while (true) {
                Socket socket = serverSocket.accept(); // Espera conexiones entrantes
                ClientHandler handler = new ClientHandler(socket);
                synchronized (clientes) {
                    clientes.add(handler);
                }
                pool.execute(handler); // Crea un hilo por cliente
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clase interna que maneja a cada cliente en un hilo separado
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String nombre;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                // Primer mensaje: nombre de usuario
                nombre = in.readLine();
                if (nombre == null || nombre.trim().isEmpty()) {
                    nombre = "Anonimo";
                }
                System.out.println("[Conectado] " + nombre + " desde " + socket.getRemoteSocketAddress());
                broadcast("** " + nombre + " se ha unido al chat **", this);

                String mensaje;
                while ((mensaje = in.readLine()) != null) {
                    mensaje = mensaje.trim();
                    if (mensaje.isEmpty()) continue;

                    if (mensaje.startsWith("/listar")) {
                        String listaUsuarios;
                        synchronized (clientes) {
                            listaUsuarios = String.join(", ", getNombres());
                        }
                        out.println("Usuarios conectados: " + listaUsuarios);
                    } else if (mensaje.startsWith("/quitar")) {
                        break; // sale del bucle, cierra conexión
                    } else {
                        broadcast(nombre + ": " + mensaje, this);
                    }
                }
            } catch (IOException e) {
                System.out.println("[Error] Cliente desconectado inesperadamente: " + e.getMessage());
            } finally {
                cerrarConexion();
            }
        }

        private void cerrarConexion() {
            try {
                synchronized (clientes) {
                    clientes.remove(this);
                }
                System.out.println("[Desconectado] " + nombre + " (" + socket.getRemoteSocketAddress() + ")");
                broadcast("** " + nombre + " ha abandonado el chat **", this);
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void broadcast(String mensaje, ClientHandler origen) {
            synchronized (clientes) {
                for (ClientHandler cliente : clientes) {
                    if (cliente != origen) {
                        cliente.out.println(mensaje);
                    }
                }
            }
        }

        private List<String> getNombres() {
            List<String> nombres = new ArrayList<>();
            for (ClientHandler c : clientes) {
                nombres.add(c.nombre);
            }
            return nombres;
        }
    }
}
