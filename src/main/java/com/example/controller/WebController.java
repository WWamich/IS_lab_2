package com.example.controller;

import com.example.model.Human;
import com.example.model.Mood;
import com.example.model.WeaponType;
import com.example.service.HumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@Controller
public class WebController {

    @Autowired
    private HumanService HumanService;

    @GetMapping("/")
    public String index(Model model, 
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String searchName,
                       @RequestParam(required = false) String searchSoundtrack) {
        
        List<Human> Humans;
        long totalElements;
        
        if (searchName != null && !searchName.isEmpty()) {
            Humans = HumanService.findByNameContaining(searchName);
            totalElements = Humans.size();
        } else if (searchSoundtrack != null && !searchSoundtrack.isEmpty()) {
            Humans = HumanService.findBySoundtrackNameContaining(searchSoundtrack);
            totalElements = Humans.size();
        } else {
            Humans = HumanService.findAll(page, size);
            totalElements = HumanService.count();
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Human> HumanPage = new PageImpl<>(Humans, pageable, totalElements);
        
        model.addAttribute("Humans", HumanPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", HumanPage.getTotalPages());
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("size", size);
        model.addAttribute("searchName", searchName);
        model.addAttribute("searchSoundtrack", searchSoundtrack);
        
        return "index";
    }

    @GetMapping("/human/{id}")
    public String viewHuman(@PathVariable Long id, Model model) {
        Optional<Human> Human = HumanService.findById(id);
        if (Human.isPresent()) {
            model.addAttribute("Human", Human.get());
            return "view-human";
        } else {
            return "redirect:/";
        }
    }

    @GetMapping("/human/new")
    public String newHuman(Model model) {
        model.addAttribute("Human", new Human());
        model.addAttribute("moods", Mood.values());
        model.addAttribute("weaponTypes", WeaponType.values());
        return "edit-human";
    }

    @GetMapping("/human/edit/{id}")
    public String editHuman(@PathVariable Long id, Model model) {
        Optional<Human> Human = HumanService.findById(id);
        if (Human.isPresent()) {
            model.addAttribute("Human", Human.get());
            model.addAttribute("moods", Mood.values());
            model.addAttribute("weaponTypes", WeaponType.values());
            return "edit-human";
        } else {
            return "redirect:/";
        }
    }

    @PostMapping("/human/save")
    public String saveHuman(@Valid @ModelAttribute Human Human, 
                                BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("moods", Mood.values());
            model.addAttribute("weaponTypes", WeaponType.values());
            return "edit-human";
        }
        
        HumanService.save(Human);
        return "redirect:/";
    }

    @PostMapping("/human/delete/{id}")
    public String deleteHuman(@PathVariable Long id) {
        HumanService.deleteById(id);
        return "redirect:/";
    }

    @GetMapping("/special-operations")
    public String specialOperations(Model model) {
        model.addAttribute("weaponTypes", WeaponType.values());
        return "special-operations";
    }

    @PostMapping("/special-operations/delete-by-weapon-type")
    public String deleteByWeaponType(@RequestParam WeaponType weaponType) {
        HumanService.deleteAllByWeaponType(weaponType);
        return "redirect:/special-operations?success=deleted";
    }

    @GetMapping("/special-operations/max-mood")
    public String getMaxMood(Model model) {
        Optional<Human> Human = HumanService.findOneWithMaxMood();
        if (Human.isPresent()) {
            model.addAttribute("maxMoodHuman", Human.get());
        }
        return "max-mood-result";
    }

    @PostMapping("/special-operations/waiting-greater-than")
    public String getByMinutesOfWaitingGreaterThan(@RequestParam float minutes, Model model) {
        List<Human> Humans = HumanService.findByMinutesOfWaitingGreaterThan(minutes);
        model.addAttribute("Humans", Humans);
        model.addAttribute("minutes", minutes);
        return "waiting-greater-than-result";
    }

    @PostMapping("/special-operations/update-mood-to-gloom")
    public String updateAllHeroesMoodToGloom() {
        HumanService.updateAllHeroesMoodToGloom();
        return "redirect:/special-operations?success=mood-updated";
    }

    @PostMapping("/special-operations/assign-red-lada-kalina")
    public String updateAllHeroesWithoutCarToRedLadaKalina() {
        HumanService.updateAllHeroesWithoutCarToRedLadaKalina();
        return "redirect:/special-operations?success=car-assigned";
    }
}
