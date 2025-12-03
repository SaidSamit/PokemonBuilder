Pokémon Builder

Pokémon Builder es una aplicación Android minimalista para crear, editar y gestionar equipos de Pokémon. Combina datos en tiempo real de la PokéAPI con almacenamiento local para ofrecer búsquedas, selección de movimientos y flujos de edición pensados para ser una base educativa y ampliable.

---

Features

- Exploración de la Pokédex por nombre, generación o tipo.  
- Selección y edición de un Pokémon en un editor local con posibilidad de elegir movimientos.  
- Gestión de equipos: crear, renombrar y eliminar equipos asociados a usuarios locales; rellenar slots con selecciones desde listas.  
- Integración con PokéAPI para obtener nombres, especies por generación, Pokémon por tipo y movimientos.  
- Flujos de devolución: pantallas secundarias devuelven selecciones con metadatos (por ejemplo slot y teamId).  
- Tolerancia a fallos: fallbacks mínimos y mensajes claros cuando la API no responde.

---

Requisitos mínimos e instalación

Requisitos mínimos  
- Android mínimo: API 21 (Lollipop).  
- Compile SDK y Target SDK recomendados: 33.  
- Lenguaje: Java 8 o superior.  
- Permiso requerido: Internet para consumir la PokéAPI.

Instalación y ejecución  
1. Abrir el proyecto en Android Studio y verificar que el SDK requerido esté instalado.  
2. Configurar JDK 8 o superior en el entorno de desarrollo.  
3. Añadir permiso de Internet en el manifiesto de la aplicación.  
4. Reconstruir el proyecto y ejecutar en un emulador o dispositivo con conexión a Internet.  
5. Probar búsqueda, ordenamientos por tipo y generación, selección de Pokémon y edición de equipos.

---

Arquitectura y consumo de la PokéAPI

Visión general  
La app separa responsabilidades en tres capas: UI (Activities), red (cliente de la PokéAPI) y persistencia local (SQLite). Las Activities solicitan datos en segundo plano, parsean JSON y actualizan la UI en el hilo principal.

Qué consume de la PokéAPI  
- Nombres de Pokémon.  
- Especies por generación.  
- Pokémon por tipo.  
- Movimientos asociados a cada Pokémon.

Estrategia de red  
- Llamadas en hilos de fondo para no bloquear la UI.  
- Validación de respuestas y manejo de errores con mensajes al usuario.  
- Recomendación: añadir cache local y manejo de límites de la API para robustez.

---

Persistencia y comportamiento offline

Persistencia local  
- Usuarios y equipos se guardan en una base de datos local para permitir edición y consulta sin depender exclusivamente de la red.  
- Los equipos almacenan metadatos que facilitan reabrir y editar sin recargar todo desde la API.

Comportamiento offline  
- Funcionalidad parcial disponible sin conexión: gestión de equipos y edición de datos ya guardados.  
- Listados y búsquedas que dependen de la PokéAPI requieren conexión; la app muestra fallbacks o mensajes cuando no hay red.

Mejoras sugeridas  
- Implementar cache persistente de respuestas de la API para permitir búsquedas básicas sin conexión.  
- Sincronización incremental cuando la conexión se restablece.

---

Buenas prácticas pruebas y roadmap

Buenas prácticas aplicadas  
- Separación clara entre UI, red y persistencia.  
- Actualización de la interfaz solo desde el hilo principal.  
- Uso de metadatos para propagar selecciones entre pantallas.

Pruebas recomendadas  
- Pruebas instrumentadas para flujos de Activities (selección de Pokémon, edición de equipo).  
- Tests unitarios para la capa de persistencia y utilidades de parsing JSON.  
- Pruebas en emuladores y dispositivos con distintos niveles de API.

Roadmap sugerido  
- Migrar ListView a RecyclerView para mejor rendimiento.  
- Reemplazar AsyncTask por Executors, WorkManager o coroutines para código más moderno.  
- Añadir cache local y sincronización para robustez offline.  
- Incluir sprites e imágenes, filtros por estadísticas y mejoras de accesibilidad
