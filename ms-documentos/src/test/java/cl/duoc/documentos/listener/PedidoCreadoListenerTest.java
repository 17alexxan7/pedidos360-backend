package cl.duoc.documentos.listener;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cl.duoc.documentos.exception.MensajeInvalidoException;
import cl.duoc.documentos.messaging.PedidoCreadoEvent;
import cl.duoc.documentos.service.DocumentoService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PedidoCreadoListenerTest {

    private static final long TAG = 7L;

    private DocumentoService service;
    private Channel channel;
    private PedidoCreadoListener listener;
    private PedidoCreadoEvent evento;

    @BeforeEach
    void setUp() {
        service = mock(DocumentoService.class);
        channel = mock(Channel.class);
        listener = new PedidoCreadoListener(service, 3, 1L);
        evento = new PedidoCreadoEvent("evt-1", 1L, "Ana", "Teclado", 1, "PENDIENTE", "2026-10-13T20:00:00");
    }

    @Test
    void exitoHaceAck() throws Exception {
        listener.onPedidoCreado(evento, channel, TAG);

        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(TAG, false, false);
    }

    @Test
    void errorIrrecuperableVaADlqSinReintentos() throws Exception {
        doThrow(new MensajeInvalidoException("invalido")).when(service).enviar(evento);

        listener.onPedidoCreado(evento, channel, TAG);

        verify(service, times(1)).enviar(evento);
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(TAG, false);
    }

    @Test
    void errorRecuperableAgotaReintentosYVaADlq() throws Exception {
        doThrow(new IllegalStateException("caido")).when(service).enviar(evento);

        listener.onPedidoCreado(evento, channel, TAG);

        verify(service, times(3)).enviar(evento);
        verify(channel).basicNack(TAG, false, false);
        verify(channel, never()).basicAck(TAG, false);
    }

    @Test
    void errorRecuperableQueSeRecuperaTerminaEnAck() throws Exception {
        doThrow(new IllegalStateException("caido")).doNothing().when(service).enviar(evento);

        listener.onPedidoCreado(evento, channel, TAG);

        verify(service, times(2)).enviar(evento);
        verify(channel).basicAck(TAG, false);
        verify(channel, never()).basicNack(TAG, false, false);
    }
}