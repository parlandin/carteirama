package com.parlandim.carteirama.export;

import com.parlandim.carteirama.common.security.AuthUser;
import com.parlandim.carteirama.common.time.BusinessClock;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final BusinessClock clock;

    @GetMapping
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal AuthUser user,
                                         @RequestParam List<ExportService.Type> type,
                                         @RequestParam ExportService.Format format,
                                         @RequestParam(required = false) String month) {
        var file = exportService.export(user.requireAccountId(), type, format, clock.parseMonth("month", month));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
