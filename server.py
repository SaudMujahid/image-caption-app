import socket
import threading
import os
from PIL import Image
import torch
from transformers import BlipProcessor, BlipForConditionalGeneration

# Device setup
device = "cuda" if torch.cuda.is_available() else "cpu"
print(f"[SERVER] Using device: {device}")

# Load BLIP-base model
print("[SERVER] Loading BLIP-base model, please wait...")
processor = BlipProcessor.from_pretrained(
    "Salesforce/blip-image-captioning-base",
    use_fast=True
)
model = BlipForConditionalGeneration.from_pretrained(
    "Salesforce/blip-image-captioning-base"
).to(device)
print("[SERVER] Model loaded.")

def generate_caption(image_path):
    image = Image.open(image_path).convert("RGB")
    inputs = processor(image, return_tensors="pt").to(device)
    with torch.no_grad():
        out = model.generate(**inputs)
    return processor.decode(out[0], skip_special_tokens=True)

def handle_client(conn, addr):
    print(f"[SERVER] Connected by {addr}")
    temp_path = "received_image.jpg"  # define early for cleanup

    try:
        # Receive image size
        img_size_data = conn.recv(16).decode().strip()
        if not img_size_data:
            print("[SERVER] No size received. Closing connection.")
            conn.close()
            return
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
        with open(temp_path, "wb") as f:
            f.write(img_data)

        print("[SERVER] Image received. Generating caption...")
        caption = generate_caption(temp_path)
        print(f"[SERVER] Caption: {caption}")

        # Send caption back
        conn.sendall(caption.encode())

    except Exception as e:
        print(f"[SERVER] Error: {e}")
        try:
            conn.sendall(b"ERROR")
        except:
            pass

    finally:
        conn.close()
        if os.path.exists(temp_path):
            os.remove(temp_path)

def start_server(host="0.0.0.0", port=5000):
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((host, port))
    server_socket.listen(5)
    print(f"[SERVER] Listening on {host}:{port}")

    while True:
        conn, addr = server_socket.accept()
        threading.Thread(target=handle_client, args=(conn, addr)).start()

if __name__ == "__main__":
    start_server()

