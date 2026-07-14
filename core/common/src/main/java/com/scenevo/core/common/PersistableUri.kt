package com.scenevo.core.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import timber.log.Timber

object PersistableUri {

    fun takeRead(context: Context, uriString: String) {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
        if (uri.scheme != "content") return
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.onFailure { Timber.w(it, "Persistable URI failed for $uriString") }
    }

    fun takeReadAll(context: Context, uriStrings: List<String>) {
        uriStrings.forEach { takeRead(context, it) }
    }
}
