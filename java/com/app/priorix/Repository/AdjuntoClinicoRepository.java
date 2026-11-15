package com.app.priorix.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.app.priorix.model.entity.AdjuntoClinico;

@Repository
public interface AdjuntoClinicoRepository extends MongoRepository<AdjuntoClinico, String> {

    List<AdjuntoClinico> findByPacienteId(String pacienteId);

    List<AdjuntoClinico> findByTriajeId(String triajeId);

    List<AdjuntoClinico> findByAtencionId(String atencionId);

    List<AdjuntoClinico> findByHistoriaClinicaId(String historiaClinicaId);
}
