import socket
import threading
import os
from PIL import Image
from transformers import BlipProcessor, BlipForConditionalGeneration

# Load BLIP model once at startup
print("[SERVER] Loading model, please wait...")
processor = BlipProcessor.from_pretrained("Salesforce/blip-image-captioning-base")
model = BlipForConditionalGeneration.from_pretrained("Salesforce/blip-image-captioning-base")
print("[SERVER] Model loaded.")

def generate_caption(image_path):
    image = Image.open(image_path).convert("RGB")
    inputs = processor(image, return_tensors="pt")
    out = model.generate(**inputs)
    return processor.decode(out[0], skip_special_tokens=True)

def handle_client(conn, addr):
    print(f"[SERVER] Connected by {addr}")

    # Receive image size first
    img_size_data = conn.recv(16).decode()
    img_size = int(img_size_data)
    conn.sendall(b"SIZE_OK")

    # Receive image bytes
    img_data = b""
    while len(img_data) < img_size:
        packet = conn.recv(4096)
        if not packet:
            break
        img_data += packet

    # Save image temporarily
    temp_path = "received_image.jpg"
    with open(temp_path, "wb") as f:
        f.write(img_data)

    print("[SERVER] Image received. Generating caption...")
    caption = generate_caption(temp_path)
    print(f"[SERVER] Caption: {caption}")

    # Send caption back to client
    conn.sendall(caption.encode())

    # Clean up
    conn.close()
    os.remove(temp_path)

def start_server(host="0.0.0.0", port=5000):
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((host, port))
    server_socket.listen(5)
    print(f"[SERVER] Listening on {host}:{port}")

    while True:
        conn, addr = server_socket.accept()
        thread = threading.Thread(target=handle_client, args=(conn, addr))
        thread.start()

if __name__ == "__main__":
    start_server()

