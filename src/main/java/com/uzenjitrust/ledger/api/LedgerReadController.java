package com.uzenjitrust.ledger.api;

import com.uzenjitrust.ledger.service.LedgerReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ledger")
@Tag(name = "Ledger Read")
public class LedgerReadController {

    private final LedgerReadService ledgerReadService;

    public LedgerReadController(LedgerReadService ledgerReadService) {
        this.ledgerReadService = ledgerReadService;
    }

    @GetMapping("/journal-entries")
    @Operation(summary = "List journal entries (admin)")
    public ResponseEntity<Page<LedgerReadService.LedgerEntryView>> listJournalEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(ledgerReadService.listJournalEntries(page, size, sortBy, direction));
    }
}
