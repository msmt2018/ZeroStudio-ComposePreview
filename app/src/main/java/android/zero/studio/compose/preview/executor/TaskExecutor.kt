package android.zero.studio.compose.preview.executor

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.function.Supplier

/**
 * 异步任务调度执行器。基于 CompletableFuture 实现生产级的非阻塞任务管理。
 * 
 * 工作流程线路图:
 * 1. 任务提交 (executeAsync): 接收一个 Callable。
 * 2. 线程切换: 将任务提交至 ForkJoinPool 或默认异步线程池执行。
 * 3. 结果合并: 任务完成后，通过 whenComplete 触发调用方的回调。
 * 4. 异常传播: 如果在编译任务中发生异常，将其封装并通过 CallbackWithError 传回。
 * 
 * @author android_zero
 */
object TaskExecutor {

    fun interface Callback<R> {
        fun complete(result: R?)
    }

    fun interface CallbackWithError<R> {
        fun complete(result: R?, error: Throwable?)
    }

    /**
     * 执行异步任务，不强制要求错误处理。
     */
    @JvmStatic
    @JvmOverloads
    fun <R> executeAsync(
        callable: Callable<R>,
        callback: Callback<R>? = null
    ): CompletableFuture<R?> {
        return CompletableFuture.supplyAsync(Supplier {
            try {
                return@Supplier callable.call()
            } catch (t: Throwable) {
                return@Supplier null
            }
        }).whenComplete { result, _ ->
            callback?.complete(result)
        }
    }

    /**
     * 执行异步任务，并将捕获的异常信息传回回调。
     */
    @JvmStatic
    @JvmOverloads
    fun <R> executeAsyncProvideError(
        callable: Callable<R>,
        callback: CallbackWithError<R>? = null
    ): CompletableFuture<R?> {
        return CompletableFuture.supplyAsync(Supplier {
            try {
                return@Supplier callable.call()
            } catch (t: Throwable) {
                throw CompletionException(t)
            }
        }).whenComplete { result, throwable ->
            val actualError = if (throwable is CompletionException) throwable.cause else throwable
            callback?.complete(result, actualError)
        }
    }
}