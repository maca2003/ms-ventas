package cl.duoc.msventas.dto;

import cl.duoc.msventas.model.EstadoVenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(
        Long id,
        LocalDateTime fecha,
        String vendedor,
        EstadoVenta estado,
        BigDecimal total,
        List<DetalleVentaResponse> detalles
) {
}
