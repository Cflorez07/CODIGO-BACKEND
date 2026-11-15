package com.app.priorix.Controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.priorix.Repository.HistoriaClinicaRepository;
import com.app.priorix.Service.AdjuntoService;
import com.app.priorix.Service.PdfService;
import com.app.priorix.model.entity.AdjuntoClinico;
import com.app.priorix.model.entity.HistoriaClinica;

/** Lista y consulta de historias clínicas ya registradas. */
@Controller
@RequestMapping("/medico/historias")
public class HistoriasMedicoController {

    @Autowired
    private HistoriaClinicaRepository historiaRepository;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private AdjuntoService adjuntoService;

    @GetMapping
    public String listar(@RequestParam(value = "q", required = false) String q, Model model) {
        String f = q == null ? "" : q.trim().toLowerCase();
        List<HistoriaClinica> todas = historiaRepository.findAll();
        for (HistoriaClinica h : todas) {
            if (h.getId() == null || h.getId().isBlank()) {
                h.setId(null);
                HistoriaClinica rep = historiaRepository.save(h);
                h.setId(rep.getId());
            }
        }
        List<HistoriaClinica> historias = todas.stream()
                .filter(h -> f.isEmpty() || coincide(h, f))
                .sorted(Comparator.comparing(HistoriaClinica::getFechaAtencion,
                        Comparator.nullsLast(Comparator.<LocalDate>reverseOrder())))
                .toList();
        model.addAttribute("historias", historias);
        model.addAttribute("query", q);
        return "medico/historias";
    }

    @GetMapping({"/ver/{id}", "/ver"})
    public String ver(@PathVariable(required = false) String id, Model model) {
        if (id == null || id.isBlank() || "null".equals(id)) return "redirect:/medico/historias?error=sinId";
        HistoriaClinica h = historiaRepository.findById(id).orElse(null);
        if (h == null) return "redirect:/medico/historias?error=notfound";
        model.addAttribute("h", h);

        List<AdjuntoClinico> adjuntos = new ArrayList<>(adjuntoService.listarPorHistoria(h.getId()));
        if (h.getPaciente() != null && h.getPaciente().getId() != null) {
            List<AdjuntoClinico> pacAdj = adjuntoService.listarPorPaciente(h.getPaciente().getId());
            for (AdjuntoClinico a : pacAdj) {
                if (adjuntos.stream().noneMatch(existing -> existing.getId().equals(a.getId()))) {
                    adjuntos.add(a);
                }
            }
        }
        model.addAttribute("adjuntos", adjuntos);
        return "medico/historia-ver";
    }

    @GetMapping("/ver/")
    public String verConSlashSinId() {
        return "redirect:/medico/historias?error=sinId";
    }

    @GetMapping("/pdf/{id}")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable String id) {
        HistoriaClinica h = historiaRepository.findById(id).orElse(null);
        if (h == null) return ResponseEntity.notFound().build();

        byte[] pdfBytes = pdfService.generarHistoriaClinicaPdf(h);
        String docPac = (h.getPaciente() != null && h.getPaciente().getDocumento() != null)
                ? h.getPaciente().getDocumento() : id;
        String filename = "historia_clinica_" + docPac + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private boolean coincide(HistoriaClinica h, String f) {
        String nombre = (h.getPaciente() != null && h.getPaciente().getNombre() != null) ? h.getPaciente().getNombre() : "";
        String apellido = (h.getPaciente() != null && h.getPaciente().getApellido() != null) ? h.getPaciente().getApellido() : "";
        String paciente = (nombre + " " + apellido).trim().toLowerCase();
        String doc = (h.getPaciente() != null && h.getPaciente().getDocumento() != null)
                ? h.getPaciente().getDocumento().toLowerCase() : "";
        String diag = h.getDiagnostico() == null ? "" : h.getDiagnostico().toLowerCase();
        return paciente.contains(f) || diag.contains(f) || doc.contains(f);
    }
}
