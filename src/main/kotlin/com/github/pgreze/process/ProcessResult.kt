package com.github.pgreze.process

data class ProcessResult(
    val resultCode: Int,
    val output: List<String>,
)

/**
 * Ensure a [process] call always conclude correctly.
 * @return [ProcessResult.output] because we're sure the result is valid.
 */
fun ProcessResult.unwrap(): List<String> {
    check(resultCode == 0) { "Invalid result: $resultCode" }
    return output
}
