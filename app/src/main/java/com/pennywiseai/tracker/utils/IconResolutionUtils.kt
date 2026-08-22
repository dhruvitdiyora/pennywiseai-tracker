package com.pennywiseai.tracker.utils

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Stable icon resolution, adapted from Cashiro's `IconResolutionUtils`.
 *
 * Android resource IDs are not stable across builds -- adding or removing any
 * drawable renumbers others -- so a category row that stored the raw `Int` would
 * silently point at a different picture after the next release. We store the
 * resource *name* string instead and resolve it to an ID at runtime.
 *
 * Unlike Cashiro's original, this has no hardcoded `when (name)` fallback map:
 * PennyWise has no renamed icons and no legacy rows written under old names, so
 * an unresolvable name just returns 0 and the caller falls back to the default icon.
 */
object IconResolutionUtils {

    // getIdentifier() does a string lookup against the resource table on every
    // call. Icon names are a closed set (~496), so memoize name -> resId.
    private val nameToResIdCache = ConcurrentHashMap<String, Int>()

    /**
     * Resolves a resource ID to its name string.
     * Returns empty string if the resource ID is invalid.
     */
    fun resIdToName(context: Context, resId: Int): String {
        if (resId == 0) return ""
        return try {
            val type = context.resources.getResourceTypeName(resId)
            if (type == "drawable" || type == "mipmap") {
                context.resources.getResourceEntryName(resId)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Resolves a stable resource name string back to its current resource ID.
     * Returns 0 if the name cannot be resolved. Memoized -- see [nameToResIdCache].
     */
    fun nameToResId(context: Context, name: String?): Int {
        if (name.isNullOrEmpty()) return 0

        nameToResIdCache[name]?.let { return it }

        val resId = try {
            context.resources.getIdentifier(name, "drawable", context.packageName)
        } catch (e: Exception) {
            0
        }
        nameToResIdCache[name] = resId
        return resId
    }

    /**
     * Checks if a resource ID exists and is a drawable/mipmap.
     * Returns the same resId if valid, otherwise returns fallbackResId.
     */
    fun getSafeResId(context: Context, resId: Int, fallbackResId: Int): Int {
        if (resId == 0) return fallbackResId
        return try {
            val type = context.resources.getResourceTypeName(resId)
            if (type == "drawable" || type == "mipmap") resId else fallbackResId
        } catch (e: Exception) {
            fallbackResId
        }
    }

    /**
     * Convenience wrapper so callers don't have to re-derive validity from
     * [nameToResId] themselves.
     */
    fun isValidIconName(context: Context, name: String?): Boolean =
        nameToResId(context, name) != 0
}
