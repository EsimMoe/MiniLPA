package com.github.pgreze.process

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset

sealed class InputSource {
    /**
     * Natively supported file redirection.
     * @see ProcessBuilder.Redirect.from
     */
    class FromFile(val file: File) : InputSource()

    /**
     * Allows custom logic given the [Process.getInputStream] instance,
     * auto-closed after [handler] completes.
     */
    class FromStream(val handler: suspend (OutputStream) -> Unit) : InputSource()

    @Suppress("BlockingMethodInNonBlockingContext")
    companion object {
        @JvmStatic
        fun fromString(
            string: String,
            charset: Charset = Charset.forName("UTF-8"),
        ): InputSource = FromStream {
            it.write(string.toByteArray(charset))
        }

        @JvmStatic
        fun fromInputStream(
            inputStream: InputStream,
            bufferSize: Int = DEFAULT_BUFFER_SIZE,
        ): InputSource = FromStream { out ->
            inputStream.use { it.copyTo(out, bufferSize) }
        }
    }
}

internal fun InputSource.toNative() = when (this) {
    is InputSource.FromFile -> ProcessBuilder.Redirect.from(file)
    is InputSource.FromStream -> ProcessBuilder.Redirect.PIPE
}
