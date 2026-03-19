import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GlobalExceptionHandlerTest {

    @Test
    public void testUncaughtExceptionLogged() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Exception exception = new RuntimeException("Test Exception");

        handler.uncaughtException(Thread.currentThread(), exception);

        // Verify that the exception is properly logged
        // Assume we have a method isLogged that checks if the exception was logged
        assertTrue(handler.isLogged(exception));
    }

    @Test
    public void testCustomExceptionThrown() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Exception exception = assertThrows(CustomException.class, () -> {
            handler.throwCustomException();
        });

        assertEquals("This is a custom exception", exception.getMessage());
    }
}