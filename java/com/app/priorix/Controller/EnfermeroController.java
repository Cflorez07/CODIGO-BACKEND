package com.app.priorix.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.priorix.Repository.PacienteRepository;
import com.app.priorix.Service.ListaEsperaService;
import com.app.priorix.model.entity.Triaje;
import com.app.priorix.model.node.NodoPaciente;

@Controller
@RequestMapping("/enfermero")
public class EnfermeroController {

    @Autowired
    private ListaEsperaService listaEspera;

    @Autowired
    private PacienteRepository pacienteRepository;

    // Panel de inicio: resumen de la fila de espera y accesos
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        int[] conteos = new int[5];   // niveles 1 (rojo) a 5 (verde)
        int total = 0;

        NodoPaciente nodo = listaEspera.obtenerLista();
        while (nodo != null) {
            total++;
            Triaje t = nodo.getTriaje();
            if (t != null && t.getNivel() != null) {
                int n = t.getNivel().getNivel();
                if (n >= 1 && n <= 5) conteos[n - 1]++;
            }
            nodo = nodo.getSiguiente();
        }

        model.addAttribute("total", total);
        model.addAttribute("conteos", conteos);
        model.addAttribute("nnPendientes", pacienteRepository.findByTipoDocumento("NN").size());
        return "enfermero/dashboard";
    }
}
