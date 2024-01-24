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

class PermissionManager(context: Context, permissionsRequired: ArrayList<PermissionData>) {
    var activityResultLauncher: ActivityResultLauncher<Array<String>>? = null

    init {
        // Inicialitza el launcher per demanar permisos
        initPermissionLauncher(context, permissionsRequired)
    }

    private fun initPermissionLauncher(
        context: Context,
        permissionsRequired: ArrayList<PermissionData>
    ) {
        // Inicialitza el launcher per demanar permisos
        activityResultLauncher =
            (context as AppCompatActivity).registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
                ActivityResultCallback<Map<String, Boolean>> { permissions ->
                    // Check if all permissions are granted
                    if (permissions.containsValue(false)) {
                        // Check every permission
                        for (permission in permissions.keys) {
                            val position = permissionsRequired.indexOfFirst { it.getPermission() == permission }

                            when {
                                permissions[permission] == true -> {
                                    // Permission granted
                                    showAlert(
                                        context,
                                        R.string.permissionGranted,
                                        permissionsRequired[position].getPermissionGrantedMessage()
                                    )
                                }
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                    context,
                                    permission
                                ) -> {
                                    // Permission denied
                                    showAlert(
                                        context,
                                        R.string.permissionDenied,
                                        permissionsRequired[position].getPermissionExplanation()
                                    )
                                }
                                else -> {
                                    // Permission denied permanently
                                    showAlert(
                                        context,
                                        R.string.permissionPermDenied,
                                        permissionsRequired[position].getPermissionPermanentDeniedMessage(),
                                        { _, _ ->
                                            // Go to app settings
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            val uri = Uri.fromParts("package", context.packageName, null)
                                            intent.data = uri
                                            context.startActivity(intent)
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

    fun hasAllNeededPermissions(context: Context, permissions: ArrayList<PermissionData>): Boolean {
        // Comprova que tingui els permisos necessaris
        return permissions.all { hasPermission(context, it.getPermission() ?: "") }
    }

    fun getRejectedPermissions(
        context: Context,
        permissions: ArrayList<PermissionData>
    ): ArrayList<PermissionData> {
        // Retorna només els permisos rebutjats
        return ArrayList(permissions.filter { !hasPermission(context, it.getPermission() ?: "") })
    }


    private fun hasPermission(context: Context, permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun askForPermissions(context: Context?, permissions: ArrayList<PermissionData>) {
        // Demana tots els permisos necessaris
        activityResultLauncher?.launch(permissions.map { it.getPermission() ?: "" }.toTypedArray())
    }

    fun askOnePermission(context: Context?, permission: PermissionData) {
        // Demana el permís necessari
        activityResultLauncher?.launch(arrayOf(permission.getPermission() ?: ""))
    }

    private fun showAlert(
        context: Context,
        titleResId: Int,
        message: String?,
        positiveClickListener: DialogInterface.OnClickListener? = null,
        negativeClickListener: DialogInterface.OnClickListener? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle(titleResId)
            .setMessage(message)
            .setCancelable(true)
            .setPositiveButton("Ok", positiveClickListener)
            .setNegativeButton("Cancel", negativeClickListener)
            .create()
            .show()
    }
}
