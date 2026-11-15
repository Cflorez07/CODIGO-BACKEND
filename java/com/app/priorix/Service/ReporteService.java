package com.app.priorix.Service;

import java.util.List;
import java.util.Map;

public interface ReporteService {

    List<Map<String, Object>> generarReporteGeneral();

    // Fechas en formato yyyy-MM-dd; nivel "1" a "5"; medico = parte del usuario. Todos son opcionales.
    List<Map<String, Object>> filtrarReportes(String fechaInicio, String fechaFin, String nivel, String medico);
}
