package com.app.priorix.Controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.app.priorix.Service.LecturaStreamService;

/**
 * Recibe lecturas de signos vitales (de un equipo o del simulador) y las
 * empuja en vivo al formulario de triaje del enfermero.
 *
 * Ejemplo de cuerpo JSON:
 * {"frecuenciaCardiaca":96,"frecuenciaRespiratoria":18,
 *  "presionArterial":"120/80","temperatura":37.2,"saturacionOxigeno":97}
 */
@RestController
@RequestMapping("/enfermero/api/lecturas")
public class LecturaController {

    private final LecturaStreamService stream;

    public LecturaController(LecturaStreamService stream) {
        this.stream = stream;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication auth) {
        return stream.suscribir(auth.getName());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> recibir(@RequestBody Map<String, Object> body,
                                                       Authentication auth) {
        Map<String, Object> lectura = validar(body);
        if (lectura.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La lectura no trae valores válidos"));
        }
        int entregados = stream.enviar(auth.getName(), lectura);
        return ResponseEntity.ok(Map.of("entregadoA", entregados, "valores", lectura));
    }

    // Solo deja pasar los campos conocidos y dentro de rangos fisiológicos razonables
    private Map<String, Object> validar(Map<String, Object> b) {
        Map<String, Object> r = new LinkedHashMap<>();
        entero(b, r, "frecuenciaCardiaca", 30, 250);
        entero(b, r, "frecuenciaRespiratoria", 5, 60);
        entero(b, r, "saturacionOxigeno", 50, 100);
        decimal(b, r, "temperatura", 30.0, 45.0);

        Object pa = b.get("presionArterial");
        if (pa != null && pa.toString().matches("^\\d{2,3}/\\d{2,3}$")) {
            r.put("presionArterial", pa.toString());
        }
        return r;
    }

    private void entero(Map<String, Object> b, Map<String, Object> r, String k, int min, int max) {
        Object v = b.get(k);
        if (v == null) return;
        try {
            int n = (int) Math.round(Double.parseDouble(v.toString()));
            if (n >= min && n <= max) r.put(k, n);
        } catch (NumberFormatException ignored) { }
    }

    private void decimal(Map<String, Object> b, Map<String, Object> r, String k, double min, double max) {
        Object v = b.get(k);
        if (v == null) return;
        try {
            double n = Double.parseDouble(v.toString());
            if (n >= min && n <= max) r.put(k, Math.round(n * 10.0) / 10.0);
        } catch (NumberFormatException ignored) { }
    }
}
