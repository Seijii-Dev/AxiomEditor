/*
 * This file is part of Axiom Editor.
 *
 * Axiom Editor is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Axiom Editor is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Axiom Editor.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package auto.axiom.editor.utils

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Execute a blocking operation on the UI thread.
 * PERFORMANCE NOTE: This should be used sparingly. Prefer suspending functions
 * and proper coroutine structures when possible.
 */
fun <R> runOnUiThread(block: () -> R): R {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        return block()
    }

    var result: R? = null
    var exception: Throwable? = null
    val latch = Object()

    Handler(Looper.getMainLooper()).post {
        try {
            result = block()
        } catch (e: Throwable) {
            exception = e
        } finally {
            synchronized(latch) {
                latch.notifyAll()
            }
        }
    }

    synchronized(latch) {
        while (result == null && exception == null) {
            latch.wait()
        }
    }

    if (exception != null) {
        throw exception!!
    }
    return result!!
}

/**
 * Execute a suspend function synchronously within a coroutine scope.
 * PERFORMANCE NOTE: Prefer structured concurrency with launch/async
 * instead of blocking. Use this only when blocking is absolutely necessary.
 */
fun <R> CoroutineScope.execute(block: suspend () -> R): R = runBlocking {
    block()
}

/**
 * Suspend the coroutine and execute the given block on the main thread.
 * Resumes with the block's result when complete.
 * This is the preferred way to interact with the UI from background coroutines.
 */
suspend fun <R> onUiThread(block: () -> R): R {
    return suspendCancellableCoroutine { continuation ->
        if (Looper.myLooper() == Looper.getMainLooper()) {
            continuation.resume(block())
        } else {
            Handler(Looper.getMainLooper()).post {
                try {
                    continuation.resume(block())
                } catch (e: Throwable) {
                    continuation.resumeWith(Result.failure(e))
                }
            }
        }
    }
}
