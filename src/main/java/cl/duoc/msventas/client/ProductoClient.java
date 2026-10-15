package cl.duoc.msventas.client;

import cl.duoc.msventas.client.dto.ProductoDto;
import cl.duoc.msventas.exception.RecursoNoEncontradoException;
import cl.duoc.msventas.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente REST hacia MS-PRODUCTOS.
 */
@Component
public class ProductoClient {

    private final RestClient restClient;

    public ProductoClient(@Qualifier("productosRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public ProductoDto obtenerProducto(Long productoId) {
        try {
            return restClient.get()
                    .uri("/api/productos/{id}", productoId)
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                        throw new RecursoNoEncontradoException("El producto " + productoId + " no existe");
                    })
                    .body(ProductoDto.class);
        } catch (RestClientException ex) {
            throw new ServicioNoDisponibleException("MS-PRODUCTOS no está disponible en este momento");
        }
    }
}
