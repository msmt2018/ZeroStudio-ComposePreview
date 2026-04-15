package android.zero.studio.compose.preview.executor

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

/**
 * Kotlin-friendly extensions for [TaskExecutor].
 * These top-level functions allow passing direct lambdas instead of [Callable] instances.
 * 
 * @author android_zero
 */

/**
 * Extension to execute a lambda asynchronously.
 * 
 * @param callable The lambda task to execute.
 * @param callback Callback to receive the result.
 */
fun <R> executeAsync(
    callable: () -> R?,
    callback: (R?) -> Unit
): CompletableFuture<R?> {
    // Restoration of TaskExecutorKt$$ExternalSyntheticLambda2 (Callable bridge)
    // and TaskExecutorKt$$ExternalSyntheticLambda3 (Callback bridge)
    return TaskExecutor.executeAsync(
        Callable { callable.invoke() },
        TaskExecutor.Callback { result -> callback.invoke(result) }
    )
}

/**
 * Extension to execute a lambda asynchronously with error handling.
 * 
 * @param callable The lambda task to execute.
 * @param callback Callback to receive the result and optional Throwable.
 */
fun <R> executeAsyncProvideError(
    callable: () -> R?,
    callback: (R?, Throwable?) -> Unit
): CompletableFuture<R?> {
    // Restoration of TaskExecutorKt$$ExternalSyntheticLambda0 (Callable bridge)
    // and TaskExecutorKt$$ExternalSyntheticLambda1 (CallbackWithError bridge)
    return TaskExecutor.executeAsyncProvideError(
        Callable { callable.invoke() },
        TaskExecutor.CallbackWithError { result, error -> callback.invoke(result, error) }
    )
}