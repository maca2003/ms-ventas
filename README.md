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

## Docker

El `Dockerfile` usa dos etapas:

1. **Compilación** (`maven:3.9.9-eclipse-temurin-21`): descarga las dependencias (en una capa separada para aprovechar la caché) y genera el `.jar`.
2. **Ejecución** (`eclipse-temurin:21-jre-alpine`): imagen liviana solo con el JRE, usuario sin privilegios (uid 1001), `HEALTHCHECK` y la JVM limitada al 75% de la memoria del contenedor.

```bash
docker build -t ms-ventas:1.0.0 .
docker run -p 8083:8083 -e DB_HOST=host.docker.internal -e DB_PASSWORD=root --name ms-ventas ms-ventas:1.0.0
```

`DB_HOST=host.docker.internal` permite al contenedor usar el MySQL instalado en tu computador. Para levantar el sistema completo (todos los servicios + MySQL) usa el `docker-compose.yml` del repositorio [infraestructura-swarm](https://github.com/maca2003/infraestructura-swarm).

## CI/CD (GitHub Actions)

Archivo `.github/workflows/ci-cd.yml`. Se ejecuta en cada push o pull request a `main` y `develop`:

| Etapa | Ramas | Qué hace |
|---|---|---|
| Build y test | todas | `mvn clean verify` (compila, ejecuta pruebas y genera el `.jar`) |
| Provisionar Amazon ECR | `main` | Crea el repositorio de imágenes `ms-ventas` en ECR si no existe |
| Construir y publicar imagen | `main` | `docker build` y `docker push` a ECR con etiquetas `<commit>` y `latest` |
| Desplegar en Docker Swarm | `main` | Por SSH al manager: `docker service update --image ... sistema_ms-ventas` y verifica que `/api/ventas` responda 200 a través del API Gateway |

Secrets requeridos (Settings → Secrets and variables → Actions):

| Secret | Valor |
|---|---|
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | Learner Lab → AWS Details → AWS CLI (cambian en cada sesión) |
| `EC2_SSH_KEY` | Contenido de `labsuser.pem` |
| `EC2_HOST` | IP pública del manager (salida `IpPublicaManager` del stack `cluster-swarm`) |

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
