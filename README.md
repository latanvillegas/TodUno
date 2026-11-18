# 🌙 TodUno (4 en 1)

**Calendario Lunar, Observatorio Astronómico de la Luna, Reloj Astronómico/Mundial y Calculadora Científica en una sola APK para Android.**

---

## ✨ Módulos Incluidos

1. **📅 Calendario Lunar**: Cuadrícula mensual con miniaturas lunares reales, hitos lunares del mes (*Luna Nueva*, *Cuarto Creciente*, *Luna Llena* y *Cuarto Menguante*) y agenda personal con recordatorios persistentes tras reinicio.
2. **🌕 Observatorio Lunar**: Efemérides reales (algoritmos astronómicos de Jean Meeus), orientación fiel Hemisferio Norte/Sur, iluminación, edad sinódica, distancia geocéntrica, diámetro angular, tránsito y latitud eclíptica, además de superlunas y eclipses solares/lunares calculados dinámicamente para cualquier siglo.
3. **⌚ Reloj, Husos Horarios, Cronómetro y Temporizador**: Reloj analógico con esferas personalizables persistentes, hora digital (12h/24h), UTC, Tiempo Sideral (GMST), reloj mundial personalizable por el usuario con desfase exacto respecto a tu huso, cronómetro con vueltas y temporizador persistente.
4. **🧮 Calculadora Científica**: Funciones trigonométricas en grados (`DEG`) y radianes (`RAD`), logaritmos, potencias, raíces, constante `π`, tecla `Ans` e historial guardado localmente.

---

## 📦 Compilación Automatizada (Solo Release APK)

El flujo `.github/workflows/android-release.yml` está configurado para ejecutar **únicamente** la compilación de `TodUno-release.apk` (`:app:assembleRelease`), sin compilar variantes `debug`, sin ejecutar suites adicionales ni publicar lanzamientos o etiquetas automáticas. El archivo firmado `TodUno-release.apk` queda disponible directamente como *Artifact* de la ejecución en GitHub Actions.
