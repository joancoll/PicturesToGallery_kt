package cat.dam.andy.picturestogallery_kt

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class PermissionManager(private val activityContext: Context) {
    data class PermissionData(
        var permission: String?,
        var permissionExplanation: String?,
        var permissionDeniedMessage: String?,
        var permissionGrantedMessage: String?,
        var permissionPermanentDeniedMessage: String?
    )

    private val permissionsRequired = mutableListOf<PermissionData>()
    private var activityResultLauncher: ActivityResultLauncher<Array<String>>? = null

    init {
        // Inicialitza el launcher per demanar permisos
        initPermissionLauncher()
    }

    private fun initPermissionLauncher() {
        // Inicialitza el launcher per demanar permisos
        activityResultLauncher =
            (activityContext as AppCompatActivity).registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
                ActivityResultCallback<Map<String, Boolean>> { permissions ->
                    // Check if all permissions are granted
                    if (permissions.containsValue(false)) {
                        // Check every permission
                        for (permissionKey in permissions.keys) {
                            val position = permissionsRequired.indexOfFirst { it.permission == permissionKey }

                            when {
                                permissions[permissionKey] == true -> {
                                    // Permission granted
                                    showAlert(
                                        R.string.permissionGranted,
                                        permissionsRequired[position].permissionGrantedMessage
                                    )
                                }
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    activityContext,
                                    permissionKey
                                ) -> {
                                    // Permission denied
                                    showAlert(
                                        R.string.permissionDenied,
                                        permissionsRequired[position].permissionExplanation,
                                        { _, _ ->
                                            // Ask again for permission
                                            askOnePermission(permissionsRequired[position])
                                        },
                                        { dialogInterface, _ ->
                                            dialogInterface.dismiss()
                                        }
                                    )
                                }
                                else -> {
                                    // Permission denied permanently
                                    showAlert(
                                        R.string.permissionPermDenied,
                                        permissionsRequired[position].permissionPermanentDeniedMessage,
                                        { _, _ ->
                                            // Go to app settings
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            val uri = Uri.fromParts("package", activityContext.packageName, null)
                                            intent.data = uri
                                            activityContext.startActivity(intent)
                                        },
                                        { dialogInterface, _ ->
                                            dialogInterface.dismiss()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
    }

    fun addPermission( permission: String?, permissionExplanation: String, permissionDeniedMessage: String,permissionGrantedMessage: String, permissionPermanentDeniedMessage: String){
        permissionsRequired.add(PermissionData(permission, permissionExplanation, permissionDeniedMessage, permissionGrantedMessage, permissionPermanentDeniedMessage))
    }

    fun hasAllNeededPermissions(): Boolean {
        // Comprova que tingui els permisos necessaris
        return permissionsRequired.all { hasPermission(it.permission ?: "") }
    }

    fun getRejectedPermissions(): ArrayList<PermissionData> {
        // Retorna només els permisos rebutjats
        return ArrayList(permissionsRequired.filter { !hasPermission(it.permission ?: "") })
    }


    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            activityContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun askForPermissions(permissions: ArrayList<PermissionData>) {
        // Demana tots els permisos necessaris
        activityResultLauncher?.launch(permissions.map { it.permission ?: "" }.toTypedArray())
    }

    fun askOnePermission(permission: PermissionData) {
        // Demana el permís necessari
        activityResultLauncher?.launch(arrayOf(permission.permission ?: ""))
    }

    private fun showAlert(
        titleResId: Int,
        message: String?,
        positiveClickListener: DialogInterface.OnClickListener? = null,
        negativeClickListener: DialogInterface.OnClickListener? = null
    ) {
        AlertDialog.Builder(activityContext)
            .setTitle(titleResId)
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton("Ok", positiveClickListener)
            .setNegativeButton("Cancel", negativeClickListener)
            .create()
            .show()
    }
}
