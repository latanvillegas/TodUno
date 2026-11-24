# Política de seguridad — TodUno

Este documento establece cómo comunicar vulnerabilidades y cómo proteger los artefactos de publicación y los datos de quienes usan TodUno.

## Versiones y alcance

| Línea | Estado |
| --- | --- |
| `v0.0.1` (línea de firma actual) | Publicación estable actual. |
| APK históricos `v1.x` | Línea anterior con firma distinta; no se consideran actualizaciones compatibles con `v0.0.1`. |

La versión compatible más reciente está disponible en [GitHub Releases](https://github.com/latanvillegas/TodUno/releases/latest). No se promete un plazo específico de remediación ni soporte extendido para publicaciones históricas.

## Notificación privada de vulnerabilidades

1. Comprueba si el repositorio ofrece **Security → Advisories → Report a vulnerability** y utiliza esa función privada cuando esté disponible.
2. Si no está habilitada, solicita al mantenedor un canal privado adecuado **sin compartir detalles explotables ni material confidencial en una Issue pública**.
3. Describe versión afectada, impacto, pasos de reproducción mínimos, dispositivos probados y posible mitigación; elimina identificadores, datos personales, credenciales y secretos de toda evidencia.
4. Coordina la divulgación pública después de validar la corrección. No asumas que una incidencia pública es un canal de divulgación privada.

**Nunca adjuntes** tokens, contraseñas, claves privadas, archivos JKS/PKCS12, contenido base64 de claves, copias JSON de usuarios, capturas con datos privados o volcados de memoria sin depurar.

## Modelo de protección de claves y artefactos

- **Custodia:** la clave privada de firma se conserva fuera del control de versiones, con permisos restringidos y una copia de respaldo cifrada fuera del dispositivo de trabajo.
- **Separación:** los builds de pruebas/lint no requieren secretos de publicación. La firma de distribución solo se ejecuta en el flujo autorizado de GitHub Actions.
- **Fallo seguro:** si faltan credenciales de firma o no coincide el certificado esperado, el workflow debe detenerse, no crear otra clave ni publicar un APK diferente.
- **Validación:** la CI verifica paquete, versión, firma y certificado del APK, alineación de 16 KB, firma del AAB y sumas SHA-256.
- **Distribución:** los binarios oficiales se obtienen únicamente de [GitHub Releases](https://github.com/latanvillegas/TodUno/releases). Las sumas de comprobación ayudan a detectar corrupción, pero por sí solas no acreditan la identidad del editor frente a una fuente comprometida.
- **Datos de usuario:** los respaldos JSON pueden contener información personal y deben almacenarse y compartirse con medidas proporcionales a su sensibilidad.

Las reglas de `.gitignore` reducen inclusiones accidentales, pero no impiden el uso de `git add -f`, la exposición en logs, ni la filtración de claves que ya hayan sido publicadas. No deben presentarse como un control suficiente.

## Respuesta a incidentes

Si se sospecha exposición de credenciales o material criptográfico:

1. Suspender temporalmente la distribución afectada y preservar evidencia **sin publicarla**.
2. Revocar o reemplazar inmediatamente tokens y otras credenciales renovables, y auditar su uso.
3. Si se expuso la **clave privada de firma**, tratarla como comprometida; evaluar el procedimiento de recuperación o rotación admitido por los canales de distribución y las consecuencias sobre las actualizaciones existentes.
4. Comprobar accesos, cambios en GitHub Actions, etiquetas, releases y artefactos; determinar el alcance real antes de reanudar la publicación.
5. Comunicar a los usuarios los pasos de mitigación pertinentes sin divulgar nuevos secretos.

No confíes en borrar un commit o una incidencia como única remediación: los datos públicos pueden haber sido copiados.

## Privacidad

Para cuestiones de datos locales, permisos de Android, archivos exportados o copias de seguridad del sistema, consulta [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md).
