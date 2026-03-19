import org.junit.Test
import org.junit.Assert.*

class ResultTest {

    @Test
    fun testSuccess() {
        val successResult = Result.Success("Data")
        assertTrue(successResult.isSuccess())
        assertEquals("Data", successResult.getOrNull())
    }

    @Test
    fun testError() {
        val errorResult = Result.Error(Exception("An error occurred"))
        assertTrue(errorResult.isError())
        assertNull(errorResult.getOrNull())
    }

    @Test
    fun testLoading() {
        val loadingResult = Result.Loading
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
        val result = Result.Error(Exception("Error!"))
        assertTrue(result.isError())
    }

    @Test
    fun testIsLoading() {
        val result = Result.Loading
        assertTrue(result.isLoading())
    }
}

fun <T> Result<T>.getOrNull(): T? {
    return when (this) {
        is Result.Success -> this.data
        is Result.Error -> null
        Result.Loading -> null
    }
}

fun <T> Result<T>.isSuccess(): Boolean {
    return this is Result.Success
}

fun <T> Result<T>.isError(): Boolean {
    return this is Result.Error
}

fun <T> Result<T>.isLoading(): Boolean {
    return this == Result.Loading
}