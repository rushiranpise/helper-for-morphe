package dev.rushi.apkdownloadhelper.sources

import kotlinx.coroutines.CancellationException

/**
 * Runs one source lookup. Cancellation must keep unwinding the calling coroutine:
 * a cancelled lookup is not a failed lookup and must never become a cacheable miss.
 */
internal suspend inline fun <T> runSourceLookup(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        Result.failure(error)
    }
