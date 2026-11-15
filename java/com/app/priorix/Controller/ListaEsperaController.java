package com.app.priorix.Controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.app.priorix.Service.ListaEsperaService;
import com.app.priorix.model.node.NodoPaciente;

@Controller
@RequestMapping("/medico")
public class ListaEsperaController {

    @Autowired
    private ListaEsperaService listaEsperaService;

    @GetMapping("/lista-espera")
    public String mostrarLista(Model model) {
        // La lista enlazada se recorre aquí para que la vista reciba TODOS los pacientes.
        // Antes se enviaba solo el primer nodo y th:each mostraba un único paciente.
        List<NodoPaciente> filas = new ArrayList<>();
        NodoPaciente nodo = listaEsperaService.obtenerLista();
        while (nodo != null) {
            filas.add(nodo);
            nodo = nodo.getSiguiente();
        }
        model.addAttribute("lista", filas);
        return "medico/lista-espera";
    }
}
