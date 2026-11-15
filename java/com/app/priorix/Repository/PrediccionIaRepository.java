package com.app.priorix.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.app.priorix.model.entity.PrediccionIa;

@Repository
public interface PrediccionIaRepository extends MongoRepository<PrediccionIa, String> {

    List<PrediccionIa> findTop100ByOrderByFechaHoraDesc();

    long countByCoincideTrue();

    long countByCoincideFalse();
}
