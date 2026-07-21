# Resultados de Ejecución — CP30 a CP38
## Panel de Supervisor, Auditoría y Protección XSS

Responsable: Mateo Llumigusin
Fecha de ejecución: 2026-07-21
Rama: `pruebas/panel-supervisor-auditoria-xss`
Entorno: JDK 21.0.5 · Spring Boot 4.0.6 · H2 en memoria (perfil `test`) · Windows 11

> Contenido listo para trasladar a la sección 3.2 del SQAP.

---

## Resumen

| Métrica | Valor |
|---|---|
| CP implementados | 9 de 9|
| CP aprobados | 8 |
| CP con hallazgo (fallo esperado) | 1 (CP38) |
| Tests totales de la sección | 17 (15 activos + 2 de regresión deshabilitados) |
| Estado de la suite completa | 40 tests · 37 pasan · 2 skipped · 1 falla ajena |

---

## CP30

| Campo | Contenido |
|---|---|
| **IDENTIFICADOR** | CP30 |
| **DESCRIPCIÓN** | Verificar el acceso al panel de supervisor con un usuario que tiene el rol autorizado (SUPERVISOR). |
| **PRECONDICIONES** | Usuario autenticado con rol SUPERVISOR y token JWT válido. |
| **DATOS DE ENTRADA/PASOS** | 1. GET `/api/admin/pendientes` 2. GET `/api/admin/quarantine` 3. GET `/api/admin/audit-log` |
| **RESULTADO ESPERADO** | 200 OK en los tres endpoints, mostrando álbumes pendientes, imágenes en cuarentena y el historial de logs. |
| **RESULTADO OBTENIDO** | 200 OK en los tres. Se verificó el contenido: `titulo` del álbum pendiente, `nombreArchivo` y `estado=QUARANTINE` de la imagen, y `accion`/`usuarioId` del log. |
| **ESTADO** | **APROBADO** |

## CP31

| Campo | Contenido |
|---|---|
| **IDENTIFICADOR** | CP31 |
| **DESCRIPCIÓN** | Verificar que un usuario sin privilegios de supervisor no pueda acceder al panel administrativo. |
| **PRECONDICIONES** | Usuario autenticado con rol USER. |
| **DATOS DE ENTRADA/PASOS** | 1. GET `/api/admin/audit-log` con token de rol USER. |
| **RESULTADO ESPERADO** | 403 Forbidden; no se expone ningún log ni dato administrativo. |
| **RESULTADO OBTENIDO** | 403 Forbidden. Se verificó además que `auditLogRepository.findAll()` nunca se invocó: la barrera corta antes de consultar la base de datos. |
| **ESTADO** | **APROBADO** |

## CP32

| Campo | Contenido |
|---|---|
| **IDENTIFICADOR** | CP32 |
| **DESCRIPCIÓN** | Verificar el registro de auditoría al aprobar un álbum pendiente. |
| **PRECONDICIONES** | Existe un álbum pendiente (`aprobado=false`); supervisor autenticado. |
| **DATOS DE ENTRADA/PASOS** | 1. POST `/api/admin/albums/{id}/aprobar` |
| **RESULTADO ESPERADO** | El álbum queda `aprobado=true`; se crea un AuditLog con `accion=APROBAR_ALBUM`, el `usuarioId` del supervisor, `recursoId` del álbum y detalles con el título. |
| **RESULTADO OBTENIDO** | 200 OK. `aprobado=true` verificado sobre el objeto capturado. AuditLog capturado con `accion=APROBAR_ALBUM`, `usuarioId=supervisor`, `recursoId=1`, `recursoTipo=ALBUM`, `detalles` conteniendo el título e `ipAddress` no vacío. |
| **ESTADO** | **APROBADO** |

## CP33

| Campo | Contenido |
|---|---|
| **IDENTIFICADOR** | CP33 |
| **DESCRIPCIÓN** | Verificar el registro de auditoría al rechazar un álbum con imágenes asociadas. |
| **PRECONDICIONES** | Existe un álbum pendiente con imágenes asociadas. |
| **DATOS DE ENTRADA/PASOS** | 1. DELETE `/api/admin/albums/{id}/rechazar` |
| **RESULTADO ESPERADO** | El álbum y sus imágenes físicas se eliminan; se crea un log con `accion=RECHAZAR_ALBUM` y el detalle correspondiente. |
| **RESULTADO OBTENIDO** | 200 OK. Se crearon 2 archivos reales en directorio temporal y se comprobó `Files.exists()==false` para ambos tras la operación. Verificados `imageRepository.deleteAll()` y `albumRepository.delete()`. AuditLog con `accion=RECHAZAR_ALBUM`, `recursoTipo=ALBUM` y detalles con el título. |
| **ESTADO** | **APROBADO** |

## CP34

| Campo | Contenido |
|---|---|
| **IDENTIFICADOR** | CP34 |
| **DESCRIPCIÓN** | Verificar el registro de auditoría al aprobar una imagen que se encontraba en cuarentena. |
| **PRECONDICIONES** | Existe una imagen con estado QUARANTINE. |
| **DATOS DE ENTRADA/PASOS** | 1. PUT `/api/admin/image/{id}/approve` |
| **RESULTADO ESPERADO** | La imagen cambia a estado seguro; se genera un log con `accion=APROBAR_IMAGEN`; el panel refleja el nuevo total de amenazas neutralizadas. |
| **RESULTADO OBTENIDO** | 200 OK con mensaje "Imagen aprobada y enviada a la bóveda segura.". Verificada la llamada a `approveQuarantinedImage(5)`. AuditLog con `accion=APROBAR_IMAGEN`, `recursoTipo=IMAGE`, detalles con el nombre del archivo. Consulta posterior a `/api/admin/quarantine` devuelve lista vacía. |
| **ESTADO** | **APROBADO** |

## CP35

| Campo | Contenido |
|---|---|
| **IDENTIFICADOR** | CP35 |
| **DESCRIPCIÓN** | Verificar el registro de auditoría al rechazar/bloquear una imagen en cuarentena. |
| **PRECONDICIONES** | Existe una imagen con estado QUARANTINE. |
| **DATOS DE ENTRADA/PASOS** | 1. PUT `/api/admin/image/{id}/reject` |
| **RESULTADO ESPERADO** | La imagen se elimina de los servidores; se genera un log con `accion=RECHAZAR_IMAGEN`; el panel muestra el detalle de alerta. |
| **RESULTADO OBTENIDO** | 200 OK con mensaje "Imagen rechazada y eliminada de los servidores.". Verificada la llamada a `rejectQuarantinedImage(5)`. AuditLog con `accion=RECHAZAR_IMAGEN`, `recursoTipo=IMAGE`, detalles con el nombre del archivo. |
| **ESTADO** | **APROBADO** |

## CP36

| Campo | Contenido |
|---|---|
| **IDENTIFICADOR** | CP36 |
| **DESCRIPCIÓN** | Verificar la sanitización de un script malicioso ingresado en la descripción de un álbum. |
| **PRECONDICIONES** | Usuario con rol USER autenticado. |
| **DATOS DE ENTRADA/PASOS** | 1. POST `/api/albums/solicitar` con `descripcion = "<script>alert('XSS')</script>Hola"`. |
| **RESULTADO ESPERADO** | El álbum se guarda con la descripción escapada (`&lt;script&gt;...`); al renderizarse no se ejecuta ningún script. |
| **RESULTADO OBTENIDO** | 200 OK. Leído desde la base H2: la descripción **no** contiene `<script>` ni `</script>`, y **sí** contiene `&lt;script&gt;`, conservando el texto "Hola" al final. El álbum se guardó con `aprobado=false`. |
| **ESTADO** | **APROBADO** |

## CP37

| Campo | Contenido |
|---|---|
| **IDENTIFICADOR** | CP37 |
| **DESCRIPCIÓN** | Verificar que la sanitización se vuelva a aplicar al actualizar la descripción de un álbum existente. |
| **PRECONDICIONES** | Álbum existente con descripción previamente sanitizada. |
| **DATOS DE ENTRADA/PASOS** | 1. Actualizar el álbum con `descripcion = "<img src=x onerror=alert(1)>"`. |
| **RESULTADO ESPERADO** | La descripción almacenada queda neutralizada, sin el atributo `onerror` ejecutable. |
| **RESULTADO OBTENIDO** | La descripción almacenada no contiene `<img` ni ningún carácter `<`; contiene `&lt;img`. El callback `@PreUpdate` se ejecuta correctamente y el payload queda inerte. |
| **ESTADO** | **APROBADO** |
| **DESVIACIÓN** | El SQAP plantea el caso como actualización vía API, pero `AlbumController` **no expone ningún endpoint** para editar la descripción de un álbum existente. La prueba se ejecutó en la capa de persistencia, que es donde el control (`@PreUpdate`) realmente actúa. Si se agrega dicho endpoint, debe añadirse la variante HTTP. |

## CP38

| Campo | Contenido |
|---|---|
| **IDENTIFICADOR** | CP38 |
| **DESCRIPCIÓN** | Verificar si el campo título del álbum sanitiza contenido potencialmente malicioso (hallazgo potencial). |
| **PRECONDICIONES** | Usuario autenticado; endpoint POST `/api/albums/solicitar-lote` disponible. |
| **DATOS DE ENTRADA/PASOS** | 1. Enviar `titulo = "<script>alert(1)</script>"` junto con una descripción válida. |
| **RESULTADO ESPERADO** | El título debería sanitizarse igual que la descripción, sin permitir que el payload se almacene o renderice sin escapar. |
| **RESULTADO OBTENIDO** | 200 OK. Leído desde la base H2: la `descripcion` **sí** se sanitizó, pero el `titulo` quedó almacenado **literal**: `<script>alert(1)</script>`. El criterio de aceptación **no se cumple**. |
| **ESTADO** | **FALLIDO — hallazgo XSS-01 confirmado** |


---

## Cobertura JaCoCo — clases del alcance

Generado con `mvnw verify`. Reporte completo en `target/site/jacoco/index.html`.

| Clase | Instrucciones | Ramas | Líneas cubiertas | Líneas sin cubrir |
|---|---|---|---|---|
| `SecurityConfig` | 100 % | n/a | 34 | 0 |
| `Album` | 100 % | 50 % | 3 | 0 |
| `XssSanitizer` | 77.9 % | 16.7 % | 7 | 5 |
| `AuditService` | 77.8 % | 33.3 % | 16 | 3 |
| `SupervisorController` | 73.7 % | 28.6 % | 50 | 19 |
| `AlbumController` | 31.2 % | 8.3 % | 21 | 31 |
| `AuditLog` | 0 % | n/a | 0 | 2 |

**Lectura de los números.** `SupervisorController` (73.7 %) y `AuditService` (77.8 %) quedan
bien cubiertos: son el núcleo de la sección. `AlbumController` está bajo (31.2 %) porque solo
se ejercitaron los endpoints relevantes a XSS y autorización; el resto pertenece a los módulos
de otros integrantes. La cobertura de ramas es baja de forma generalizada porque los CP
recorren los caminos exitosos y las barreras de rol, no las ramas de excepción
(`catch IOException`, errores 500).

---

## Limitación conocida de CP32–CP35

`AuditLog` aparece con **0 % de cobertura**, y esto tiene una consecuencia real: el método
`@PrePersist onCreate()`, que asigna el campo `timestamp`, **nunca se ejecuta** en estas
pruebas porque `AuditLogRepository` está mockeado y no hay persistencia real.

Los criterios de aceptación del SQAP para el módulo de auditoría exigen explícitamente que
el log contenga **"Fecha y hora"**. Los CP32–CP35 verifican `usuarioId`, `accion`,
`recursoId`, `recursoTipo`, `detalles` e `ipAddress`, pero **no verifican `timestamp`**.

**Recomendación:** añadir un `@DataJpaTest` sobre `AuditLogRepository` que persista un
`AuditLog` real y afirme que `timestamp` queda asignado y es coherente con el momento de la
operación. Es una prueba corta y cierra el único criterio del SQAP que hoy queda sin evidencia.

---

## Hallazgos de la sección

| ID | Defecto | Severidad | Estado |
|---|---|---|---|
| SEC-01 | Falta `@EnableMethodSecurity`; un rol USER puede auto-aprobar álbumes, borrar álbumes ajenos y listar los pendientes | **Crítica** | Confirmado por ejecución |
| XSS-01 | El `titulo` del álbum no se sanitiza | Alta | Confirmado por ejecución (CP38) |
| CFG-01 | `zap-api-scan.bat` apunta a `/v3/api-docs` inexistente | Media | Bloquea CP39 |
| BLD-01 | Faltaba `spring-boot-starter-data-jpa-test`; rompía la compilación de toda la suite | Media | Corregido |
| CAL-01 | Doble escapado de `descripcion` en cada `@PreUpdate` | Baja | Confirmado por ejecución |

> **SEC-01 y CAL-01 no forman parte de los 10 CP del SQAP**: son hallazgos emergentes de la
> auditoría. Reportarlos en la sección de Análisis.

---

## Nota de trazabilidad

Todas las pruebas se ejecutaron sobre el estado del código previo a cualquier corrección de
los hallazgos. Si el equipo aplica la corrección de SEC-01 (activar method security), el
sistema auditado cambia y **los CP30–CP38 deben re-ejecutarse como regresión**.

Las pruebas de regresión de XSS-01 y SEC-01 quedan escritas y marcadas `@Disabled`; se
habilitan al corregir cada defecto.
