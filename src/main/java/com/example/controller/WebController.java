package com.example.controller;

import com.example.model.Car;
import com.example.model.Coordinates;
import com.example.model.Human;
import com.example.model.Mood;
import com.example.model.WeaponType;
import com.example.service.HumanService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class WebController {

    @Autowired
    private HumanService humanService;

    @GetMapping("/")
    public String index(Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String searchName,
                       @RequestParam(required = false) String searchSoundtrack) {

        Page<Human> humanPage = humanService.findAllWithFilters(searchName, searchSoundtrack, PageRequest.of(page, size));
        
        model.addAttribute("humans", humanPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", humanPage.getTotalPages());
        model.addAttribute("totalElements", humanPage.getTotalElements());
        model.addAttribute("size", size);
        model.addAttribute("searchName", searchName);
        model.addAttribute("searchSoundtrack", searchSoundtrack);
        
        return "index";
    }

    @GetMapping("/human/{id}")
    public String viewHuman(@PathVariable Long id, Model model) {
        Optional<Human> humanOpt = humanService.findById(id);
        if (humanOpt.isPresent()) {
            model.addAttribute("humanBeing", humanOpt.get());
            return "view-human-being";
        }
        return "redirect:/";
    }

    @GetMapping("/human/new")
    public String newHuman(Model model) {
        Human human = new Human();
        human.setCoordinates(new Coordinates());
        human.setCar(new Car());

        model.addAttribute("human", human);
        model.addAttribute("moods", Mood.values());
        model.addAttribute("weaponTypes", WeaponType.values());
        return "edit-human-being";
    }

    @GetMapping("/human/edit/{id}")
    public String editHuman(@PathVariable Long id, Model model) {
        Optional<Human> humanOpt = humanService.findById(id);
        if (humanOpt.isPresent()) {
            Human human = humanOpt.get();
            if (human.getCar() == null) {
                human.setCar(new Car());
            }
            model.addAttribute("human", human);
            model.addAttribute("moods", Mood.values());
            model.addAttribute("weaponTypes", WeaponType.values());
            return "edit-human-being";
        }
        return "redirect:/";
    }

    @PostMapping("/human/save")
    public String saveHuman(@Valid @ModelAttribute("human") Human human, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("moods", Mood.values());
            model.addAttribute("weaponTypes", WeaponType.values());
            return "edit-human-being";
        }
        
        if (human.getCar() != null && (human.getCar().getName() == null || human.getCar().getName().isBlank())) {
            human.setCar(null);
        }
        
        if (human.getId() == null) {
            human.setCreationDate(LocalDate.now());
        }

        humanService.save(human);
        return "redirect:/";
    }

    @PostMapping("/human/delete/{id}")
    public String deleteHuman(@PathVariable Long id) {
        humanService.deleteById(id);
        return "redirect:/";
    }

    @GetMapping("/special-operations")
    public String specialOperations(Model model) {
        model.addAttribute("weaponTypes", WeaponType.values());
        return "special-operations";
    }

    @PostMapping("/special-operations/delete-by-weapon-type")
    public String deleteByWeaponType(@RequestParam WeaponType weaponType, RedirectAttributes redirectAttributes) {
        humanService.deleteAllByWeaponType(weaponType);
        redirectAttributes.addAttribute("success", "deleted");
        return "redirect:/special-operations";
    }

    @GetMapping("/special-operations/max-mood")
    public String getMaxMood(Model model) {
        Optional<Human> human = humanService.findOneWithMaxMood();
        human.ifPresent(value -> model.addAttribute("maxMoodHumanBeing", value));
        return "max-mood-result";
    }

    @PostMapping("/special-operations/waiting-greater-than")
    public String getByMinutesOfWaitingGreaterThan(@RequestParam float minutes, Model model) {
        List<Human> humans = humanService.findByMinutesOfWaitingGreaterThan(minutes);
        model.addAttribute("humanBeings", humans);
        model.addAttribute("minutes", minutes);
        return "waiting-greater-than-result";
    }

    @PostMapping("/special-operations/update-mood-to-gloom")
    public String updateAllHeroesMoodToGloom(RedirectAttributes redirectAttributes) {
        humanService.updateAllHeroesMoodToGloom();
        redirectAttributes.addAttribute("success", "mood-updated");
        return "redirect:/special-operations";
    }

    @PostMapping("/special-operations/assign-red-lada-kalina")
    public String updateAllHeroesWithoutCarToRedLadaKalina(RedirectAttributes redirectAttributes) {
        humanService.updateAllHeroesWithoutCarToRedLadaKalina();
        redirectAttributes.addAttribute("success", "car-assigned");
        return "redirect:/special-operations";
    }
}