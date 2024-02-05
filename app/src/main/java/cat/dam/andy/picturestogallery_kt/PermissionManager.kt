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
        var permissionInfo: String?,
        var permissionNeededMessage: String?,
        var permissionDeniedMessage: String?,
        var permissionGrantedMessage: String?,
        var permissionPermanentDeniedMessage: String?
    )

    private val permissionsRequired = mutableListOf<PermissionData>()
    private var permissionRequired: String=""
    private var singlePermissionResultLauncher: ActivityResultLauncher<String>? = null
    private var multiplePermissionResultLauncher: ActivityResultLauncher<Array<String>>? = null

    init {
        // Inicialitza el launcher per demanar un sol permís
        initSinglePermissionLauncher()
        // Inicialitza el launcher per demanar múltiples permisos
        initMultiplePermissionLauncher()
    }

    private fun initSinglePermissionLauncher() {
        // Inicialitza el launcher per demanar un sol permís
        singlePermissionResultLauncher =
            (activityContext as AppCompatActivity).registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    // Permission granted
                    permissionsRequired
                        .firstOrNull { it.permission == permissionRequired }
                        ?.let { matchedPermission ->
                            showAlert(
                                R.string.permissionGranted,
                                matchedPermission.permissionGrantedMessage
                            )
                        }
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activityContext,
                        permissionRequired
                    )) {
                    // Permission denied, but not permanently
                    permissionsRequired
                        .firstOrNull { it.permission == permissionRequired }
                        ?.let { matchedPermission ->
                            showAlert(
                                R.string.permissionDenied,
                                matchedPermission.permissionNeededMessage,
                                { _, _ ->
                                    // Ask again for permission
                                    askForPermission(matchedPermission)
                                },
                                { dialogInterface, _ ->
                                    dialogInterface.dismiss()
                                }
                            )
                        }
                    // detecta si l'usuari ha cancel·lat la petició de permís o si l'ha denegat permanentment
                } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        activityContext,
                        permissionRequired )) {
                    // Permission denied permanently
                    permissionsRequired
                        .firstOrNull { it.permission == permissionRequired }
                        ?.let { matchedPermission ->
                            showAlert(
                                R.string.permissionPermDenied,
                                matchedPermission.permissionPermanentDeniedMessage,
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


    private fun initMultiplePermissionLauncher() {
        // Inicialitza el launcher per demanar permisos
        multiplePermissionResultLauncher =
            (activityContext as AppCompatActivity).registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
                ActivityResultCallback<Map<String, Boolean>> { permissions ->
                    // Check if all permissions are granted
                    if (permissions.containsValue(false)) {
                        // Check every permission
                        for (permissionKey in permissions.keys) {
                            val position =
                                permissionsRequired.indexOfFirst { it.permission == permissionKey }

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
                                        permissionsRequired[position].permissionNeededMessage,
                                        { _, _ ->
                                            // Ask again for permission
                                            askForPermission(permissionsRequired[position])
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
                                            val intent =
                                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                            val uri = Uri.fromParts(
                                                "package",
                                                activityContext.packageName,
                                                null
                                            )
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

    fun addPermission(
        permission: String?,
        permissionInfo: String?,
        permissionNeededMessage: String,
        permissionDeniedMessage: String,
        permissionGrantedMessage: String,
        permissionPermanentDeniedMessage: String
    ) {
        permissionsRequired.add(
            PermissionData(
                permission,
                permissionInfo,
                permissionNeededMessage,
                permissionDeniedMessage,
                permissionGrantedMessage,
                permissionPermanentDeniedMessage
            )
        )
    }

    fun getAllNeededPermissions(): MutableList<PermissionData> {
        // Comprova que tingui els permisos necessaris
        return permissionsRequired
    }

    fun hasAllNeededPermissions(): Boolean {
        // Comprova que tingui els permisos necessaris
        return permissionsRequired.all { hasPermission(it.permission ?: "") }
    }

    fun hasThisNeededPermission(permission: String): Boolean {
        // Comprova que tingui el permís necessari
        val matchingPermission = permissionsRequired.firstOrNull { it.permission == permission }
        return matchingPermission?.let { matchingPermission.permission?.let { it ->
            hasPermission(it)
        } } ?: false
    }

    fun getRejectedPermissions(): ArrayList<PermissionData> {
        // Retorna només els permisos rebutjats
        return ArrayList(permissionsRequired.filter { !hasPermission(it.permission ?: "") })
    }


    fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            activityContext,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun askForThisPermission(permission: String) {
        // Demana el permis necessari amb el launcher
        permissionRequired = permission
        singlePermissionResultLauncher?.launch(permission)
    }

    fun askForAllNeededPermissions() {
        // Demana tots els permisos necessaris
        multiplePermissionResultLauncher?.launch(permissionsRequired.map { it.permission ?: "" }
            .toTypedArray())
    }

    private fun askForPermission(permission: PermissionData) {
        // Demana el permís necessari
        multiplePermissionResultLauncher?.launch(arrayOf(permission.permission ?: ""))
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
