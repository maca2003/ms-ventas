package cl.duoc.msventas.client.dto;

/**
 * Cuerpo que espera MS-INVENTARIO para registrar una salida o entrada de stock.
 */
public record MovimientoDto(
        Integer cantidad,
        String motivo
) {
}
