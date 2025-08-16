import socket
import os

def send_image(image_path, server_ip, port=5000):
    img_size = os.path.getsize(image_path)

    # Connect to server
    client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client_socket.connect((server_ip, port))

    # Send image size first
    client_socket.sendall(str(img_size).encode())
    ack = client_socket.recv(16)  # Expect SIZE_OK

    # Send image bytes
    with open(image_path, "rb") as f:
        client_socket.sendall(f.read())

    # Receive caption
    caption = client_socket.recv(4096).decode()
    print(f"[CLIENT] Caption: {caption}")

    client_socket.close()

if __name__ == "__main__":
    image_path = input("Enter image file path: ").strip()
    server_ip = input("Enter server IP address: ").strip()
    send_image(image_path, server_ip)
