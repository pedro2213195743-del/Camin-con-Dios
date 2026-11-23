# Camino con Dios — Proyecto Android Nativo (APK Independiente 100% Offline)

Este proyecto genera un **APK nativo de Android** (`com.caminocondios.app`) **sin barra de URL**, sin dependencias de APK Forge ni AppsGeyser, y con **voz en español offline nativa (`AndroidTTS`)** + **Biblia completa de 66 libros incrustada dentro del APK**.

## Compilar el APK en 2 minutos (sin instalar Android Studio)

1. Crea un repositorio nuevo (público o privado) en [GitHub](https://github.com/new) llamado `camino-con-dios`.
2. Descomprime este ZIP y sube todos los archivos (incluyendo la carpeta `.github`) al repositorio.
3. Abre la pestaña **Actions** en GitHub y entra a **Compilar APK Independiente (Camino con Dios)**.
4. En ~2 minutos aparecerá el archivo **`CaminoConDios-APK-Offline`** en la sección **Artifacts**: descárgalo, descomprímelo e instala el `.apk` en tu Android.

## Compilar localmente en Android Studio

1. Abre esta carpeta en Android Studio.
2. Opcional: descarga `es_rvr1960.json` en `app/src/main/assets/www/bible_es.json` para incluir la Biblia completa desde el primer segundo sin internet.
3. Ve a **Build → Generate Signed App Bundle / APK → APK**.
