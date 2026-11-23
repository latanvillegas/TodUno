# 🌙 TodUno (4 en 1)

**Calendario Lunar, Observatorio Astronómico de la Luna, Reloj Astronómico/Mundial y Calculadora Científica en una sola APK para Android.**

---

## ✨ Módulos Incluidos

1. **📅 Calendario Lunar**: Cuadrícula mensual con miniaturas lunares reales, hitos lunares del mes (*Luna Nueva*, *Cuarto Creciente*, *Luna Llena* y *Cuarto Menguante*) y agenda personal con recordatorios persistentes tras reinicio.
2. **🌕 Observatorio Lunar**: Efemérides reales (algoritmos astronómicos de Jean Meeus), orientación fiel Hemisferio Norte/Sur, iluminación, edad sinódica, distancia geocéntrica, diámetro angular, tránsito y latitud eclíptica, además de superlunas y eclipses solares/lunares calculados dinámicamente para cualquier siglo.
3. **⌚ Reloj, Husos Horarios, Cronómetro y Temporizador**: Reloj analógico con esferas personalizables persistentes, hora digital (12h/24h), UTC, Tiempo Sideral (GMST), reloj mundial personalizable por el usuario con desfase exacto respecto a tu huso, cronómetro con vueltas y temporizador persistente.
4. **🧮 Calculadora Científica**: Funciones trigonométricas en grados (`DEG`) y radianes (`RAD`), logaritmos, potencias, raíces, constante `π`, tecla `Ans` e historial guardado localmente.

---

## 📦 Verificación y distribución

- **PR y actualizaciones a `main`:** pruebas unitarias Android y lint.
- **Etiquetas `v*` válidas:** compilan `TodUno-release.apk` y `TodUno-release.aab`, verifican el certificado de producción y publican un GitHub Release.
- **Firma fail-closed:** los secretos `KEYSTORE_BASE64`, `STORE_PASSWORD` y `KEY_PASSWORD` son obligatorios para publicar. No se generan claves temporales.
- **Próxima versión propuesta:** `v1.0.14` (versionCode 14). No etiquetar ni publicar hasta recuperar la clave de `v1.0.13` y completar las pruebas de dispositivo.

Consulta [el procedimiento de lanzamiento](docs/RELEASE_CHECKLIST.md), [la política de privacidad](docs/PRIVACY_POLICY.md) y [la ficha de tienda](docs/STORE_LISTING.md).

> La distribución en tiendas de código abierto necesita una decisión explícita del titular sobre la licencia del proyecto. El repositorio todavía no declara una licencia de reutilización.
