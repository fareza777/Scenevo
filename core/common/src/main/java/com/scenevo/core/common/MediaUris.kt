package com.scenevo.core.common

import android.net.Uri
import java.io.File

object MediaUris {

    /** Normalize absolute paths, file://, and content:// into a Media3-safe Uri. */
    fun parse(uriOrPath: String): Uri = when {
        uriOrPath.startsWith("content://", ignoreCase = true) ||
            uriOrPath.startsWith("file://", ignoreCase = true) ||
            uriOrPath.startsWith("android.resource://", ignoreCase = true) -> Uri.parse(uriOrPath)
        uriOrPath.startsWith("/") -> Uri.fromFile(File(uriOrPath))
        else -> Uri.parse(uriOrPath)
    }

    fun fileUri(file: File): String = file.toURI().toString()
}
