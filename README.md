# TodUno

**Calendario lunar, observatorio astronómico, relojes y calculadora científica para Android.**

TodUno reúne cuatro herramientas en una sola aplicación, con funciones principales disponibles sin conexión y sin necesidad de crear una cuenta.

[**Descargar TodUno 0.0.1**](https://github.com/latanvillegas/TodUno/releases/tag/v0.0.1) · [Política de privacidad](docs/PRIVACY_POLICY.md) · [Avisos de seguridad](SECURITY.md)

## Funciones

| Módulo | Qué ofrece |
| --- | --- |
| Calendario lunar | Fases lunares, eventos, agenda personal y recordatorios. |
| Observatorio | Información astronómica lunar, próximos fenómenos y vista según el hemisferio. |
| Relojes | Hora local y mundial, cronómetro y temporizador. |
| Calculadora | Operaciones científicas e historial local. |

## Instalación

**Versión pública actual: 0.0.1.** Requiere Android 7.0 o posterior.

1. Abre la [publicación oficial](https://github.com/latanvillegas/TodUno/releases/tag/v0.0.1).
2. Descarga `TodUno-release.apk` desde sus archivos adjuntos.
3. Abre el archivo en Android y sigue las indicaciones del instalador.

El archivo `.aab` se utiliza para distribución mediante tiendas y no se instala directamente.

### Verificar la descarga

La publicación incluye `SHA256SUMS` para comprobar que los archivos no se han alterado durante la descarga. En un sistema con `sha256sum`, ejecuta el siguiente comando **en una carpeta que contenga el APK, el AAB y el archivo SHA256SUMS de la misma publicación**:

```bash
sha256sum -c SHA256SUMS
```

Ambos archivos deben indicar `OK`. Esta comprobación de integridad **no sustituye** la validación del origen oficial de la descarga.

### Mantener los datos

Antes de desinstalar la aplicación o cambiar de dispositivo, utiliza la función de exportación para realizar una copia de seguridad. Guarda los respaldos personales en una ubicación segura y no los adjuntes a incidencias públicas.

Si Android indica que una versión instalada tiene una firma incompatible, respalda tus datos antes de desinstalarla e instala la versión oficial; restaura después la copia de seguridad. No fuerces la instalación ni borres datos sin disponer de una copia válida.

## Privacidad y seguridad

TodUno no requiere una cuenta para sus funciones principales. Las preferencias y la información personal de la aplicación se gestionan localmente, sujetas también a las opciones de respaldo del propio Android.

Consulta la [política de privacidad](docs/PRIVACY_POLICY.md) para conocer los permisos, la conservación de datos y el comportamiento de las copias de seguridad.

Los archivos oficiales se distribuyen a través de [GitHub Releases](https://github.com/latanvillegas/TodUno/releases/latest). Para comunicar una vulnerabilidad utiliza la [política de seguridad](SECURITY.md); **no publiques credenciales, respaldos personales ni detalles de explotación en Issues**.

## Soporte

- [Incidencias y sugerencias generales](https://github.com/latanvillegas/TodUno/issues)
- [Publicación actual](https://github.com/latanvillegas/TodUno/releases/latest)
- [Política de privacidad](docs/PRIVACY_POLICY.md)
- [Política de seguridad](SECURITY.md)

## Derechos de uso

El repositorio no incluye actualmente un archivo `LICENSE`. La visibilidad pública del código no concede por sí sola permisos de reutilización o redistribución. Cualquier licencia futura deberá ser autorizada expresamente por el titular del proyecto.
