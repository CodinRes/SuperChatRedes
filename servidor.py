import socket
import threading

HOST = '0.0.0.0'      # Escuchar en todas las interfaces de red (0.0.0.0) 
PORT = 5000           # Puerto de escucha para el chat (elegido arbitrariamente)

server_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM) #Se crea un socket TCP con socket.AF_INET (familia IPv4) y socket.SOCK_STREAM (tipo TCP)
server_sock.bind((HOST, PORT))
server_sock.listen(5)  # Cola de hasta 5 conexiones pendientes
print(f"Servidor escuchando en {HOST}:{PORT}")


clients = []  # lista de sockets de clientes conectados
clients_names = {}  # diccionario para mapear socket de cliente a su nombre de usuario

def atender_cliente(conn, addr):
    # Función que se ejecutará en un hilo para atender a un cliente en particular.
    print(f"[Conexión establecida] Cliente desde {addr}")
    try:
        # Primero, recibir el nombre de usuario que el cliente envía como identificación
        username = conn.recv(1024).decode('utf-8')
        if not username:
            conn.close()
            return
        username = username.strip()
        clients_names[conn] = username

        _broadcast(f"** {username} se ha unido al chat **", conn)

        # Bucle principal de recepción de mensajes de este cliente
        while True:
            data = conn.recv(1024) # Buffer de 1024 bytes, modificar segun la longitud maxima de mensajes deseado.
            if not data:
                # Si data viene vacío, el cliente cerró la conexión
                break
            mensaje = data.decode('utf-8').strip()
            if mensaje == '':
                continue  # ignorar mensajes vacíos (por ejemplo solo ENTER)
            # Manejo de comandos especiales:
            if mensaje.startswith('/listar'):
                # Preparar lista de usuarios conectados
                lista_usuarios = ", ".join(clients_names.values())
                conn.send(f"Usuarios conectados: {lista_usuarios}\n".encode('utf-8'))
            elif mensaje.startswith('/quitar'):
                # El cliente pidió desconectarse
                break  # salimos del bucle para cerrar conexión
            else:
                # Mensaje normal: reenviarlo a todos los demás clientes
                mensaje_formateado = f"{username}: {mensaje}"
                _broadcast(mensaje_formateado, conn)
    except Exception as e:
        print(f"[Error] Problema con el cliente {addr}: {e}")
    finally:
        # Cleanup al desconectarse el cliente
        if conn in clients:
            clients.remove(conn)
        nombre = clients_names.get(conn, "<desconocido>")
        print(f"[Desconexión] {nombre} ({addr}) se ha desconectado.")
        # Notificar a los demás que el usuario salió (opcional)
        _broadcast(f"** {nombre} ha abandonado el chat **", conn)
        clients_names.pop(conn, None)
        conn.close()

def _broadcast(mensaje, origen_conn=None):
    """Envía el mensaje a todos los clientes conectados, excepto al de origen (si se indica)."""
    data = mensaje.encode('utf-8')
    for client in clients:
        if client is origen_conn:
            continue
        try:
            client.send(data)
        except Exception as e:
            print(f"[Advertencia] No se pudo enviar a un cliente: {e}")


while True:
    conn, addr = server_sock.accept()  # Esperar una nueva conexión
    clients.append(conn)
    # Crear y lanzar un hilo para atender al nuevo cliente
    hilo = threading.Thread(target=atender_cliente, args=(conn, addr), daemon=True)
    hilo.start()
