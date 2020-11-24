package edu.ktu.takepicture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    val IMAGE_CAPTURE_REQUEST = 1
    var imgPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.img_capture_btn).setOnClickListener {
            requestCapture()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus)
        {
            val prefs = getSharedPreferences("file", Context.MODE_PRIVATE)
            if(prefs.contains("img"))
            {
                imgPath = prefs.getString("img", "")
                processAndSetImage()
            }
        }
    }

    private fun requestCapture()
    {
        val intent = Intent()
        if(intent.resolveActivity(packageManager) != null)
        {
            val imgFile = createTempFile()
            val imgUri = FileProvider.getUriForFile(
                this,
                "edu.ktu.takepicture.fileprovider",
                imgFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri)
            startActivityForResult(intent, IMAGE_CAPTURE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == IMAGE_CAPTURE_REQUEST && resultCode == RESULT_OK)
        {
            //val bitmap = data?.extras?.get("data") as Bitmap

            processAndSetImage()
            notifyGallery()
        }
    }

    private fun processAndSetImage()
    {
        val img = findViewById<ImageView>(R.id.img)
        val targetW = img.width
        val targetH = img.height

        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true

        BitmapFactory.decodeFile(imgPath, bmOptions)
        val imgW = bmOptions.outWidth
        val imgH = bmOptions.outHeight

        val scale = Math.max(1, Math.min(imgW/targetW, imgH/targetH))

        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scale

        var bitmap = BitmapFactory.decodeFile(imgPath, bmOptions)

        Toast.makeText(this,
            "Image resized from ${imgW}x${imgH} to ${bitmap.width}x${bitmap.height}",
            Toast.LENGTH_LONG
        ).show()

        val exif = ExifInterface(imgPath!!)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL)

        when(orientation)
        {
            ExifInterface.ORIENTATION_ROTATE_90 -> bitmap = rotateBitmap(bitmap,
                90)
            ExifInterface.ORIENTATION_ROTATE_180 -> bitmap = rotateBitmap(bitmap,
            180)
            ExifInterface.ORIENTATION_ROTATE_270 -> bitmap = rotateBitmap(bitmap,
                270)
        }

        img.setImageBitmap(bitmap)
    }

    private fun rotateBitmap(bitmap: Bitmap, deg: Int) : Bitmap
    {
        val mat = Matrix()
        mat.postRotate(deg.toFloat())
        return Bitmap.createBitmap(bitmap,
            0, 0,
            bitmap.width, bitmap.height,
            mat,
            true)
    }

    private fun notifyGallery()
    {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also {
            intent ->
            intent.data = Uri.fromFile(File(imgPath))
            sendBroadcast(intent)
        }
    }

    private fun createTempFile(): File
    {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "IMG_${timestamp}",
            ".jpg",
            storageDir
        ).apply {
            imgPath = absolutePath
        }
    }

    override fun onPause() {
        super.onPause()

        val prefs = getSharedPreferences("file", Context.MODE_PRIVATE).edit()
        if(imgPath != null)
        {
            prefs.putString("img", imgPath)
        }
        else
        {
            prefs.remove("img")
        }
        prefs.apply()
    }
}