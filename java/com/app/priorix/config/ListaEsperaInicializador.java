package com.app.priorix.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.app.priorix.Repository.AtencionRepository;
import com.app.priorix.Repository.TriajeRepository;
import com.app.priorix.Service.ListaEsperaService;
import com.app.priorix.model.entity.Atencion;
import com.app.priorix.model.entity.Triaje;
import com.app.priorix.model.node.NodoPaciente;

/**
 * La lista de espera vive en memoria, así que se vacía cada vez que la app se
 * reinicia. Este componente la reconstruye al arrancar con los triajes de HOY
 * que todavía no tienen una atención médica asociada.
 *
 * Se insertan del más antiguo al más nuevo, así que dentro de un mismo nivel
 * de prioridad se respeta el orden de llegada.
 */
@Component
public class ListaEsperaInicializador implements ApplicationRunner {

    private final TriajeRepository triajeRepository;
    private final AtencionRepository atencionRepository;
    private final ListaEsperaService listaEspera;

    public ListaEsperaInicializador(TriajeRepository triajeRepository,
                                    AtencionRepository atencionRepository,
                                    ListaEsperaService listaEspera) {
        this.triajeRepository = triajeRepository;
        this.atencionRepository = atencionRepository;
        this.listaEspera = listaEspera;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (listaEspera.obtenerLista() != null) return; // ya tiene datos, no tocar

        // Ids de triajes que ya fueron tomados por un médico (en proceso o atendidos)
        Set<String> yaAtendidos = new HashSet<>();
        for (Atencion a : atencionRepository.findAll()) {
            if (a.getTriaje() != null && a.getTriaje().getId() != null) {
                yaAtendidos.add(a.getTriaje().getId());
            }
        }

        List<Triaje> deHoy = triajeRepository.findByFechaRegistro(LocalDate.now());
        deHoy.sort(Comparator.comparing(
                Triaje::getHoraRegistro,
                Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())));

        int recuperados = 0;
        for (Triaje t : deHoy) {
            if (t.getPrioridad() == null) continue;            // sin nivel, no se puede ordenar
            if (yaAtendidos.contains(t.getId())) continue;     // ya lo tomó un médico
            listaEspera.agregar(new NodoPaciente(t));
            recuperados++;
        }
        System.out.println("Lista de espera reconstruida: " + recuperados + " paciente(s) de hoy.");
    }
}
