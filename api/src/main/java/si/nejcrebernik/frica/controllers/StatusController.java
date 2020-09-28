package si.nejcrebernik.frica.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import si.nejcrebernik.frica.repositories.StatusRepository;
import si.nejcrebernik.frica.entities.StatusEntity;

@Controller
@RequestMapping(path = "/status")
public class StatusController {

    @Autowired
    private StatusRepository statusRepository;

    @GetMapping
    public @ResponseBody Iterable<StatusEntity> status() {
        return statusRepository.findAll();
    }
}
