package com.app.priorix.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.priorix.Service.ListaEsperaService;
import com.app.priorix.api.dto.ApiDtos.EsperaItem;
import com.app.priorix.model.node.NodoPaciente;

@RestController
@RequestMapping("/api/v1/lista-espera")
public class ApiListaEsperaController {

    private final ListaEsperaService listaEspera;

    public ApiListaEsperaController(ListaEsperaService listaEspera) {
        this.listaEspera = listaEspera;
    }

    /** Fila de espera ordenada por urgencia (nivel 1 primero; a igual nivel, por orden de llegada). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ENFERMERO','MEDICO')")
    public List<EsperaItem> listar() {
        List<EsperaItem> items = new ArrayList<>();
        // Se copia la lista enlazada a una lista normal mientras nadie la modifica
        synchronized (listaEspera) {
            NodoPaciente n = listaEspera.obtenerLista();
            int pos = 1;
            while (n != null) {
                items.add(EsperaItem.from(pos++, n));
                n = n.getSiguiente();
            }
        }
        return items;
    }
}
