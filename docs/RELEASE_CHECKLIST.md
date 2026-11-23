# Procedimiento de lanzamiento de TodUno

## 1. Recuperar y asegurar la clave de firma

La versión pública `v1.0.13` (18-11-2026) está firmada con el certificado SHA-256:

```text
477d7bf8d50a6f5177a387bdd30ddcb44e84080bd773db80c12a189a7b6081b9
```

El APK producido por el último workflow de la antigua rama `main` tenía otro certificado (`f927036d1cfad89e7dd56e09d93ee4c5bfe36acebdd2c9bf922569db75981c97`), por lo que **no constituye una actualización instalable sobre v1.0.13**.

1. Localizar la clave privada original que firmó `v1.0.13` antes de publicar otra versión.
2. Verificar que el archivo JKS recuperado contiene el alias `upload` y que su certificado coincide exactamente con el SHA-256 anterior. El certificado es público; **no subir el JKS ni las contraseñas al repositorio**.
3. Configurar los secretos de Actions: `KEYSTORE_BASE64`, `STORE_PASSWORD` y `KEY_PASSWORD`.
4. Guardar copia cifrada y controlada de la clave y contraseñas en un gestor seguro, separada de GitHub. El CI fallará intencionadamente si falta alguno de los secretos.
5. Si esa clave ya no existe, **no usar una clave nueva como si fuera una actualización**. Definir y comunicar un proceso de migración de firma o reinstalación con exportación de datos.

## 2. Actualizar la versión

La configuración del código propone `versionCode=14` y `versionName=1.0.14`, para la etiqueta `v1.0.14`.

- Las siguientes versiones requieren `versionCode` estrictamente creciente.
- Cada etiqueta `vX.Y.Z` debe coincidir con `versionName=X.Y.Z` dentro del APK.
- No cambiar `applicationId=com.aistudio.selenialunar.kxmpzq` si se desea mantener la vía de actualización.

## 3. Validaciones automáticas

En cada PR y push a `main`, el workflow ejecuta `testDebugUnitTest` y `lintDebug`, sin acceso a secretos de firma.

Solo una etiqueta `v*` que pertenezca a `main` ejecuta la creación de APK y AAB de publicación. Se verifican:

- Existencia del JKS original y de las contraseñas.
- Compilación `assembleRelease` y `bundleRelease`.
- Identificador, `versionName`, `versionCode` y firma del APK mediante `aapt2` y `apksigner`.
- Igualdad con el certificado público de `v1.0.13`.
- Alineación ZIP de 16 KB (`zipalign -c -P 16`).
- Firma de AAB mediante `jarsigner` y hashes SHA-256.
- Creación de GitHub Release con `TodUno-release.apk`, `TodUno-release.aab` y `SHA256SUMS`. No se sobrescriben releases existentes.

Ejecutar primero el PR de infraestructura y comprobar los resultados. **No crear la etiqueta** hasta recuperar la clave y completar las pruebas de dispositivo.

## 4. Validación física

- Instalar desde cero y actualizar desde `v1.0.13` con la **misma firma** sin perder datos.
- Probar calendario, notas, copias JSON, observatorio, reloj, cronómetro y calculadora.
- Probar notificaciones autorizadas, reinicio, cambio de zona horaria y widget.
- Revisar Android 7, Android 13+ y Android 16, incluido un dispositivo con páginas de memoria de 16 KB cuando esté disponible.
- Validar los permisos y la política de privacidad contra el binario final.

## 5. Publicación y tiendas

- Verificar el Release en GitHub antes de solicitar su inclusión en Orion Store u otros catálogos.
- Usar enlace directo al APK publicado y enlaces directos a iconos y capturas PNG.
- Obtener autorización sobre la licencia de código y recursos y publicar `LICENSE` si procede.
- Completar email de soporte, identidad del editor, política pública, clasificación por edades y metadatos exigidos por cada tienda.
- Para Google Play usar el `.aab`, configurar Play App Signing y revisar las declaraciones de datos.
- No asumir que el catálogo aprobó la aplicación hasta recibir la confirmación del mantenedor.

## 6. Regla de continuidad

Nunca utilizar un JKS nuevo generado dentro de un runner. No publicar APKs si la verificación de firma o las pruebas fallan. Conservar los registros del SHA de Git, SHA-256 de binario, certificado y ejecución de Actions de cada entrega.
