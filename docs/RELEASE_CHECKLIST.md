# Publicación de TodUno 2.0.0 con firma nueva

## Identidad del lanzamiento

- **Versión nueva:** `2.0.0`
- **Git tag:** `v2.0.0` (no reutilizar `v1.0.13`)
- **versionCode:** `20000`
- **applicationId:** `com.aistudio.selenialunar.kxmpzq` (sin cambios)
- **Clave privada:** JKS generada en Termux, alias `upload`, RSA 4096.
- **Verificación del firmante:** SHA-256 del certificado público almacenado en `RELEASE_CERT_SHA256`.

**Advertencia de compatibilidad:** el nuevo certificado de firma es distinto al utilizado para `v1.x`. Android no permite actualizar normalmente una instalación anterior con un APK que lleva otro certificado, aunque el número de versión sea mayor. Como el `applicationId` se conserva, la versión nueva no puede instalarse junto a la anterior. Es obligatorio respaldar datos, desinstalar y reinstalar, salvo que exista una rotación de firma compatible expresamente configurada y verificada.

## Preparación con Termux

1. Instalar herramientas: `pkg update && pkg install -y openjdk-21 gh coreutils git`.
2. Autenticarse en GitHub desde Termux con `gh auth login --hostname github.com --git-protocol https --web` (el script también ofrecerá iniciar sesión).
3. Clonar o actualizar este repositorio y ejecutar `bash tools/setup_signing_termux.sh`.
4. El script crea `~/.config/toduno/signing/toduno-release.jks` si aún no existe, solicita la contraseña sin mostrarla, calcula la huella del certificado y carga **cuatro secretos de repositorio**:
   - `KEYSTORE_BASE64`: archivo JKS codificado en base64.
   - `STORE_PASSWORD`: contraseña del almacén.
   - `KEY_PASSWORD`: contraseña de la clave (igual a la del almacén en este procedimiento).
   - `RELEASE_CERT_SHA256`: huella SHA-256 pública del certificado.
5. Comprobar los nombres con `gh secret list -R latanvillegas/TodUno` o en Settings → Secrets and variables → Actions. GitHub no revela el contenido.
6. Guardar el JKS y su contraseña mediante un respaldo seguro fuera del dispositivo. **Nunca** subir JKS, base64 o contraseñas al repositorio, incidencias o chat. Si se pierde esa clave, las futuras actualizaciones volverán a sufrir incompatibilidad de firma.

## Validación del APK/AAB

En cada PR y push a `main`, GitHub Actions ejecuta pruebas unitarias y Android Lint. La publicación se lanza únicamente al crear una etiqueta `v*` que apunta a un commit existente en `main`.

En el proceso de publicación se exige:

- Los cuatro secretos presentes, sin generación de claves temporales.
- Compilación de `assembleRelease` y `bundleRelease`.
- APK con el paquete esperado, versión `2.0.0` y `versionCode >= 20000`.
- APK firmado y certificado coincidente con `RELEASE_CERT_SHA256`.
- ZIP alineado para páginas de 16 KB, y AAB verificado con `jarsigner`.
- Archivos `TodUno-release.apk`, `TodUno-release.aab` y `SHA256SUMS` en GitHub Releases.
- No sobrescribir etiquetas o releases existentes.

La firma del APK nuevo debe comprobarse también en un dispositivo Android real antes de solicitar la aprobación de tiendas.

## Migración de usuarios de v1.x

1. Abrir la versión antigua y **exportar JSON** de calendario, notas, preferencias e historial.
2. Guardar el archivo JSON fuera del área privada de la aplicación, en un lugar accesible tras desinstalar.
3. Comprobar que la exportación y una copia de seguridad adicional son legibles.
4. Desinstalar la versión antigua e instalar la versión `2.0.0` con la firma nueva.
5. Restaurar los datos desde el JSON y comprobar notas, recordatorios, reloj, widget e historial.

La migración **no es automática** y no está validada hasta comprobarla con el APK final.

## Requisitos de tiendas aún pendientes

- Confirmar los derechos y la licencia del código y de los recursos gráficos, especialmente antes de solicitar IzzyOnDroid o F-Droid.
- Contacto público de soporte, identidad del editor y formularios de privacidad.
- Revisar capturas y dimensiones del icono contra la versión final, y verificar permisos.
- Crear una etiqueta nueva `v2.0.0` **solo cuando los secretos estén cargados y las pruebas hayan aprobado**.
- Tras una publicación correcta en GitHub Releases, presentar la solicitud de Orion Store y revisar las condiciones de otras tiendas.

No usar el certificado de `v1.0.13` como valor esperado para la nueva clave. No afirmar que v2 puede actualizar v1 sin reinstalación.
