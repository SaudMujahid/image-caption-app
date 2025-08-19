package com.example.nonbias

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var btnCamera: Button
    private lateinit var btnGallery: Button
    private lateinit var btnSend: Button
    private lateinit var captionOverlayText: TextView

    private lateinit var ipInput: EditText
    private lateinit var portInput: EditText
    private lateinit var btnConnect: Button

    private var serverIp: String? = null
    private var serverPort: Int? = null
    private var isConnected = false

    private var selectedBitmap: Bitmap? = null

    private val CAMERA_PERMISSION_CODE = 200

    companion object {
        const val REQUEST_CAMERA = 100
        const val REQUEST_GALLERY = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipInput = findViewById(R.id.ipInput)
        portInput = findViewById(R.id.portInput)
        btnConnect = findViewById(R.id.btnConnect)

        imagePreview = findViewById(R.id.imagePreview)
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)
        btnSend = findViewById(R.id.btnSend)
        captionOverlayText = findViewById(R.id.captionOverlayText)

        btnConnect.setOnClickListener {
            val ip = ipInput.text.toString().trim()
            val port = portInput.text.toString().trim().toIntOrNull()

            if (ip.isNotEmpty() && port != null) {
                testConnection(ip, port)
            } else {
                Toast.makeText(this, "Enter valid IP and Port", Toast.LENGTH_SHORT).show()
            }
        }

        btnCamera.setOnClickListener {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            } else {
                openCamera()
            }
        }

        btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_GALLERY)
        }

        btnSend.setOnClickListener {
            selectedBitmap?.let {
                sendImageToServer(it)
            } ?: Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CAMERA -> {
                    val bitmap = data.extras?.get("data") as? Bitmap
                    if (bitmap != null) {
                        selectedBitmap = bitmap
                        imagePreview.setImageBitmap(bitmap)
                        btnSend.isEnabled = true
                    } else {
                        Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_GALLERY -> {
                    val uri: Uri? = data.data
                    if (uri != null) {
                        try {
                            val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                                val source = ImageDecoder.createSource(contentResolver, uri)
                                ImageDecoder.decodeBitmap(source)
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            }
                            selectedBitmap = bitmap
                            imagePreview.setImageBitmap(bitmap)
                            btnSend.isEnabled = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun testConnection(ip: String, port: Int) {
        thread {
            try {
                val socket = Socket(ip, port)
                socket.close()

                runOnUiThread {
                    Toast.makeText(this, "Connected to $ip:$port", Toast.LENGTH_SHORT).show()
                    serverIp = ip
                    serverPort = port
                    isConnected = true
                    btnSend.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    isConnected = false
                    btnSend.isEnabled = false
                }
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    private fun sendImageToServer(bitmap: Bitmap) {
        thread {
            try {
                val byteStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteStream)
                val imageBytes = byteStream.toByteArray()

                if (!isConnected || serverIp == null || serverPort == null) {
                    runOnUiThread {
                        Toast.makeText(this, "Not connected to server!", Toast.LENGTH_SHORT).show()
                    }
                    return@thread
                }

                val socket = Socket(serverIp, serverPort!!)
                val outStream = DataOutputStream(socket.getOutputStream())
                val inStream = DataInputStream(socket.getInputStream())

                // 1. Send image size as string
                outStream.write(imageBytes.size.toString().toByteArray())
                outStream.flush()

                // Wait for SIZE_OK
                val ackBuffer = ByteArray(16)
                inStream.read(ackBuffer)

                // 2. Send image bytes
                outStream.write(imageBytes)
                outStream.flush()

                // 3. Receive caption
                val buffer = ByteArray(4096)
                val bytesRead = inStream.read(buffer)
                val caption = String(buffer, 0, bytesRead)

                // 4. Update UI
                runOnUiThread {
                    captionOverlayText.text = caption.trim()
                    captionOverlayText.visibility = View.VISIBLE

                    // Re-enable camera and gallery for next round
                    btnCamera.isEnabled = true
                    btnGallery.isEnabled = true
                    btnSend.isEnabled = false  // disable until a new image is chosen
                    selectedBitmap = null
                }


                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    captionOverlayText.text = "Error: ${e.message}"
                }
            }
        }
    }
}
