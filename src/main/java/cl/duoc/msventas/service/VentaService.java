package cl.duoc.msventas.service;

import cl.duoc.msventas.client.InventarioClient;
import cl.duoc.msventas.client.ProductoClient;
import cl.duoc.msventas.client.dto.ProductoDto;
import cl.duoc.msventas.dto.DetalleVentaResponse;
import cl.duoc.msventas.dto.ItemVentaRequest;
import cl.duoc.msventas.dto.VentaRequest;
import cl.duoc.msventas.dto.VentaResponse;
import cl.duoc.msventas.exception.ConflictoException;
import cl.duoc.msventas.exception.RecursoNoEncontradoException;
import cl.duoc.msventas.model.DetalleVenta;
import cl.duoc.msventas.model.EstadoVenta;
import cl.duoc.msventas.model.Venta;
import cl.duoc.msventas.repository.VentaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoClient productoClient;
    private final InventarioClient inventarioClient;

    public VentaService(VentaRepository ventaRepository,
                        ProductoClient productoClient,
                        InventarioClient inventarioClient) {
        this.ventaRepository = ventaRepository;
        this.productoClient = productoClient;
        this.inventarioClient = inventarioClient;
    }

    public List<VentaResponse> listar(String vendedor) {
        List<Venta> ventas = (vendedor == null || vendedor.isBlank())
                ? ventaRepository.findAllByOrderByFechaDesc()
                : ventaRepository.findByVendedorOrderByFechaDesc(vendedor);
        return ventas.stream().map(this::aResponse).toList();
    }

    public VentaResponse obtener(Long id) {
        return aResponse(buscar(id));
    }

    /**
     * Registra una venta:
     * 1. Consulta precio y nombre de cada producto en MS-PRODUCTOS.
     * 2. Calcula subtotales y total.
     * 3. Descuenta stock en MS-INVENTARIO (si algo falla, devuelve lo ya descontado).
     * 4. Guarda la venta en la base de datos.
     */
    public VentaResponse registrar(VentaRequest request, String vendedor) {
        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now());
        venta.setVendedor(vendedor);

        for (ItemVentaRequest item : request.items()) {
            ProductoDto producto = productoClient.obtenerProducto(item.productoId());
            if (!producto.activo()) {
                throw new ConflictoException("El producto " + producto.nombre() + " no está activo");
            }
            venta.agregarDetalle(new DetalleVenta(
                    producto.id(), producto.nombre(), item.cantidad(), producto.precio()));
        }
        venta.setTotal(calcularTotal(venta.getDetalles()));

        List<DetalleVenta> descontados = new ArrayList<>();
        try {
            for (DetalleVenta detalle : venta.getDetalles()) {
                inventarioClient.descontarStock(detalle.getProductoId(), detalle.getCantidad(), "Venta");
                descontados.add(detalle);
            }
            return aResponse(ventaRepository.save(venta));
        } catch (RuntimeException ex) {
            devolverStock(descontados, "Reversa por venta fallida");
            throw ex;
        }
    }

    /**
     * Anula una venta registrada y devuelve el stock al inventario.
     */
    public VentaResponse anular(Long id) {
        Venta venta = buscar(id);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new ConflictoException("La venta " + id + " ya está anulada");
        }
        for (DetalleVenta detalle : venta.getDetalles()) {
            inventarioClient.devolverStock(detalle.getProductoId(), detalle.getCantidad(),
                    "Anulación venta " + id);
        }
        venta.setEstado(EstadoVenta.ANULADA);
        return aResponse(ventaRepository.save(venta));
    }

    /**
     * Elimina una venta. Solo se permite si antes fue anulada (para no perder stock).
     */
    public void eliminar(Long id) {
        Venta venta = buscar(id);
        if (venta.getEstado() != EstadoVenta.ANULADA) {
            throw new ConflictoException("Solo se pueden eliminar ventas anuladas. Anule la venta " + id + " primero");
        }
        ventaRepository.delete(venta);
    }

    public BigDecimal calcularTotal(List<DetalleVenta> detalles) {
        return detalles.stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void devolverStock(List<DetalleVenta> detalles, String motivo) {
        for (DetalleVenta detalle : detalles) {
            try {
                inventarioClient.devolverStock(detalle.getProductoId(), detalle.getCantidad(), motivo);
            } catch (RuntimeException ignored) {
                // Se intenta devolver el resto aunque uno falle
            }
        }
    }

    private Venta buscar(Long id) {
        return ventaRepository.findWithDetallesById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta con id " + id + " no encontrada"));
    }

    private VentaResponse aResponse(Venta venta) {
        List<DetalleVentaResponse> detalles = venta.getDetalles().stream()
                .map(d -> new DetalleVentaResponse(
                        d.getProductoId(), d.getNombreProducto(), d.getCantidad(),
                        d.getPrecioUnitario(), d.getSubtotal()))
                .toList();
        return new VentaResponse(venta.getId(), venta.getFecha(), venta.getVendedor(),
                venta.getEstado(), venta.getTotal(), detalles);
    }
}
