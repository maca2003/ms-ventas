package cl.duoc.msventas.dto;

import java.math.BigDecimal;

public record DetalleVentaResponse(
        Long productoId,
        String nombreProducto,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
