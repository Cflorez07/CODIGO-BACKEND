package com.app.priorix.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.priorix.Repository.AtencionRepository;
import com.app.priorix.Repository.PacienteRepository;
import com.app.priorix.Repository.UsuarioRepository;
import com.app.priorix.Service.PredictorTriajeService;
import com.app.priorix.Service.ReporteService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private AtencionRepository atencionRepository;
    @Autowired private ReporteService reporteService;
    @Autowired private PredictorTriajeService predictorTriajeService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        model.addAttribute("totalPacientes", pacienteRepository.count());
        model.addAttribute("totalAtenciones", atencionRepository.count());

        try {
            List<Map<String, Object>> filas = reporteService.filtrarReportes(null, null, null, null);
            int[] conteos = new int[5];
            long suma = 0;
            int conEspera = 0;
            for (Map<String, Object> r : filas) {
                if (r.get("nivelNumero") instanceof Integer n && n >= 1 && n <= 5) conteos[n - 1]++;
                if (r.get("tiempoEspera") instanceof Long e) { suma += e; conEspera++; }
            }
            int max = 1;
            for (int c : conteos) max = Math.max(max, c);
            int[] pct = new int[5];
            for (int i = 0; i < 5; i++) pct[i] = conteos[i] * 100 / max;

            model.addAttribute("conteos", conteos);
            model.addAttribute("pct", pct);
            model.addAttribute("esperaPromedio", conEspera > 0 ? suma / conEspera : 0);
            model.addAttribute("casosUrgentes", conteos[0] + conteos[1]);
        } catch (Exception e) {
            model.addAttribute("conteos", new int[]{0,0,0,0,0});
            model.addAttribute("pct", new int[]{0,0,0,0,0});
            model.addAttribute("esperaPromedio", 0);
            model.addAttribute("casosUrgentes", 0);
        }

        try {
            Map<String, Object> iaMetricas = predictorTriajeService.obtenerMetricasDashboard();
            model.addAttribute("ia", iaMetricas);
        } catch (Exception e) {
            model.addAttribute("ia", Map.of("precision", 0, "totalPredicciones", 0, "aciertos", 0));
        }

        return "admin/dashboard";
    }
}

