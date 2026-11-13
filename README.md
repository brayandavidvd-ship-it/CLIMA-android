# 🌤️ Clima App (Android Native)

Una aplicación móvil nativa para Android desarrollada en **Kotlin** y **Jetpack Compose**, diseñada con un enfoque minimalista, limpio y atmosférico. El fondo de la interfaz reacciona dinámicamente al clima real, transformándose según las condiciones meteorológicas actuales.

---

## ✨ Características Principales

* **Fondo Atmosférico Dinámico:** La interfaz cambia de color y animación fluida (soleado, lluvia, llovizna, niebla, tormenta, nieve, etc.) según el estado del clima real.
* **Cápsula de Resumen Rápido:** Acceso directo a métricas clave (humedad, viento, índice UV) desde la pantalla principal.
* **Pantalla de Detalles Completos:** Vista profunda con diseño *glassmorphism* que detalla el viento, humedad relativa, índice UV máximo, presión atmosférica, probabilidad de lluvia y ciclo solar (amanecer/atardecer).
* **Asistente de IA (Gemini en la nube):** Integración opcional con modelos de Google AI Studio (`gemini-3.5-flash-lite` y `gemini-3.5-flash`) para consultar el clima, resolver dudas sobre horas específicas o días futuros mediante lenguaje natural.
* **Sistema Multifuente de Clima:** Arquitectura extensible preparada para alternar entre proveedores meteorológicos (como Open-Meteo y fuentes de prueba/demo).
* **Recomendaciones Locales Automáticas:** Tarjetas inteligentes basadas en reglas fijas (protector solar, paraguas, abrigarse, etc.) que funcionan incluso sin IA ni conexión a internet.
* **Modo de Prueba (Debug):** Herramienta exclusiva para desarrolladores en versiones `debug` para forzar y probar cualquiera de los 9 estados del clima visualmente.

---

## 🎨 Especificaciones de Diseño

* **Estilo Minimalista:** Sin efectos de neón ni sombras artificiales pesadas. Tarjetas translúcidas con bordes sutiles y esquinas redondeadas.
* **Paleta de Colores Templada:** Diseñada para ofrecer una lectura limpia y cómoda con textos siempre en color blanco.
* **Adaptive Icon:** Icono de aplicación minimalista y colorido que combina un círculo azul vivo, un sol amarillo suave y una nube geométrica limpia.

---

## 📱 Tecnologías Utilizadas

* **Lenguaje:** Kotlin
* **UI Toolkit:** Jetpack Compose (Material 3)
* **APIs:** Open-Meteo API (Clima y Geocodificación) / Google AI Studio (Gemini SDK)
* **Arquitectura:** Componentes nativos optimizados con `graphicsLayer` para un rendimiento fluido a 60 FPS y respeto estricto de los *WindowInsets* del sistema.

---

## 🚀 Configuración y Uso

1. Clona este repositorio o impórtalo directamente en **Android Studio**.
2. Sincroniza las dependencias de Gradle.
3. Si deseas utilizar el asistente inteligente, abre los **Ajustes** dentro de la app, activa el asistente e ingresa tu clave de API gratuita obtenida en [Google AI Studio](https://aistudio.google.com/app/apikey).
