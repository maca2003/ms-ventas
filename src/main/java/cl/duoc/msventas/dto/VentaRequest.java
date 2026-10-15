package cl.duoc.msventas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record VentaRequest(
        @NotEmpty(message = "La venta debe tener al menos un producto")
        List<@Valid ItemVentaRequest> items
) {
}
