# ms-ventas

Microservicio de **ventas**: registra ventas, calcula el total y mantiene el historial. Forma parte de la solución de microservicios (MS-AUTH, MS-PRODUCTOS, MS-VENTAS, MS-INVENTARIO y API Gateway).

Al registrar una venta se comunica por **REST síncrono** con:

- **MS-PRODUCTOS** (`localhost:8082`): obtiene nombre y precio de cada producto.
- **MS-INVENTARIO** (`localhost:8084`): descuenta el stock. Si un producto no tiene stock suficiente, se devuelve lo ya descontado y la venta no se guarda.

## Tecnologías

- Java 21
- Spring Boot 3.5 (Web, Data JPA, Validation, RestClient)
- MySQL 8
- Maven
- JUnit 5 + Mockito

## Estructura

```
src/main/java/cl/duoc/msventas
├── controller   -> Endpoints REST
├── service      -> Lógica de negocio (cálculo de total, registro, anulación)
├── repository   -> Acceso a datos (Spring Data JPA)
├── model        -> Entidades JPA (Venta 1 ── * DetalleVenta)
├── client       -> Clientes REST hacia MS-PRODUCTOS y MS-INVENTARIO
├── config       -> Configuración de RestClient
├── dto          -> Objetos de entrada y salida
└── exception    -> Manejo global de errores
```

## Requisitos

- Java 21
- Maven 3.9+
- MySQL 8 corriendo en `localhost:3306`
- **ms-productos** y **ms-inventario** en ejecución para registrar ventas

## Cómo ejecutar

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/maca2003/ms-ventas.git
   cd ms-ventas
   ```
2. Revisar el usuario y contraseña de MySQL en `src/main/resources/application.properties` (por defecto `root` / `root`). La base de datos `ventas_db` se crea automáticamente.
3. Compilar, ejecutar pruebas y empaquetar:
   ```bash
   mvn clean install
   ```
4. Ejecutar:
   ```bash
   java -jar target/ms-ventas-1.0.0.jar
   ```
   o bien `mvn spring-boot:run`.

El servicio queda disponible en `http://localhost:8083`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/ventas?vendedor=` | Historial de ventas (filtro opcional por vendedor) |
| GET | `/api/ventas/{id}` | Obtener venta con su detalle |
| POST | `/api/ventas` | Registrar venta |
| PUT | `/api/ventas/{id}/anular` | Anular venta (devuelve el stock) |
| DELETE | `/api/ventas/{id}` | Eliminar venta (solo si está anulada) |

El vendedor se toma del header `X-Usuario`, que agrega el API Gateway a partir del token JWT.

### Ejemplo de cuerpo

Registrar venta:
```json
{
  "items": [
    { "productoId": 1, "cantidad": 2 },
    { "productoId": 2, "cantidad": 1 }
  ]
}
```

## Respuestas de error

| Código | Caso |
|---|---|
| 400 | Venta sin productos o cantidad inválida |
| 404 | Venta o producto no encontrado |
| 409 | Stock insuficiente, producto inactivo o venta ya anulada |
| 503 | MS-PRODUCTOS o MS-INVENTARIO no disponibles |
