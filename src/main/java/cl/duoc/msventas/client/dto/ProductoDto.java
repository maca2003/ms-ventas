package cl.duoc.msventas.client.dto;

import java.math.BigDecimal;

/**
 * Datos del producto que entrega MS-PRODUCTOS (solo los campos que usa ventas).
 */
public record ProductoDto(
        Long id,
        String nombre,
        BigDecimal precio,
        boolean activo
) {
}
