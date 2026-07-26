package com.itsthevine.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The goodie box and catering tables, as a customer reads them. */
@RestController
public class CateringController {

    private final CateringMenu menu;

    public CateringController(CateringMenu menu) {
        this.menu = menu;
    }

    @GetMapping("/api/catering")
    public CateringMenu.MenuView catering() {
        return menu.menu();
    }
}
