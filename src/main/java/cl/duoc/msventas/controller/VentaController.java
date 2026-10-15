package cl.duoc.msventas.controller;

import cl.duoc.msventas.dto.VentaRequest;
import cl.duoc.msventas.dto.VentaResponse;
import cl.duoc.msventas.service.VentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    // Historial de ventas (opcionalmente filtrado por vendedor)
    @GetMapping
    public ResponseEntity<List<VentaResponse>> listar(@RequestParam(required = false) String vendedor) {
        return ResponseEntity.ok(ventaService.listar(vendedor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtener(id));
    }

    // El API Gateway agrega el header X-Usuario con el usuario del token JWT
    @PostMapping
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest request,
                                                   @RequestHeader(value = "X-Usuario", defaultValue = "sin-usuario") String vendedor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(request, vendedor));
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<VentaResponse> anular(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.anular(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ventaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
