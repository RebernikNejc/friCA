package si.nejcrebernik.frica.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import si.nejcrebernik.frica.entities.CountryEntity;
import si.nejcrebernik.frica.repositories.CountryRepository;

@Controller
@RequestMapping("/country")
public class CountryController {

    @Autowired
    private CountryRepository countryRepository;

    @GetMapping
    public @ResponseBody Iterable<CountryEntity> country() {
        return countryRepository.findAll();
    }
}
