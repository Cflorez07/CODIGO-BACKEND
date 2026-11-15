package com.app.priorix.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.app.priorix.model.entity.IngresoFallecido;

@Repository
public interface IngresoFallecidoRepository extends MongoRepository<IngresoFallecido, String> {

    long countByCodigoStartingWith(String prefijo);
}
