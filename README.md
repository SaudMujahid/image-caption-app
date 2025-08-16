# 📸 Image Caption App

This project is a **client–server image captioning system**:  
- The **Android app** lets you capture or pick an image and send it to a server.  
- The **Python server** processes the image, generates a caption using a deep learning model, and sends it back to the app.  

---

## 🚀 Features
- Take a photo with the camera or select one from the gallery  
- Send the image to a Python backend via sockets  
- Receive an AI-generated caption  
- Display the caption directly inside the app  

---

## 🛠️ Server Setup (Python)

1. **Clone this repository**
   ```bash
   git clone https://github.com/saudmujahid/image-caption-app.git
   cd image-caption-app
   ```

2. **Create a virtual environment**
   ```bash
   python3 -m venv venv
   source venv/bin/activate   # Linux / macOS
   venv\Scripts\activate      # Windows PowerShell
   ```

3. **Install dependencies**
   ```bash
   pip install --upgrade pip
   pip install torch torchvision pillow transformers
   ```


4. **Run the server**
   ```bash
   python server.py
   ```

   By default it runs on port **5000**.  

   ⚠️ Ensure that the server’s IP/Port matches the values in your Android app (`SERVER_IP` and `SERVER_PORT` in `MainActivity.kt`).

---

## 📱 Android App Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/saudmujahid/image-caption-app.git
   ```

2. **Open in Android Studio**
   - Launch **Android Studio**  
   - Select **Open Project** and choose the `image-caption-app/android/` folder (where your `app/` module is).  

3. **Sync Gradle**
   Android Studio will fetch all required dependencies automatically.  

4. **Run the App**
   - Connect an Android device (or start an emulator)  
   - Press **Run ▶️**  

---

## 🌐 Networking Notes
- If running **locally on Wi-Fi**:  
  - Find your machine’s local IP with `ip addr` (Linux/macOS) or `ipconfig` (Windows).  
  - Replace `SERVER_IP` in `MainActivity.kt` with that IP (e.g., `192.168.1.113`).  
  - Make sure your Android device is on the same Wi-Fi network.  

- If running on a **cloud/VPS**:  
  - Use your public IP in `SERVER_IP`.  
  - Open port `5000` in your firewall/security group.  

---

## 📂 Project Structure
```
image-caption-app/
│
├── android/         # Android client (Kotlin project)
│   └── app/
│
├── server.py        # Python server
│
└── README.md
```

---


## 📜 License
MIT License
