package com.app.priorix.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.priorix.Repository.PrediccionIaRepository;
import com.app.priorix.model.entity.Paciente;
import com.app.priorix.model.entity.PrediccionIa;
import com.app.priorix.model.entity.Triaje;

@Service
public class PredictorTriajeService {

    @Autowired
    private PrediccionIaRepository prediccionIaRepository;

    public PrediccionIa predecirYRegistrar(Triaje triaje, Paciente paciente, String enfermeroUsername) {
        PrediccionIa pred = new PrediccionIa();
        pred.setEnfermeroUsername(enfermeroUsername);

        if (paciente != null) {
            pred.setPacienteId(paciente.getId());
            pred.setPacienteNombre((paciente.getNombre() + " " + paciente.getApellido()).trim());
            pred.setSexo(paciente.getSexo());
            if (paciente.getFechaNacimiento() != null) {
                pred.setEdad(Period.between(paciente.getFechaNacimiento(), LocalDate.now()).getYears());
            }
        }

        pred.setFrecuenciaCardiaca(triaje.getFrecuenciaCardiaca());
        pred.setFrecuenciaRespiratoria(triaje.getFrecuenciaRespiratoria());
        pred.setPresionArterial(triaje.getPresionArterial());
        pred.setTemperatura(triaje.getTemperatura());
        pred.setSaturacionOxigeno(triaje.getSaturacionOxigeno());
        pred.setEstadoConsciencia(triaje.getEstadoConsciencia());
        pred.setNivelDolor(triaje.getNivelDolor());
        pred.setSintomas(triaje.getSintomas());

        // Motor clínico estandarizado (ESI / Manchester)
        List<String> factores = new ArrayList<>();
        int nivelSugerido = calcularNivelClinico(triaje, factores);

        String nivelNombre;
        String colorHex;
        switch (nivelSugerido) {
            case 1 -> { nivelNombre = "Rojo"; colorHex = "#C62828"; }
            case 2 -> { nivelNombre = "Naranja"; colorHex = "#E65100"; }
            case 3 -> { nivelNombre = "Amarillo"; colorHex = "#F9A825"; }
            case 4 -> { nivelNombre = "Verde"; colorHex = "#2E7D32"; }
            default -> { nivelNombre = "Azul"; colorHex = "#1565C0"; }
        }

        pred.setNivelPredicho(nivelNombre);
        pred.setNivelPredichoNumero(nivelSugerido);
        pred.setColorHex(colorHex);
        pred.setFactores(factores);

        return prediccionIaRepository.save(pred);
    }

    public void registrarConfirmacion(String prediccionId, String nivelAsignado, String enfermeroUsername) {
        if (prediccionId == null || prediccionId.isBlank()) return;
        try {
            PrediccionIa p = prediccionIaRepository.findById(prediccionId).orElse(null);
            if (p != null) {
                p.setNivelAsignado(nivelAsignado);
                if (enfermeroUsername != null) p.setEnfermeroUsername(enfermeroUsername);
                boolean match = p.getNivelPredicho() != null &&
                        p.getNivelPredicho().equalsIgnoreCase(nivelAsignado != null ? nivelAsignado.trim() : "");
                p.setCoincide(match);
                prediccionIaRepository.save(p);
            }
        } catch (Exception e) {
            // No bloqueante
        }
    }

    private int calcularNivelClinico(Triaje t, List<String> factores) {
        String consciencia = t.getEstadoConsciencia() != null ? t.getEstadoConsciencia().trim() : "";
        Integer fc = t.getFrecuenciaCardiaca();
        Integer fr = t.getFrecuenciaRespiratoria();
        Integer spo2 = t.getSaturacionOxigeno();
        Double temp = t.getTemperatura();
        Integer dolor = t.getNivelDolor() != null ? t.getNivelDolor() : 0;
        int[] pa = parsearPresion(t.getPresionArterial());
        int pas = pa[0];
        int pad = pa[1];

        // NIVEL 1 - RESUCITACIÓN (ROJO)
        if (consciencia.equalsIgnoreCase("Inconsciente")) {
            factores.add("Pérdida del estado de alerta (Inconsciente)");
            return 1;
        }
        if (spo2 != null && spo2 < 85) {
            factores.add("Hipoxemia severa crítica (SpO2 " + spo2 + "% < 85%)");
            return 1;
        }
        if (fc != null && (fc < 40 || fc > 150)) {
            factores.add("Frecuencia cardíaca en rango de colapso hemodinámico (" + fc + " lpm)");
            return 1;
        }
        if (fr != null && (fr < 8 || fr > 35)) {
            factores.add("Frecuencia respiratoria en insuficiencia respiratoria severa (" + fr + " rpm)");
            return 1;
        }

        // NIVEL 2 - EMERGENCIA (NARANJA)
        boolean esNivel2 = false;
        if (spo2 != null && spo2 >= 85 && spo2 < 90) {
            factores.add("Hipoxemia moderada que requiere oxigenoterapia inmediata (SpO2 " + spo2 + "%)");
            esNivel2 = true;
        }
        if (temp != null && (temp >= 40.0 || temp < 35.0)) {
            factores.add("Temperatura corporal crítica (" + temp + "°C)");
            esNivel2 = true;
        }
        if (dolor >= 8) {
            factores.add("Dolor severo agudo e incapacitante (" + dolor + "/10)");
            esNivel2 = true;
        }
        if (fc != null && ((fc >= 121 && fc <= 150) || (fc >= 40 && fc <= 49))) {
            factores.add("Taquicardia severa o bradicardia marcada (" + fc + " lpm)");
            esNivel2 = true;
        }
        if (fr != null && fr >= 31 && fr <= 35) {
            factores.add("Taquipnea severa (" + fr + " rpm)");
            esNivel2 = true;
        }
        if (pas > 180 || pad > 110 || (pas > 0 && pas < 90)) {
            factores.add("Presión arterial en rango de alarma hemodinámica (" + t.getPresionArterial() + " mmHg)");
            esNivel2 = true;
        }
        if (consciencia.equalsIgnoreCase("Estuporoso") || consciencia.equalsIgnoreCase("Confuso")) {
            factores.add("Alteración del estado mental (" + consciencia + ")");
            esNivel2 = true;
        }
        if (esNivel2) return 2;

        // NIVEL 3 - URGENCIA (AMARILLO)
        boolean esNivel3 = false;
        if (spo2 != null && spo2 >= 90 && spo2 <= 94) {
            factores.add("Saturación de oxígeno limítrofe (" + spo2 + "%)");
            esNivel3 = true;
        }
        if (temp != null && temp >= 38.5 && temp < 40.0) {
            factores.add("Fiebre alta documentada (" + temp + "°C)");
            esNivel3 = true;
        }
        if (dolor >= 5 && dolor <= 7) {
            factores.add("Dolor moderado a severo (" + dolor + "/10)");
            esNivel3 = true;
        }
        if (fc != null && fc >= 100 && fc <= 120) {
            factores.add("Taquicardia moderada (" + fc + " lpm)");
            esNivel3 = true;
        }
        if (fr != null && fr >= 21 && fr <= 30) {
            factores.add("Taquipnea leve a moderada (" + fr + " rpm)");
            esNivel3 = true;
        }
        if (pas >= 140 || pad >= 90) {
            factores.add("Cifras tensionales elevadas (" + t.getPresionArterial() + " mmHg)");
            esNivel3 = true;
        }
        if (esNivel3) return 3;

        // NIVEL 4 - URGENCIA MENOR (VERDE)
        if (dolor >= 1 && dolor <= 4) {
            factores.add("Dolor leve a tolerable (" + dolor + "/10)");
            return 4;
        }
        if (temp != null && temp >= 37.5 && temp < 38.5) {
            factores.add("Febrícula leve (" + temp + "°C)");
            return 4;
        }

        // NIVEL 5 - SIN URGENCIA (AZUL)
        factores.add("Signos vitales estables y dentro de rangos normales");
        factores.add("Ausencia de dolor agudo relevante");
        return 5;
    }

    private int[] parsearPresion(String pa) {
        if (pa == null || !pa.contains("/")) return new int[]{0, 0};
        try {
            String[] parts = pa.split("/");
            int s = Integer.parseInt(parts[0].trim().replaceAll("[^0-9]", ""));
            int d = Integer.parseInt(parts[1].trim().replaceAll("[^0-9]", ""));
            return new int[]{s, d};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    public Map<String, Object> obtenerMetricasDashboard() {
        Map<String, Object> metricas = new HashMap<>();
        List<PrediccionIa> todas = prediccionIaRepository.findTop100ByOrderByFechaHoraDesc();
        long total = prediccionIaRepository.count();

        long conDecision = todas.stream().filter(p -> p.getNivelAsignado() != null && !p.getNivelAsignado().isBlank()).count();
        long aciertos = todas.stream().filter(p -> Boolean.TRUE.equals(p.getCoincide())).count();
        double precision = conDecision > 0 ? (aciertos * 100.0 / conDecision) : 0.0;

        // Distribución por nivel predicho
        Map<String, Long> distribucion = new LinkedHashMap<>();
        distribucion.put("Rojo", todas.stream().filter(p -> "Rojo".equalsIgnoreCase(p.getNivelPredicho())).count());
        distribucion.put("Naranja", todas.stream().filter(p -> "Naranja".equalsIgnoreCase(p.getNivelPredicho())).count());
        distribucion.put("Amarillo", todas.stream().filter(p -> "Amarillo".equalsIgnoreCase(p.getNivelPredicho())).count());
        distribucion.put("Verde", todas.stream().filter(p -> "Verde".equalsIgnoreCase(p.getNivelPredicho())).count());
        distribucion.put("Azul", todas.stream().filter(p -> "Azul".equalsIgnoreCase(p.getNivelPredicho())).count());

        // Matriz de confusión resumida (Nivel IA vs Nivel Enfermero)
        Map<String, Map<String, Integer>> matriz = new LinkedHashMap<>();
        String[] niveles = {"Rojo", "Naranja", "Amarillo", "Verde", "Azul"};
        for (String ia : niveles) {
            Map<String, Integer> fila = new LinkedHashMap<>();
            for (String enf : niveles) fila.put(enf, 0);
            matriz.put(ia, fila);
        }

        for (PrediccionIa p : todas) {
            if (p.getNivelPredicho() != null && p.getNivelAsignado() != null) {
                String ia = capitalizar(p.getNivelPredicho());
                String enf = capitalizar(p.getNivelAsignado());
                if (matriz.containsKey(ia) && matriz.get(ia).containsKey(enf)) {
                    matriz.get(ia).put(enf, matriz.get(ia).get(enf) + 1);
                }
            }
        }

        metricas.put("totalPredicciones", total);
        metricas.put("evaluadas", conDecision);
        metricas.put("aciertos", aciertos);
        metricas.put("precision", Math.round(precision * 10.0) / 10.0);
        metricas.put("distribucion", distribucion);
        metricas.put("matriz", matriz);
        metricas.put("recientes", todas.stream().limit(15).toList());

        // Honestidad técnica sobre el estado del dataset (requisito 6.4 y 7.2)
        int minRegistrosParaReentrenamiento = 50;
        metricas.put("minimoRequerido", minRegistrosParaReentrenamiento);
        metricas.put("datosSuficientes", total >= minRegistrosParaReentrenamiento);
        metricas.put("faltantes", Math.max(0, minRegistrosParaReentrenamiento - total));

        return metricas;
    }

    private String capitalizar(String s) {
        if (s == null || s.isBlank()) return "";
        String t = s.trim();
        return t.substring(0, 1).toUpperCase() + t.substring(1).toLowerCase();
    }
}
