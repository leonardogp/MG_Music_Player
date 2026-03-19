package com.lg.monkeymusicplayer

import android.content.Context
import com.lg.monkeymusicplayer.core.exception.GlobalExceptionHandler
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ExceptionHandlerTest {

    @Test
    fun testHandlerInstantiation() {
        val mockContext = mock(Context::class.java)
        val handler = GlobalExceptionHandler(mockContext)
        assertNotNull(handler)
    }

    @Test
    fun testSetup() {
        val mockContext = mock(Context::class.java)
        GlobalExceptionHandler.setup(mockContext)
        // Verify that the default uncaught exception handler is set
        assertNotNull(Thread.getDefaultUncaughtExceptionHandler())
    }
}
