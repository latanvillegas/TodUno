# Lanzamiento de TodUno 0.0.1 con firma permanente

## Versión pública e identidad Android

- **Versión pública inicial:** `0.0.1` (`versionName`).
- **Etiqueta GitHub:** `v0.0.1`.
- **`versionCode=20001`** (identificador interno Android, distinto de la versión visible).
- **`applicationId=com.aistudio.selenialunar.kxmpzq`** sin cambios.
- **Clave privada:** la JKS persistente creada en Termux (`~/.config/toduno/signing/toduno-release.jks`, alias `upload`).
- **Huella pública SHA-256:** `8388ac50c1cfba89cff8d0df9c08f196e612d91f4836ef425ce5b14138778afe`.

### Por qué `versionCode` no es 1

La primera compilación de prueba, mostrada como `2.0.0`, ya se instaló en Android con `versionCode=20000` y el **mismo certificado**. Android exige un `versionCode` mayor para actualizar normalmente esa instalación sin desinstalar. Por eso `0.0.1` utiliza `20001`. La etiqueta, ficha y versión visible siguen siendo **0.0.1**, como primera publicación de la nueva línea. Para versiones posteriores, incrementar `versionCode` sin reutilizar números.

**Compatibilidad con publicaciones antiguas:** los APK históricos `v1.x` fueron firmados con un certificado distinto. No se pueden actualizar directamente con `v0.0.1`; quien use esos APK debe exportar su JSON, guardar una copia accesible fuera de la app, desinstalar la versión anterior, instalar la nueva y restaurar el respaldo. En cambio, la compilación de prueba `2.0.0` firmada con la misma JKS sí es candidata a actualizarse directamente a `0.0.1` sin borrar datos.

## Clave y secretos: ya configurados desde Termux

El script `bash tools/setup_signing_termux.sh` crea o reutiliza la JKS persistente, muestra su huella y configura los secretos de Actions: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`, `RELEASE_CERT_SHA256`. Verificar sus nombres con `gh secret list -R latanvillegas/TodUno`.

**No generar una clave adicional** para `0.0.1`. Mantener una copia cifrada y accesible del JKS fuera del dispositivo, así como las contraseñas en un gestor seguro. Nunca publicar esas credenciales ni el contenido base64.

## Comprobación de candidato 0.0.1 antes de publicar

Tras integrar este cambio a `main`, la CI debe aprobar los tests y Android Lint. Ejecutar desde Termux:

```bash
cd "$HOME/TodUno-v2"
git pull --ff-only origin main
gh workflow run android-release.yml -R latanvillegas/TodUno --ref main
gh run list -R latanvillegas/TodUno --workflow android-release.yml --event workflow_dispatch --limit 5
```

Seleccionar el **ID numérico real de la nueva ejecución**, esperar a que termine con `success` y descargar:

```bash
RUN_ID=123456789  # Reemplazar por el ID numérico real de la nueva ejecución
gh run watch "$RUN_ID" -R latanvillegas/TodUno --exit-status
mkdir -p "$HOME/toduno-0.0.1-candidate"
gh run download "$RUN_ID" -R latanvillegas/TodUno --name TodUno-signed-candidate --dir "$HOME/toduno-0.0.1-candidate"
cd "$HOME/toduno-0.0.1-candidate"
sha256sum -c SHA256SUMS
```

El workflow verifica paquete, versión `0.0.1`, `versionCode >= 20001`, firma del APK coincidente con `RELEASE_CERT_SHA256`, alineación ZIP de 16 KB, y firma del AAB. Descarga de candidato **no** publica GitHub Release.

Actualizar la instalación de prueba `2.0.0` a `0.0.1` sin desinstalar; confirmar conservación de datos y funcionamiento de calendario, notas, recordatorios, observatorio, reloj y calculadora. Probar también la restauración JSON y el funcionamiento después del reinicio. Si falla una verificación, corregir antes de crear tag.

## Crear publicación oficial

Solo con pruebas y respaldos aprobados, crear etiqueta **`v0.0.1`** en un commit verificado de `main`. El workflow compila y publica `TodUno-release.apk`, `TodUno-release.aab`, `SHA256SUMS` en GitHub Releases, sin sobrescribir publicaciones anteriores.

Antes de solicitar Orion Store u otros catálogos: revisar icono, capturas de la versión final, política de privacidad, contacto del editor, licencia/distribución de recursos y formularios exigidos por cada tienda. No declarar aprobación de ninguna tienda sin confirmación.

**Continuidad:** nunca generar una clave nueva dentro del runner de GitHub. Conservar registro de commit, etiqueta, huella de certificado, hashes SHA-256 y ejecución CI de cada lanzamiento.
