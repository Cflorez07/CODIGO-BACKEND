package com.app.priorix.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Mantiene abiertas las conexiones (SSE) de los enfermeros que están en la
 * pantalla de triaje y les entrega las lecturas de signos vitales que llegan
 * desde un equipo (o desde el simulador).
 * Las lecturas se entregan solo al enfermero al que van dirigidas.
 */
@Service
public class LecturaStreamService {

    private final Map<String, List<SseEmitter>> emisores = new ConcurrentHashMap<>();

    public SseEmitter suscribir(String usuario) {
        SseEmitter emisor = new SseEmitter(0L); // sin tiempo límite
        emisores.computeIfAbsent(usuario, k -> new CopyOnWriteArrayList<>()).add(emisor);

        Runnable quitar = () -> {
            List<SseEmitter> lista = emisores.get(usuario);
            if (lista != null) lista.remove(emisor);
        };
        emisor.onCompletion(quitar);
        emisor.onTimeout(quitar);
        emisor.onError(e -> quitar.run());

        try {
            emisor.send(SseEmitter.event().name("conectado").data("ok"));
        } catch (IOException e) {
            quitar.run();
        }
        return emisor;
    }

    /** Devuelve a cuántas pantallas abiertas del enfermero llegó la lectura. */
    public int enviar(String usuario, Map<String, Object> lectura) {
        List<SseEmitter> lista = emisores.get(usuario);
        if (lista == null) return 0;

        int entregados = 0;
        for (SseEmitter emisor : lista) {
            try {
                emisor.send(SseEmitter.event()
                        .name("lectura")
                        .data(lectura, MediaType.APPLICATION_JSON));
                entregados++;
            } catch (IOException | IllegalStateException e) {
                lista.remove(emisor);
            }
        }
        return entregados;
    }
}
