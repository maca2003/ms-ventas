package cl.duoc.msventas.client;

import cl.duoc.msventas.client.dto.MovimientoDto;
import cl.duoc.msventas.exception.ConflictoException;
import cl.duoc.msventas.exception.RecursoNoEncontradoException;
import cl.duoc.msventas.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente REST hacia MS-INVENTARIO.
 */
@Component
public class InventarioClient {

    private final RestClient restClient;

    public InventarioClient(@Qualifier("inventarioRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Descuenta stock. Si no hay stock suficiente MS-INVENTARIO responde 409.
     */
    public void descontarStock(Long productoId, int cantidad, String motivo) {
        enviarMovimiento("/api/inventario/producto/{productoId}/salida", productoId, cantidad, motivo);
    }

    /**
     * Devuelve stock (se usa al anular una venta o si falla el registro).
     */
    public void devolverStock(Long productoId, int cantidad, String motivo) {
        enviarMovimiento("/api/inventario/producto/{productoId}/entrada", productoId, cantidad, motivo);
    }

    private void enviarMovimiento(String uri, Long productoId, int cantidad, String motivo) {
        try {
            restClient.post()
                    .uri(uri, productoId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new MovimientoDto(cantidad, motivo))
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                        throw new RecursoNoEncontradoException(
                                "El producto " + productoId + " no tiene inventario registrado");
                    })
                    .onStatus(status -> status.value() == HttpStatus.CONFLICT.value(), (request, response) -> {
                        throw new ConflictoException("Stock insuficiente para el producto " + productoId);
                    })
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new ServicioNoDisponibleException("MS-INVENTARIO no está disponible en este momento");
        }
    }
}
