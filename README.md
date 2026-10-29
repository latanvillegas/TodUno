# 🌙 Selenia: Luna y Calc (5 en 1)

**Calendario Astronómico, Observatorio de Fases de la Luna, Reloj Astronómico/Mundial, Calculadora Científica + de Fechas y Estudio de Colores (RGB, HEX, CMYK, HSL, Combinaciones y Tonos) en una sola APK para Android.**

---

## ✨ Características Principales (5 en 1)

### 1. 📅 Calendario Lunar Interactivo
- **Cuadrícula Mensual con Miniaturas Lunares**: Visualiza la fase exacta de la luna en cada día del mes (ciclo sinódico de `29.53059 días`).
- **Hitos Lunares del Mes**: Acceso rápido a los días exactos de *Luna Nueva*, *Cuarto Creciente*, *Luna Llena* y *Cuarto Menguante*.
- **Inspector de Efemérides**: Consulta el porcentaje de iluminación, edad lunar en días, distancia Tierra-Luna en kilómetros, signo eclíptico y recomendaciones de cultivo/observación para cualquier fecha.
- **Agenda Personal por Día**: Añade y gestiona notas o recordatorios categorizados (*Personal*, *Astronomía*, *Siembra*, *Cálculo*) en cualquier fecha del calendario.

### 2. 🌕 Observatorio de Fases de la Luna
- **Renderizado Esférico en Tiempo Real (`Canvas`)**: Representación de alta precisión con cráteres, mares lunares (*Mare Tranquillitatis*, *Oceanus Procellarum*, *Tycho*), luz cenicienta y terminador esférico exacto.
- **Selector de Hemisferio (Norte / Sur)**: Ajusta automáticamente la orientación de la zona iluminada según el hemisferio del observador.
- **Navegación Temporal Rápida**: Desplázate `-7 días`, `-1 día`, `Hoy`, `+1 día` o `+7 días` para explorar la evolución del ciclo.
- **Guía de Mareas y Biorritmo Natural**: Información detallada sobre mareas vivas (sicigia), mareas muertas (cuadratura), observación astronómica y tareas agrícolas.
- **Próximas 4 Fases Principales**: Cronograma interactivo con cuenta regresiva en días hacia las siguientes fases clave.

### 3. ⌚ Reloj Astronómico, Reloj Mundial, Cronómetro y Temporizador
- **Reloj Analógico con Complicación de Fase Lunar + Digital**: Esfera de reloj astronómico con segundero fluido, subesfera lunar integrada, cambio de formato `24h / 12h AM/PM`, **Tiempo Universal (UTC)** y **Tiempo Sideral Medio de Greenwich (GMST)**.
- **Reloj Mundial**: Consulta la hora en tiempo real e indicador día/noche (`☀️`/`🌙`) en las principales capitales y zonas horarias (*UTC, Madrid, Ciudad de México, Bogotá/Lima, Buenos Aires, Nueva York, Tokio*).
- **Cronómetro de Precisión**: Medición centesimal con registro ilimitado de vueltas (*Laps*) y tiempos parciales.
- **Temporizador con Anillo Circular**: Cuenta regresiva visual con ajustes rápidos (`-1m`, `+30s`, `+1m`) y preajustes directos (incluyendo ciclo de `29.5 min`).

### 4. 🧮 Calculadora Dual (Científica + Fechas y Ciclos Lunares)
- **Modo Científica + Lunar**:
  - Operaciones aritméticas (`+`, `−`, `×`, `÷`, `%`, `^`), paréntesis y funciones científicas (`sin`, `cos`, `tan`, `√`, `ln`, `π`).
  - **Botones Astronómicos Directos**: Multiplica por el ciclo sinódico (`× 29.53d`), convierte días naturales a meses lunares (`Días → Lunas`) o inserta la constante sinódica (`29.5306`).
  - **Historial Interactivo**: Cinta de cálculos recientes reutilizables con un toque.
- **Modo Fechas y Ciclos Lunares**:
  - Suma o resta días (`±180 días`) o ciclos sinódicos (`±12 lunas`) a la fecha seleccionada y descubre qué fecha será y qué fase lunar tendrá.
  - Calcula el intervalo exacto en **días naturales** y **meses sinódicos lunares** entre dos fechas cualesquiera.

### 5. 🎨 Estudio y Conversor de Colores (RGB • HEX • CMYK • HSL)
- **Conversión Bidireccional en Tiempo Real**:
  - **HEX**: Entrada directa `#RRGGBB`, barra de espectro cromático de `360°` y paleta de favoritos guardados.
  - **RGB**: Controles deslizantes independientes para Rojo (`0–255`), Verde (`0–255`) y Azul (`0–255`).
  - **CMYK**: Controles para Cian (`0–100%`), Magenta (`0–100%`), Amarillo (`0–100%`) y Negro/Key (`0–100%`).
  - **HSL**: Controles para Matiz (`0°–360°`), Saturación (`0–100%`) y Luminosidad (`0–100%`).
  - **Copiado Instantáneo y Contraste WCAG**: Toca cualquiera de las tarjetas (`HEX`, `RGB`, `CMYK`, `HSL`) para copiar su código al portapapeles y verifica el ratio de contraste sobre texto blanco y negro.
- **Combinaciones Armónicas**:
  - Generación automática de paletas **Complementaria**, **Análoga**, **Triádica**, **Complementaria Dividida** y **Tetrádica (Cuadrada)**.
- **Tonos, Matices y Sombras**:
  - Escala completa de **Luminosidad (`L%` de 92% pastel a 12% sombra profunda)** y escala de **Saturación (`S%` de 100% puro a 10% neutro)** con valores `HEX`, `RGB` y `CMYK` en cada muestra.

---

## 🚀 Sincronización con GitHub, Compilación con Actions y Publicación de Releases

Este proyecto incluye el flujo de trabajo automatizado en **`.github/workflows/android-release.yml`**.

### ¿Cómo funciona automáticamente al subir a GitHub?

1. **Sincroniza o haz push del repositorio a GitHub** en la rama `main` (o `master`), o crea un tag como `v1.0.0`.
2. **GitHub Actions** se ejecutará automáticamente y realizará los siguientes pasos:
   - Configura **JDK 21**, **Android SDK (API 36)** y **Gradle 9.3.1**.
   - Ejecuta las pruebas unitarias (`./gradlew :app:testDebugUnitTest`).
   - Genera las llaves de firma automáticamente (o utiliza tus propios secretos si los defines) y compila:
     - `Selenia-Luna-y-Calc-release.apk` (APK firmada de producción)
     - `Selenia-Luna-y-Calc-debug.apk` (APK de depuración)
   - Sube las APKs como **Artifacts** de la ejecución.
   - **Publica automáticamente un GitHub Release** con una etiqueta de versión (`v1.0.X` o el tag que hayas enviado) adjuntando las APKs listas para descargar e instalar en Android.
