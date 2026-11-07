package cl.duoc.tickets;

import cl.duoc.tickets.service.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba el flujo completo (generar -> EFS -> S3 -> modificar -> descargar -> eliminar -> estadísticas)
 * reemplazando S3 por un "bucket" en memoria, para poder correrla sin AWS.
 */
@SpringBootTest(properties = {
        "app.efs.path=target/efs-test",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "app.s3.auto-upload=true"})
@AutoConfigureMockMvc
class TicketFlujoTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    S3StorageService s3;

    final Map<String, byte[]> bucket = new HashMap<>();

    @BeforeEach
    void bucketEnMemoria() {
        doAnswer(i -> bucket.put(i.getArgument(0), i.getArgument(1))).when(s3).subir(anyString(), any(), anyMap());
        when(s3.descargar(anyString())).thenAnswer(i -> bucket.get((String) i.getArgument(0)));
        doAnswer(i -> bucket.remove((String) i.getArgument(0))).when(s3).eliminar(anyString());
        when(s3.getBucket()).thenReturn("bucket-test");
    }

    @Test
    void flujoCompleto() throws Exception {
        String body = """
                {"eventoId":"evento123","eventoNombre":"Concierto Duoc","fechaEvento":"2026-12-10T20:00:00",
                 "lugar":"Movistar Arena","usuarioId":"usuarioX","usuarioNombre":"Ana Pérez",
                 "usuarioEmail":"ana@test.cl","asiento":"A-10","precio":25000,"estado":"VENDIDO","cantidad":2}
                """;
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].claveS3").value("evento123/usuarioX/ticket001.pdf"))
                .andExpect(jsonPath("$[0].subidoS3").value(true));

        // EFS: el PDF quedó en la carpeta evento/usuario
        Path pdf = Path.of("target/efs-test/tickets/evento123/usuarioX/ticket001.pdf");
        assertThat(Files.exists(pdf)).isTrue();
        assertThat(new String(Files.readAllBytes(pdf), 0, 5)).isEqualTo("%PDF-");
        assertThat(bucket).containsKeys("evento123/usuarioX/ticket001.pdf", "evento123/usuarioX/ticket002.pdf");

        // Modificar: cambia a otro usuario -> se mueve en S3
        mvc.perform(put("/api/tickets/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":\"usuarioY\",\"usuarioNombre\":\"Luis\",\"estado\":\"RESERVADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.claveS3").value("evento123/usuarioY/ticket002.pdf"));
        assertThat(bucket).containsKey("evento123/usuarioY/ticket002.pdf").doesNotContainKey("evento123/usuarioX/ticket002.pdf");

        // Descargar uno y ZIP por evento
        mvc.perform(get("/api/tickets/1/descargar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        mvc.perform(get("/api/tickets/descargar").param("eventoId", "evento123"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("tickets_evento123.zip")));

        // Historial con filtros
        mvc.perform(get("/api/tickets").param("usuarioId", "usuarioY"))
                .andExpect(jsonPath("$.length()").value(1));

        // Estadísticas
        mvc.perform(get("/api/estadisticas/eventos/evento123"))
                .andExpect(jsonPath("$.ticketsVendidos").value(1))
                .andExpect(jsonPath("$.ticketsReservados").value(1))
                .andExpect(jsonPath("$.ingresosPorVentas").value(25000));

        // Eliminar
        mvc.perform(delete("/api/tickets/1")).andExpect(jsonPath("$.estado").value("ELIMINADO"));
        assertThat(Files.exists(pdf)).isFalse();
        assertThat(bucket).doesNotContainKey("evento123/usuarioX/ticket001.pdf");
        mvc.perform(get("/api/tickets/1/descargar")).andExpect(status().isNotFound());
        mvc.perform(get("/api/estadisticas/eventos/evento123"))
                .andExpect(jsonPath("$.ticketsEliminados").value(1));
    }
}
