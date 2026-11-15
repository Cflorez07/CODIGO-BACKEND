package com.app.priorix.Service.Impl;


import com.app.priorix.model.entity.HistoriaClinica;
import com.app.priorix.Repository.HistoriaClinicaRepository;
import com.app.priorix.Service.HistoriaClinicaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoriaClinicaServiceImpl implements HistoriaClinicaService {

    @Autowired
    private HistoriaClinicaRepository historiaClinicaRepository;

    @Override
    public HistoriaClinica guardar(HistoriaClinica historiaClinica) {
        return historiaClinicaRepository.save(historiaClinica);
    }

    @Override
    public List<HistoriaClinica> obtenerPorPaciente(String idPaciente) {
        return historiaClinicaRepository.findByPacienteId(idPaciente);
    }
    @Override
    public List<HistoriaClinica> findAll() {
        return historiaClinicaRepository.findAll();
    }

    @Override
    public HistoriaClinica obtenerPorId(String id) {
        if (id == null || id.isBlank()) return null;
        return historiaClinicaRepository.findById(id).orElse(null);
    }

    @Override
    public List<HistoriaClinica> buscar(String q) {
        List<HistoriaClinica> todas = historiaClinicaRepository.findAll();
        if (q == null || q.isBlank()) {
            return todas;
        }
        String term = q.trim().toLowerCase();
        return todas.stream()
                .filter(h -> {
                    boolean matchDiag = h.getDiagnostico() != null && h.getDiagnostico().toLowerCase().contains(term);
                    boolean matchMotivo = h.getMotivoConsulta() != null && h.getMotivoConsulta().toLowerCase().contains(term);
                    boolean matchNivel = h.getNivelTriaje() != null && h.getNivelTriaje().toLowerCase().contains(term);
                    boolean matchPac = false;
                    if (h.getPaciente() != null) {
                        String pacNom = ((h.getPaciente().getNombre() != null ? h.getPaciente().getNombre() : "") + " "
                                + (h.getPaciente().getApellido() != null ? h.getPaciente().getApellido() : "")).toLowerCase();
                        String doc = h.getPaciente().getDocumento() != null ? h.getPaciente().getDocumento().toLowerCase() : "";
                        matchPac = pacNom.contains(term) || doc.contains(term);
                    }
                    return matchDiag || matchMotivo || matchNivel || matchPac;
                })
                .toList();
    }
}
