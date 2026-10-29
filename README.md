# 🌙 Selenia: Luna y Calc (4 en 1)

**Calendario Astronómico, Observatorio de Fases de la Luna, Reloj Astronómico/Mundial y Calculadora Científica + de Fechas en una sola APK para Android.**

---

## ✨ Características Principales (4 en 1)

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

---

## 🚀 Sincronización con GitHub, Compilación con Actions y Publicación de Releases

Este proyecto incluye el flujo de trabajo automatizado en **`.github/workflows/android-release.yml`**.

### ¿Cómo funciona automáticamente al subir a GitHub?

1. **Sincroniza o haz push del repositorio a GitHub** en la rama `main` (o `master`), o crea un tag como `v1.0.0`.
2. **GitHub Actions** se ejecutará automáticamente y realizará los siguientes pasos:
   - Configura **JDK 21**, **Android SDK (API 36.1)** y **Gradle 9.3.1**.
   - Ejecuta las pruebas unitarias (`./gradlew :app:testDebugUnitTest`).
   - Genera las llaves de firma automáticamente (o utiliza tus propios secretos si los defines) y compila:
     - `Selenia-Luna-y-Calc-release.apk` (APK firmada de producción)
     - `Selenia-Luna-y-Calc-debug.apk` (APK de depuración)
   - Sube las APKs como **Artifacts** de la ejecución.
   - **Publica automáticamente un GitHub Release** con una etiqueta de versión (`v1.0.X` o el tag que hayas enviado) adjuntando las APKs listas para descargar e instalar en Android.

### Ejecución manual desde la pestaña Actions
También puedes publicar un nuevo Release manualmente en cualquier momento:
1. Ve a la pestaña **Actions** en tu repositorio de GitHub.
2. Selecciona el flujo **Build APK & Publish GitHub Release**.
3. Haz clic en **Run workflow**, escribe la versión deseada (por ejemplo, `v1.1.0`) y pulsa **Run workflow**.

### 🔐 Firma personalizada opcional (GitHub Secrets)
El flujo funciona **sin configurar ningún secreto** gracias a su generación automática de keystore. Si deseas usar tu propio certificado `.jks` permanente para actualizaciones futuras:
1. Convierte tu archivo `.jks` a Base64:
   ```bash
   base64 -w 0 my-upload-key.jks
   ```
2. En tu repositorio de GitHub, ve a **Settings → Secrets and variables → Actions** y agrega:
   - `KEYSTORE_BASE64`: El contenido en Base64 de tu keystore (con el alias `upload`).
   - `STORE_PASSWORD`: La contraseña del almacén de claves.
   - `KEY_PASSWORD`: La contraseña de la clave `upload`.

---

## 🛠️ Requisitos y Compilación Local

- **Android Studio**: Ladybug / Meerkat o superior
- **Compile SDK / Target SDK**: 36
- **Min SDK**: 24 (Android 7.0+)
- **Lenguaje y UI**: 100% Kotlin + Jetpack Compose (Material Design 3)

### Comandos útiles con Gradle
```bash
# Ejecutar pruebas unitarias
./gradlew :app:testDebugUnitTest

# Compilar APK de depuración
./gradlew :app:assembleDebug

# Compilar APK de producción (Release)
./gradlew :app:assembleRelease
```
