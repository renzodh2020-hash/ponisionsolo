# Crop Route 1.3.0 — Fabric / Minecraft 1.20.4

Mod de cliente para grabar y reproducir rutas. Basado en el proyecto `sacarbloques` del usuario.

## Controles

- **J**: iniciar o terminar una grabación.
- **K**: abrir el selector de rutas. Incluye **Temporizador** y **Vueltas P**.
- **O**: iniciar/detener la ruta seleccionada con el temporizador propio de O.
- **P**: iniciar o cancelar el ciclo infinito independiente del temporizador de O.

El temporizador de O se configura en **K > Temporizador** y se guarda en
`config/crop-route/timer.json`. Al guardarlo aparece un mensaje en el chat.

En **K > Vueltas P** se pueden configurar de forma independiente las vueltas de
`RTAFK`, `verruga11` y `verruga1` (de 1 a 9999; por defecto, 40 cada una).
La configuración se guarda en `config/crop-route/vueltas-p.json` y los cambios se
aplican al siguiente inicio del modo P.

## Ciclo infinito de P

1. Selecciona siempre **RTAFK** y recorre las vueltas configuradas.
2. Espera aleatoriamente entre **5 min 42 s y 6 min 18 s**.
3. Envía `/is go`, espera aleatoriamente entre **3 y 4 minutos** y envía `/back`.
4. Verifica durante **30 segundos como máximo** que el jugador regrese cerca de
   X 5237, Z 4621 (radio de 15 bloques). Si falla, detiene P por seguridad.
5. Selecciona **verruga11** y recorre sus vueltas configuradas.
6. Espera aleatoriamente entre **5 min 42 s y 6 min 18 s**.
7. Selecciona **verruga1** y recorre sus vueltas configuradas.
8. Espera aleatoriamente entre **20 min 42 s y 21 min 18 s**.
9. Vuelve directamente a **RTAFK** y repite el ciclo sin límite.

Una vuelta se registra al completar el avance equivalente a **toda la longitud de
la ruta circular guardada** y haber pasado cerca de su punto de referencia.
Para RTAFK y verruga11 se comprueba X 5237, Z 4621; para verruga1, X 5292,
Z 4796. La tolerancia del punto de referencia es **2 bloques**.
No se exige recorrer exactamente por la coordenada ni se utiliza la regla
anterior de recorrer solo el 75 % de la ruta. Los saltos de teletransporte
no suman vueltas.

P utiliza cambios directos entre las rutas, sin rutas de traslado adicionales
ni teletransporte salvo los comandos indicados. Los comandos `/is go` y `/back`
dependen del servidor. Al presionar P de nuevo se cancela el ciclo completo.

La detección de eventos y sus pausas automáticas anteriores se han eliminado.

## Requisitos y compilación

- Minecraft 1.20.4
- Fabric Loader 0.15.11
- Fabric API 0.97.2+1.20.4
- Java 17

Sube los archivos indicados en **ARCHIVOS-PARA-GITHUB.txt** a tu repositorio.
GitHub > **Actions** > **Build Crop Route** > **Run workflow** (si no se ejecuta
por un push). Cuando termine correctamente, descarga el artefacto, extrae
`crop-route-1.3.0.jar` y colócalo en `.minecraft/mods` retirando versiones
anteriores de Crop Route. Conserva y respalda el `routes.json` de tu instalación.

**Comprobación realizada:** compilación de sintaxis Java 17 con simuladores de
las API de Minecraft y pruebas del contador con las tres rutas reales, más una
simulación de la secuencia completa. **Pendiente:** compilación real Fabric/Gradle
con GitHub Actions y prueba de comportamiento en Minecraft.
