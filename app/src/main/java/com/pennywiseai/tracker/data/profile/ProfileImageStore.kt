package com.pennywiseai.tracker.data.profile

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Keeps the user's profile photo and Home banner as private copies inside the app's
 * `filesDir`, so they survive the photo picker's temporary URI grants and stay out of
 * shared storage. Every import gets a unique file name, which also keeps Coil's
 * memory cache from serving a stale image after a replace.
 */
@Singleton
class ProfileImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val mediaDir: File
        get() = File(context.filesDir, MEDIA_DIR).also { it.mkdirs() }

    /** Copies [source] into private storage and returns the new file's URI, or null on failure. */
    suspend fun import(source: Uri, prefix: String): String? = withContext(Dispatchers.IO) {
        val target = File(mediaDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        try {
            val input = context.contentResolver.openInputStream(source) ?: return@withContext null
            input.use { src -> target.outputStream().use { dst -> src.copyTo(dst) } }
            target.toUri().toString()
        } catch (_: Exception) {
            target.delete()
            null
        }
    }

    /**
     * Deletes the file behind [uriString] when it is one of ours (a file in the profile
     * media folder, or the legacy onboarding photo). Preset avatars (`avatar://N`),
     * foreign URIs and anything else are left untouched.
     */
    fun deleteIfOwned(uriString: String?) {
        val uri = uriString?.let(Uri::parse) ?: return
        if (uri.scheme != "file") return
        val path = uri.path ?: return
        try {
            val file = File(path).canonicalFile
            val parent = file.parentFile ?: return
            val owned = parent == mediaDir.canonicalFile ||
                (parent == context.filesDir.canonicalFile && file.name == LEGACY_ONBOARDING_PHOTO)
            if (owned && file.isFile) file.delete()
        } catch (_: Exception) {
            // Best effort: a leftover image file is harmless.
        }
    }

    companion object {
        private const val MEDIA_DIR = "profile_media"
        private const val LEGACY_ONBOARDING_PHOTO = "profile_image.jpg"
    }
}
