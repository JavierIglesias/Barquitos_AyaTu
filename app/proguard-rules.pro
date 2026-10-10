# Barquitos Ayatu — reglas de R8/ProGuard.
# El motor de juego y el protocolo de red se serializan a mano (JSON propio), así que no hace
# falta conservar nombres de clases. Solo se mantiene lo que usa la reflexión de Compose.
-dontwarn org.jetbrains.annotations.**
