Testing Guide for Monkey Music Player

This document provides quick-start instructions to run unit tests and instrumented tests for the project, plus guidance on how to add tests and verify performance improvements.

1) Prerequisitos
- JDK and Android SDKs installed
- Git repo checked out
- Gradle wrapper (gradlew) present in repo root

2) Eject unit tests (local JVM tests)
- Comando típico: `gradlew test` (Windows: `gradlew.bat test`)
- Para ejecutar solo una clase de prueba: `gradlew testDebugUnitTest --tests com.example.MyTest` (ajusta el paquete y clase a lo que necesites)
- Ubicación típica de tests unitarios: app/src/test/java/

3) Eject instrumented tests (Android unit tests)
- Comando: `gradlew connectedDebugAndroidTest` para conectar a un dispositivo/emulador
- Para correr pruebas instrumentadas en un módulo específico: `gradlew :app:connectedDebugAndroidTest`
- Ubicación típica de tests instrumentados: app/src/androidTest/java/

4) Estructura de tests existente (referencia rápida)
- Pruebas de unidad en: app/src/test/java/com/lg/monkeymusicplayer/
- Pruebas de integración/UI en: app/src/androidTest/java/com/lg/monkeymusicplayer/
- Ejemplos ya presentes: MusicPlayerManagerTest.kt, MusicRepositoryTest.kt, MusicViewModelTest.kt, etc.

5) Consejos para nuevos tests
- Mantén los tests lo más independientes posible de Android framework cuando puedas; usa Mockito para mocks de dependencias.
- Usa @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule() para pruebas de LiveData/StateFlow donde corresponda.
- Para pruebas de corrutinas, emplea StandardTestDispatcher o Unconfined/TestCoroutineDispatcher y reset al terminar.
- Añade pruebas para casos límite (errores, estados de carga, límites de tamaño de datos) y para el flujo de eventos de UI/estado.

6) Añadir pruebas y observar rendimiento
- Las pruebas unitarias deben cubrir la lógica de negocio sin depender fuertemente de IO.
- Para evaluar rendimiento, puedes añadir pruebas de tiempo de ejecución con asserts simples o usar herramientas de profiling (no cubierto por este guide).
- Si agregas código de alto costo, considera memoización con remember/derivedStateOf en Compose, o mover lógica costosa fuera del composable hacia el ViewModel o repositorios.

7) Notas sobre CI
- Asegúrate de que el proyecto compile en CI y que las pruebas se ejecuten en el pipeline.
- Considera ejecutar pruebas en modo paralelo si el CI las soporta para acelerar el pipeline.

8) Cambios futuros
- Si amplías el dominio de pruebas, actualiza este documento con ejemplos de ejecución y ejemplos de fixtures.
