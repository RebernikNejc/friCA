package si.nejcrebernik.frica.repositories;

import org.springframework.data.repository.CrudRepository;
import si.nejcrebernik.frica.entities.CAParamsEntity;

import java.util.Optional;

public interface CAParamsRepository extends CrudRepository<CAParamsEntity, Integer> {

    Optional<CAParamsEntity> findFirstByOrderByIdDesc();
}
