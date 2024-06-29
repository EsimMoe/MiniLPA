package com.github.pgreze.process

import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

private suspend fun <R> coroutineScopeIO(block: suspend CoroutineScope.() -> R) =
    withContext(Dispatchers.IO) {
        // Encapsulates all async calls in the current scope.
        // https://elizarov.medium.com/structured-concurrency-722d765aa952
        coroutineScope(block)
    }

@Suppress("BlockingMethodInNonBlockingContext", "LongParameterList", "ComplexMethod")
@JvmOverloads
suspend fun process(
    vararg command: String,
    stdin: InputSource? = null,
    stdout: Redirect = Redirect.PRINT,
    stderr: Redirect = Redirect.PRINT,
    charset: Charset = Charsets.UTF_8,
    /** Extend with new environment variables during this process's invocation. */
    env: Map<String, String>? = null,
    /** Override the process working directory. */
    directory: File? = null,
    /** Determine if process should be destroyed forcibly on job cancellation. */
    destroyForcibly: Boolean = false,
    /** Consume without delay all streams configured with [Redirect.Capture]. */
): ProcessResult = coroutineScopeIO {
    // https://www.baeldung.com/java-lang-processbuilder-api
    val process = ProcessBuilder(*command).apply {
        stdin?.toNative()?.let { redirectInput(it) }

        redirectOutput(stdout.toNative())
        redirectError(stderr.toNative())

        directory?.let { directory(it) }
        env?.let { environment().putAll(it) }
    }.start()

    // Handles async consumptions before the blocking output handling.
    if (stdout is Redirect.Consume) {
        process.inputStream.lineFlow(charset, stdout.consumer)
    }
    if (stderr is Redirect.Consume) {
        process.errorStream.lineFlow(charset, stderr.consumer)
    }

    val output = async {
        val lines = mutableListOf<String>()
        val captrue = { redirect: Redirect, stream : InputStream ->
            async {
                if (redirect is Redirect.Capture)
                {
                    stream.lineFlow(charset) { f ->
                        f.map {
                            yield()
                            redirect.consumer(it)
                            lines.add(it)
                        }.toList()
                    }
                }
            }
        }

        awaitAll(captrue(stdout, process.inputStream), captrue(stderr, process.errorStream))
        lines
    }

    val input = async {
        (stdin as? InputSource.FromStream)?.handler?.let { handler ->
            process.outputStream.use { handler(it) }
        }
    }

    try {
        @Suppress("UNCHECKED_CAST")
        ProcessResult(
            // Consume the output before waitFor,
            // ensuring no content is skipped.
            output = awaitAll(input, output).last() as List<String>,
            resultCode = runInterruptible { process.waitFor() },
        )
    } catch (e: CancellationException) {
        when (destroyForcibly) {
            true -> process.destroyForcibly()
            false -> process.destroy()
        }
        throw e
    }
}

private suspend fun <T> InputStream.lineFlow(charset: Charset, block: suspend (Flow<String>) -> T): T =
    bufferedReader(charset).use { it.lineSequence().asFlow().let { f -> block(f) } }
