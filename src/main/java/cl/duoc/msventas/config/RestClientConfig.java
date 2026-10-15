package cl.duoc.msventas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Clientes HTTP para la comunicación REST síncrona con otros microservicios.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient productosRestClient(RestClient.Builder builder,
                                          @Value("${servicios.productos.url}") String url) {
        return builder.clone().baseUrl(url).build();
    }

    @Bean
    public RestClient inventarioRestClient(RestClient.Builder builder,
                                           @Value("${servicios.inventario.url}") String url) {
        return builder.clone().baseUrl(url).build();
    }
}
