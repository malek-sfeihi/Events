package com.eventmanagment.backend.catalog;

import com.eventmanagment.backend.catalog.dto.ProviderCatalogItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class ProviderCatalogController {

    private final ProviderCatalogService providerCatalogService;

    /**
     * Lists approved provider profiles for organizers. Optional {@code eventType} filters by accepted types.
     */
    @GetMapping("/providers")
    public List<ProviderCatalogItem> listProviders(
            @RequestParam(required = false) String eventType,
            Authentication authentication) {
        return providerCatalogService.listForOrganizer(authentication.getName(), eventType);
    }
}
