package cat.dam.andy.picturestogallery_kt

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var ivImatge: ImageView
    private lateinit var btnFoto: Button
    private lateinit var btnGaleria: Button
    private var uriPhotoImage: Uri? = null
    private val context: Context = this
    private var permissionManager = PermissionManager(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initPermissions()
        initListeners()
    }

    private fun initViews() {
        ivImatge = findViewById(R.id.iv_foto)
        btnFoto = findViewById(R.id.btn_foto)
        btnGaleria = findViewById(R.id.btn_galeria)
    }

    private fun initPermissions() {
        permissionManager.addPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            getString(R.string.writeExternalStoragePermissionInfo),
            getString(R.string.writeExternalStoragePermissionNeeded),
            getString(R.string.writeExternalStoragePermissionDenied),
            getString(R.string.writeExternalStoragePermissionThanks),
            getString(R.string.writeExternalStoragePermissionSettings)
        )
    }

    private fun initListeners() {
        btnGaleria.setOnClickListener {
            if (!permissionManager.hasAllNeededPermissions()) {
                permissionManager.askForPermissions(permissionManager.getRejectedPermissions())
            } else {
                openGallery()
            }
        }
        btnFoto.setOnClickListener() {
            if (!permissionManager.hasAllNeededPermissions()) {
                permissionManager.askForPermissions(permissionManager.getRejectedPermissions())
            } else {
                takePicture()
            }
        }
    }


    private val activityResultLauncherGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        //here we will handle the result of our intent
        if (result.resultCode == RESULT_OK) {
            //image picked
            //get uri of image
            val data = result.data
            if (data != null) {
                val imageUri = data.data
                println("galeria: $imageUri")
                ivImatge.setImageURI(imageUri)
            }
        } else {
            //cancelled
            Toast.makeText(this@MainActivity, "Cancelled...", Toast.LENGTH_SHORT).show()
        }
    }

    private val activityResultLauncherPhoto = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        //here we will handle the result of our intent
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show()
            ivImatge.setImageURI(uriPhotoImage) //Amb paràmetre EXIF podem canviar orientació (per defecte horiz en versions android antigues)
            refreshGallery() //refresca gallery per veure nou fitxer
            /* Intent data = result.getData(); //si volguessim només la miniatura
        Uri imageUri = data.getData();
        ivImatge.setImageURI(imageUri);*/
        } else {
            //cancelled
            Toast.makeText(this@MainActivity, "Cancelled...", Toast.LENGTH_SHORT).show()
        }
    }


    @SuppressLint("QueryPermissionsNeeded")
    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            intent.action = Intent.ACTION_GET_CONTENT
            if (intent.resolveActivity(packageManager) != null) {
                activityResultLauncherGallery.launch(Intent.createChooser(intent, "Select File"))
            } else {
                Toast.makeText(
                    this@MainActivity, "El seu dispositiu no permet accedir a la galeria",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun takePicture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(
                    this@MainActivity, "Error en la creació del fitxer",
                    Toast.LENGTH_SHORT
                ).show()
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val values = ContentValues()
                values.put(MediaStore.Images.Media.TITLE, getString(R.string.picture_title))
                values.put(
                    MediaStore.Images.Media.DESCRIPTION,
                    getString(R.string.picture_time) + " " + System.currentTimeMillis()
                )
                val uriImage = FileProvider.getUriForFile(
                    this,
                    this.packageName + ".provider",  //(use your app signature + ".provider" )
                    photoFile
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uriImage)
                activityResultLauncherPhoto.launch(intent)
            } else {
                Toast.makeText(
                    this@MainActivity, getString(R.string.picture_creation_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                this@MainActivity, getString(R.string.camera_access_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun createImageFile(): File? {
        val wasSuccessful: Boolean //just for testing mkdirs
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val imageFileName = "IMG_" + timeStamp + "_"
        // File storageDir = getFilesDir();//no es veurà a la galeria
        // File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES+File.separator+this.getPackageName());//No es veurà a la galeria
        val storageDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + File.separator + this.packageName)
        //NOTE: MANAGE_EXTERNAL_STORAGE is a special permission only allowed for few apps like Antivirus, file manager, etc. You have to justify the reason while publishing the app to PlayStore.
        wasSuccessful = if (!storageDir.exists()) {
            storageDir.mkdir()
        } else {
            storageDir.mkdirs()
        }
        if (wasSuccessful) {
            println("storageDir: $storageDir")
        } else {
            println("storageDir: $storageDir was not created")
        }
        // Save a file: path for use with ACTION_VIEW intents
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        // Save a file: path for use with ACTION_VIEW intents
        val currentPhotoPath = image.absolutePath
        uriPhotoImage = Uri.fromFile(image)
        println("file: $uriPhotoImage")
        return image
    }

    private fun refreshGallery() {
        //Cal refrescar per poder veure la foto creada a la galeria
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        mediaScanIntent.data = uriPhotoImage
        this.sendBroadcast(mediaScanIntent)
    }

}