package rw.ac.auca.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rw.ac.auca.fine.dto.PublicFineResponse;
import rw.ac.auca.fine.service.FineService;

import java.util.List;

/** No login needed: anyone can check outstanding fines for a plate number. */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final FineService fineService;

    @GetMapping("/fines")
    public List<PublicFineResponse> outstandingFines(@RequestParam String plate) {
        return fineService.outstandingByPlate(plate);
    }
}
