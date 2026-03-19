package com.lg.monkeymusicplayer

import com.lg.monkeymusicplayer.core.result.Result
import org.junit.Assert.*
import org.junit.Test

class ResultTest {

    @Test
    fun testSuccess() {
        val successResult = Result.Success("Data")
        assertTrue(successResult.isSuccess())
        assertEquals("Data", successResult.getOrNull())
    }

    @Test
    fun testError() {
        val errorResult = Result.Error<String>("An error occurred")
        assertTrue(errorResult.isError())
        assertNull(errorResult.getOrNull())
    }

    @Test
    fun testLoading() {
        val loadingResult = Result.Loading<String>()
        assertTrue(loadingResult.isLoading())
        assertNull(loadingResult.getOrNull())
    }

    // Extension functions tests
    @Test
    fun testIsSuccess() {
        val result = Result.Success("Data")
        assertTrue(result.isSuccess())
    }

    @Test
    fun testIsError() {
        val result = Result.Error<String>("Error!")
        assertTrue(result.isError())
    }

    @Test
    fun testIsLoading() {
        val result = Result.Loading<String>()
        assertTrue(result.isLoading())
    }
}

fun <T> Result<T>.getOrNull(): T? {
    return when (this) {
        is Result.Success -> this.data
        is Result.Error -> null
        is Result.Loading -> null
    }
}

fun <T> Result<T>.isSuccess(): Boolean {
    return this is Result.Success
}

fun <T> Result<T>.isError(): Boolean {
    return this is Result.Error
}

fun <T> Result<T>.isLoading(): Boolean {
    return this is Result.Loading
}
