package edu.ktu.takepicture

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private val WRITE_EXTERNAL_STORAGE_REQUEST = 2
    private var savedImgUri: Uri? = null

    private var takePictureActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result ->
        if(result.resultCode == RESULT_OK)
        {
            processAndSetImage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.img_capture_btn).setOnClickListener {
            if (haveStoragePermission()) {
                requestCapture()
            } else {
                requestStoragePermission()
            }
        }
        loadImgUri()
    }

    private fun requestCapture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            savedImgUri = createImgUri()
            intent.putExtra(MediaStore.EXTRA_OUTPUT, savedImgUri)
            takePictureActivityResultLauncher.launch(intent)
        }
    }

    private fun processAndSetImage() {
        val img = findViewById<ImageView>(R.id.img)

        Glide.with(this)
            .load(savedImgUri)
            .into(img)
    }

    private fun getFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss").format(Date())
        return "IMG_${timestamp}"
    }

    private fun createImgUri(): Uri? {
        val fileName = getFileName()

        val imgCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val newImgDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        return contentResolver.insert(imgCollection, newImgDetails)
    }

    private fun saveImgUri() {
        val uri = savedImgUri.toString()
        val prefs = getSharedPreferences("file", Context.MODE_PRIVATE).edit()
        prefs.putString("img", uri)
        prefs.apply()
    }

    private fun loadImgUri() {
        val prefs = getSharedPreferences("file", Context.MODE_PRIVATE)
        val uri = prefs.getString("img", null)
        uri?.let {
            savedImgUri = Uri.parse(uri)
            processAndSetImage()
        }
    }

    override fun onPause() {
        super.onPause()
        saveImgUri()
    }

    private fun haveStoragePermission(): Boolean {
        // Devices with version Q+ can read and write external
        // files that belong to the app without a permission.
        // Devices with lower versions need to ask for permission
        // to write these files.
        // If you want to access images that other apps created,
        // Q+ devices would also need to request the permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun requestStoragePermission() {
        if (!haveStoragePermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_EXTERNAL_STORAGE_REQUEST
            )
        }
    }

    private fun showPermissionRequestRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.storage_permission_rationale_title))
            .setMessage(getString(R.string.storage_permission_rationale))
            .setPositiveButton(
                "OK")
                { dialogInterface, _ ->
                    dialogInterface.dismiss()
                    requestStoragePermission()
                }
            .setNegativeButton(
                "Cancel")
                { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestCapture()
            } else {
                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                if (shouldShowRationale) {
                    showPermissionRequestRationale()
                } else {
                    Snackbar.make(
                        findViewById(R.id.main_layout),
                        R.string.storage_permission_not_granted,
                        Snackbar.LENGTH_LONG
                    ).setAction(
                        getString(R.string.open_settings)
                    ) {
                        showSettings()
                    }.show()
                }
            }
        }
    }

    private fun showSettings() {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }
    }
}