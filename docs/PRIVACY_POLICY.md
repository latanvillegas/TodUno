# Política de privacidad de TodUno

**Última actualización:** 24 de noviembre de 2026

TodUno es una aplicación Android para consultar un calendario lunar, observar fenómenos astronómicos, utilizar relojes, temporizadores y una calculadora científica. Esta política describe el comportamiento de la aplicación según el código Android publicado en este repositorio.

## Información que conserva la aplicación

- Las notas del calendario y sus recordatorios se guardan localmente mediante una base de datos Room.
- El historial de la calculadora, los husos horarios seleccionados y las preferencias se guardan en el dispositivo.
- La aplicación puede generar un archivo JSON cuando el usuario elige exportar una copia de sus datos. El usuario decide dónde guardar o compartir dicho archivo mediante el selector de archivos de Android.

## Conexiones y terceros

La versión Android documentada no declara el permiso `android.permission.INTERNET` y sus funciones principales se ejecutan sin conexión. El código actual no incluye una integración activa de anuncios, analítica ni cuentas remotas. Los servicios del propio sistema operativo y los componentes instalados en el dispositivo pueden tener políticas independientes.

## Permisos

- **Notificaciones:** avisos de temporizador, notas y eventos lunares, previa autorización del usuario cuando Android lo exige.
- **Vibración:** avisos del temporizador y notificaciones.
- **Inicio del dispositivo:** reprogramar recordatorios después de reiniciar.

La aplicación no solicita permisos de ubicación ni de acceso general al almacenamiento.

## Copias de seguridad del sistema

El manifiesto conserva `android:allowBackup="true"` para permitir transferencias compatibles entre dispositivos. La configuración excluye el directorio privado de la aplicación de **las copias automáticas en la nube**. En Android 12 o superior, las reglas separadas permiten la transferencia entre dispositivos de la base de datos y preferencias; en Android 11 o anterior, las reglas de copia automática excluyen los datos de la aplicación. Las funciones de exportación y restauración JSON siguen disponibles para realizar respaldos manuales. Los proveedores y mecanismos de transferencia del sistema operativo aplican sus propios controles y políticas.

## Conservación y control

Las notas, historial y preferencias permanecen en el dispositivo mientras la aplicación conserve sus datos. La aplicación permite borrar las notas y el historial desde sus funciones correspondientes y exportar o restaurar un respaldo local. Desinstalar la aplicación normalmente elimina los datos locales, sin garantizar la eliminación de archivos JSON exportados o de copias administradas por Android.

## Contacto

Para dudas o solicitudes relativas a esta política, abre una incidencia en el [repositorio del proyecto](https://github.com/latanvillegas/TodUno/issues).

**Antes de distribuir en una tienda:** el editor debe validar esta política frente al APK final y facilitar un contacto de soporte que cumpla las exigencias de la tienda. Si se integran anuncios, analítica, servicios online o nuevas categorías de datos, esta política debe actualizarse antes de publicar.
