package cl.duoc.msventas.repository;

import cl.duoc.msventas.model.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // Carga la venta junto con sus detalles en una sola consulta
    @EntityGraph(attributePaths = "detalles")
    Optional<Venta> findWithDetallesById(Long id);

    // Historial de ventas, de la más reciente a la más antigua
    @EntityGraph(attributePaths = "detalles")
    List<Venta> findAllByOrderByFechaDesc();

    @EntityGraph(attributePaths = "detalles")
    List<Venta> findByVendedorOrderByFechaDesc(String vendedor);
}
