package si.nejcrebernik.frica.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import si.nejcrebernik.frica.entities.CAParamsEntity;
import si.nejcrebernik.frica.repositories.CAParamsRepository;

import java.util.Optional;

@Controller
@RequestMapping("/ca-params")
public class CAParamsController {

    @Autowired
    private CAParamsRepository caParamsRepository;

    @GetMapping
    public @ResponseBody CAParamsEntity getCurrentParams() {
        Optional<CAParamsEntity> params = caParamsRepository.findFirstByOrderByIdDesc();
        if (params.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return params.get();
    }

    @GetMapping(path = "/{id}")
    public @ResponseBody CAParamsEntity getParamsById(@PathVariable Integer id) {
        Optional<CAParamsEntity> params = caParamsRepository.findById(id);
        if (params.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return params.get();
    }
}
