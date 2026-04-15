package android.zero.studio.compose.preview.executor

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.function.BiConsumer
import java.util.function.Supplier

/**
 * A utility object that manages asynchronous task execution using [CompletableFuture].
 * It provides methods to execute [Callable] tasks with or without detailed error reporting.
 * 
 * 工作流程 (executeAsync):
 * 1. 接收一个 Callable 和可选的 Callback。
 * 2. 使用 supplyAsync 在异步线程执行任务，捕获所有异常并返回 null。
 * 3. 任务完成后，通过 whenComplete 触发 Callback 的 complete 方法。
 *
 * @author android_zero
 */
object TaskExecutor {

    /**
     * Functional interface for receiving a successful task result.
     */
    fun interface Callback<R> {
        fun complete(result: R?)
    }

    /**
     * Functional interface for receiving a task result along with any caught exception.
     */
    fun interface CallbackWithError<R> {
        fun complete(result: R?, error: Throwable?)
    }

    /**
     * Executes a task asynchronously. If an error occurs, the result is null.
     */
    @JvmStatic
    @JvmOverloads
    fun <R> executeAsync(
        callable: Callable<R>,
        callback: Callback<R>? = null
    ): CompletableFuture<R?> {
        // Restoration of TaskExecutor$$ExternalSyntheticLambda3 (Supplier)
        // and TaskExecutor$$ExternalSyntheticLambda4/5 (BiConsumer)
        return CompletableFuture.supplyAsync(Supplier {
            try {
                return@Supplier callable.call()
            } catch (t: Throwable) {
                // Return null on failure as per original logic
                return@Supplier null
            }
        }).whenComplete { result, _ ->
            callback?.complete(result)
        }
    }

    /**
     * Executes a task asynchronously and provides the error to the callback if execution fails.
     */
    @JvmStatic
    @JvmOverloads
    fun <R> executeAsyncProvideError(
        callable: Callable<R>,
        callback: CallbackWithError<R>? = null
    ): CompletableFuture<R?> {
        // Restoration of TaskExecutor$$ExternalSyntheticLambda0 (Supplier)
        // and TaskExecutor$$ExternalSyntheticLambda1/2 (BiConsumer)
        return CompletableFuture.supplyAsync(Supplier {
            try {
                return@Supplier callable.call()
            } catch (t: Throwable) {
                // Wrap and throw to trigger whenComplete's error handle
                throw CompletionException(t)
            }
        }).whenComplete { result, throwable ->
            // Pass both result and the root cause of the error
            callback?.complete(result, throwable)
        }
    }
}