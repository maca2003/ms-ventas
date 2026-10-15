package cl.duoc.msventas.service;

import cl.duoc.msventas.client.InventarioClient;
import cl.duoc.msventas.client.ProductoClient;
import cl.duoc.msventas.client.dto.ProductoDto;
import cl.duoc.msventas.dto.ItemVentaRequest;
import cl.duoc.msventas.dto.VentaRequest;
import cl.duoc.msventas.dto.VentaResponse;
import cl.duoc.msventas.exception.ConflictoException;
import cl.duoc.msventas.model.Venta;
import cl.duoc.msventas.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ProductoClient productoClient;

    @Mock
    private InventarioClient inventarioClient;

    @InjectMocks
    private VentaService ventaService;

    @Test
    void registrarVentaCalculaElTotal() {
        when(productoClient.obtenerProducto(1L)).thenReturn(new ProductoDto(1L, "Leche", new BigDecimal("1200"), true));
        when(productoClient.obtenerProducto(2L)).thenReturn(new ProductoDto(2L, "Pan", new BigDecimal("500"), true));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));

        VentaRequest request = new VentaRequest(List.of(
                new ItemVentaRequest(1L, 2),
                new ItemVentaRequest(2L, 3)));

        VentaResponse response = ventaService.registrar(request, "vendedor1");

        // 2 x 1200 + 3 x 500 = 3900
        assertEquals(0, new BigDecimal("3900").compareTo(response.total()));
        assertEquals(2, response.detalles().size());
    }

    @Test
    void siFaltaStockSeDevuelveLoDescontadoYNoSeGuarda() {
        when(productoClient.obtenerProducto(1L)).thenReturn(new ProductoDto(1L, "Leche", new BigDecimal("1200"), true));
        when(productoClient.obtenerProducto(2L)).thenReturn(new ProductoDto(2L, "Pan", new BigDecimal("500"), true));
        doNothing().when(inventarioClient).descontarStock(eq(1L), eq(2), anyString());
        doThrow(new ConflictoException("Stock insuficiente"))
                .when(inventarioClient).descontarStock(eq(2L), eq(3), anyString());

        VentaRequest request = new VentaRequest(List.of(
                new ItemVentaRequest(1L, 2),
                new ItemVentaRequest(2L, 3)));

        assertThrows(ConflictoException.class, () -> ventaService.registrar(request, "vendedor1"));
        verify(inventarioClient).devolverStock(eq(1L), eq(2), anyString());
        verify(ventaRepository, never()).save(any(Venta.class));
    }
}
