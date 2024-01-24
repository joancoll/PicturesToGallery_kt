package cat.dam.andy.picturestogallery_kt

class PermissionData(
    private var permission: String?,
    private var permissionExplanation: String?,
    private var permissionDeniedMessage: String?,
    private var permissionGrantedMessage: String?,
    private var permissionPermanentDeniedMessage: String?
) {
    fun getPermission(): String? {
        return permission
    }

    fun setPermission(permission: String?) {
        this.permission = permission
    }

    fun getPermissionExplanation(): String? {
        return permissionExplanation
    }

    fun setPermissionExplanation(permissionExplanation: String?) {
        this.permissionExplanation = permissionExplanation
    }

    fun getPermissionDeniedMessage(): String? {
        return permissionDeniedMessage
    }

    fun setPermissionDeniedMessage(permissionDeniedMessage: String?) {
        this.permissionDeniedMessage = permissionDeniedMessage
    }

    fun getPermissionGrantedMessage(): String? {
        return permissionGrantedMessage
    }

    fun setPermissionGrantedMessage(permissionGrantedMessage: String?) {
        this.permissionGrantedMessage = permissionGrantedMessage
    }

    fun getPermissionPermanentDeniedMessage(): String? {
        return permissionPermanentDeniedMessage
    }

    fun setPermissionPermanentDeniedMessage(permissionPermanentDeniedMessage: String?) {
        this.permissionPermanentDeniedMessage = permissionPermanentDeniedMessage
    }
}
