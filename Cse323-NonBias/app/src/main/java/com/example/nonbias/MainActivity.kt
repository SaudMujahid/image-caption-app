package com.example.nonbias

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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

    private var selectedBitmap: Bitmap? = null

    companion object {
        const val REQUEST_CAMERA = 100
        const val REQUEST_GALLERY = 101

        // Change to your server's IP address
        const val SERVER_IP = "192.168.1.113" // example
        const val SERVER_PORT = 5000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imagePreview = findViewById(R.id.imagePreview)
        btnCamera = findViewById(R.id.btnCamera)
        btnGallery = findViewById(R.id.btnGallery)
        btnSend = findViewById(R.id.btnSend)
        captionOverlayText = findViewById(R.id.captionOverlayText) // Add this TextView in XML for captions

        btnCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_CAMERA)
        }

        btnGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_GALLERY)
        }

        btnSend.setOnClickListener {
            selectedBitmap?.let {
                sendImageToServer(it)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                REQUEST_CAMERA -> {
                    val bitmap = data.extras?.get("data") as Bitmap
                    selectedBitmap = bitmap
                    imagePreview.setImageBitmap(bitmap)
                    btnSend.isEnabled = true
                }
                REQUEST_GALLERY -> {
                    val uri = data.data
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    selectedBitmap = bitmap
                    imagePreview.setImageBitmap(bitmap)
                    btnSend.isEnabled = true
                }
            }
        }
    }

    private fun sendImageToServer(bitmap: Bitmap) {
        thread {
            try {
                // Convert Bitmap to ByteArray
                val byteStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteStream)
                val imageBytes = byteStream.toByteArray()

                val socket = Socket(SERVER_IP, SERVER_PORT)
                val outStream = DataOutputStream(socket.getOutputStream())
                val inStream = DataInputStream(socket.getInputStream())

                // 1. Send image size
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
                    imagePreview.setImageDrawable(null) // clear image
                    btnCamera.isEnabled = false
                    btnGallery.isEnabled = false
                    btnSend.isEnabled = false
                    captionOverlayText.text = caption.trim()
                    captionOverlayText.visibility = View.VISIBLE
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
