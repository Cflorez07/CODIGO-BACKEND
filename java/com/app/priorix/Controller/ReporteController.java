package com.app.priorix.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.priorix.Service.PdfService;
import com.app.priorix.Service.ReporteService;

@Controller
@RequestMapping("/admin/reportes")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private PdfService pdfService;

    @GetMapping
    public String verReportes(Model model) {
        return cargar(null, null, null, null, model);
    }

    @GetMapping("/filtrar")
    public String filtrarReportes(@RequestParam(required = false) String fechaInicio,
                                  @RequestParam(required = false) String fechaFin,
                                  @RequestParam(required = false) String nivel,
                                  @RequestParam(required = false) String medico,
                                  Model model) {
        return cargar(fechaInicio, fechaFin, nivel, medico, model);
    }

    @GetMapping("/descargar-pdf")
    public ResponseEntity<byte[]> descargarPdf(@RequestParam(required = false) String fechaInicio,
                                               @RequestParam(required = false) String fechaFin,
                                               @RequestParam(required = false) String nivel,
                                               @RequestParam(required = false) String medico) {
        List<Map<String, Object>> filas = reporteService.filtrarReportes(fechaInicio, fechaFin, nivel, medico);

        int[] conteos = new int[5];
        long suma = 0;
        int conEspera = 0;
        for (Map<String, Object> r : filas) {
            if (r.get("nivelNumero") instanceof Integer n && n >= 1 && n <= 5) conteos[n - 1]++;
            if (r.get("tiempoEspera") instanceof Long e) { suma += e; conEspera++; }
        }
        Long promedio = conEspera > 0 ? (suma / conEspera) : null;

        byte[] pdfBytes = pdfService.generarReportePdf(filas, fechaInicio, fechaFin, nivel, medico, promedio, conteos);

        String filename = "reporte_priorix_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private String cargar(String fi, String ff, String nivel, String medico, Model model) {
        List<Map<String, Object>> filas = reporteService.filtrarReportes(fi, ff, nivel, medico);

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

        model.addAttribute("reportes", filas);
        model.addAttribute("total", filas.size());
        model.addAttribute("conteos", conteos);
        model.addAttribute("pct", pct);
        model.addAttribute("esperaPromedio", conEspera > 0 ? suma / conEspera : null);
        model.addAttribute("fechaInicio", fi);
        model.addAttribute("fechaFin", ff);
        model.addAttribute("nivel", nivel);
        model.addAttribute("medico", medico);
        return "admin/reportes";
    }
}
