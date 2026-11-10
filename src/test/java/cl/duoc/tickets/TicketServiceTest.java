package cl.duoc.tickets;

import cl.duoc.tickets.dto.EstadisticasEvento;
import cl.duoc.tickets.dto.TicketRequest;
import cl.duoc.tickets.dto.TicketUpdateRequest;
import cl.duoc.tickets.model.EstadoTicket;
import cl.duoc.tickets.model.Ticket;
import cl.duoc.tickets.service.EstadisticasService;
import cl.duoc.tickets.service.S3StorageService;
import cl.duoc.tickets.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas sin AWS: el bucket queda vacío, así que solo se usa el EFS (carpeta local).
 * Se ejecutan en el pipeline antes de construir la imagen Docker.
 */
@SpringBootTest(properties = {
        "app.efs.base-path=target/test-efs",
        "app.s3.bucket=",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
})
class TicketServiceTest {

    @Autowired
    TicketService service;

    @Autowired
    EstadisticasService estadisticas;

    private TicketRequest request(String evento, String usuario, EstadoTicket estado, String precio) {
        return new TicketRequest(evento, "Concierto de prueba", null, "Movistar Arena",
                usuario, "Usuario " + usuario, "test@correo.cl", "General", "A-1",
                new BigDecimal(precio), estado);
    }

    @Test
    void generaPdfEnEfsConEstructuraEventoUsuario() throws Exception {
        Ticket t = service.generar(request("evento123", "usuarioX", null, "15000"));

        assertTrue(t.getCodigo().startsWith("ticket"));
        assertEquals(EstadoTicket.RESERVADO, t.getEstado());
        assertTrue(t.getRutaEfs().replace('\\', '/')
                .endsWith("tickets/evento123/usuarioX/" + t.getCodigo() + ".pdf"));
        byte[] pdf = Files.readAllBytes(Paths.get(t.getRutaEfs()));
        assertEquals("%PDF", new String(pdf, 0, 4));
        assertFalse(t.isSubidoS3());
    }

    @Test
    void keyS3SigueElFormatoPedido() {
        assertEquals("evento123/usuarioX/ticket001.pdf",
                S3StorageService.construirKey("evento123", "usuarioX", "ticket001"));
    }

    @Test
    void modificarCambiaCarpetaYDatos() {
        Ticket t = service.generar(request("eventoMod", "u1", null, "10000"));
        String rutaAnterior = t.getRutaEfs();

        Ticket mod = service.actualizar(t.getId(), new TicketUpdateRequest(
                null, null, null, null, "u2", "Otro usuario", null, "VIP", "B-5", null, EstadoTicket.PAGADO));

        assertEquals("u2", mod.getUsuarioId());
        assertEquals("VIP", mod.getTipoEntrada());
        assertEquals(EstadoTicket.PAGADO, mod.getEstado());
        assertTrue(Files.exists(Paths.get(mod.getRutaEfs())));
        assertFalse(Files.exists(Paths.get(rutaAnterior)));
    }

    @Test
    void estadisticasPorEvento() {
        service.generar(request("eventoStats", "a", EstadoTicket.PAGADO, "20000"));
        service.generar(request("eventoStats", "b", EstadoTicket.PAGADO, "20000"));
        service.generar(request("eventoStats", "b", EstadoTicket.RESERVADO, "5000"));
        service.generar(request("eventoStats", "c", EstadoTicket.CANCELADO, "20000"));

        EstadisticasEvento e = estadisticas.porEvento("eventoStats");
        assertEquals(4, e.totalTickets());
        assertEquals(2, e.pagados());
        assertEquals(1, e.reservados());
        assertEquals(1, e.cancelados());
        assertEquals(3, e.reservasActivas());
        assertEquals(0, new BigDecimal("40000").compareTo(e.montoVendido()));
        assertEquals(3, e.usuariosUnicos());
    }

    @Test
    void eliminarBorraElArchivoDelEfs() {
        Ticket t = service.generar(request("eventoDel", "u1", null, "1000"));
        service.eliminar(t.getId());
        assertFalse(Files.exists(Paths.get(t.getRutaEfs())));
    }
}
