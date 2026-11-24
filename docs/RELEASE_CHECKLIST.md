# Proceso de calidad y publicación

Esta guía pública describe los requisitos verificables de un lanzamiento. Las instrucciones de aprovisionamiento, credenciales, custodia y recuperación de materiales privados se mantienen **fuera del repositorio público**.

## Publicación actual

- **Versión:** `0.0.1`.
- **Fuente oficial:** [GitHub Releases](https://github.com/latanvillegas/TodUno/releases/latest).
- **Archivos distribuidos:** APK, Android App Bundle y manifiesto de hashes SHA-256.

## Criterios de aceptación

Antes de publicar una versión, el responsable debe comprobar:

1. Las pruebas automatizadas y el análisis estático finalizan correctamente.
2. Los artefactos de distribución están firmados y las verificaciones automatizadas de identidad, versión e integridad aprueban.
3. La versión candidata ha sido probada en al menos un dispositivo Android compatible.
4. La actualización y el tratamiento de datos existentes se han comprobado con un respaldo previo.
5. La ficha de tienda, la política de privacidad y los recursos gráficos reflejan la versión que se va a distribuir.
6. No se adjuntan credenciales, datos personales ni material de uso interno a cambios, incidencias o publicaciones.

## Gestión de información sensible

- La documentación pública no debe incluir claves privadas, contraseñas, tokens, copias de seguridad reales ni instrucciones de recuperación de secretos.
- Los procedimientos internos de firma y respuesta a incidentes deben permanecer en un sistema privado con acceso restringido.
- Las verificaciones públicas de integridad no sustituyen la autenticidad del origen de los archivos.
- Una revisión de documentación o una regla de ignorados no constituye una garantía de ausencia de fugas: deben utilizarse controles de acceso, análisis de secretos y revisión de cambios.

## Distribución en tiendas

La publicación en [GitHub Releases](https://github.com/latanvillegas/TodUno/releases/latest) no implica aceptación automática por tiendas externas. Antes de solicitar una incorporación, revisar los requisitos de la tienda, licencias de los recursos, privacidad, soporte y metadatos.

Para comunicar vulnerabilidades, consultar [SECURITY.md](../SECURITY.md).
