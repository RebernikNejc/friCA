package si.nejcrebernik.frica.repositories;

import org.springframework.data.repository.CrudRepository;
import si.nejcrebernik.frica.entities.StatusEntity;

public interface StatusRepository extends CrudRepository<StatusEntity, Integer> {
}
