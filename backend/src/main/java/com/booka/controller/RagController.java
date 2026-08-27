package com.booka.controller;

import com.booka.domain.enums.RagAudience;
import com.booka.dto.RagDtos.*;
import com.booka.rag.RagService;
import com.booka.security.BookaUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping("/public/chat")
    RagChatResponse publicChat(
            @Valid @RequestBody RagChatRequest request,
            HttpServletRequest httpRequest) {
        String clientKey = httpRequest.getRemoteAddr();
        return ragService.publicChat(request, clientKey);
    }

    @PostMapping("/chat")
    RagChatResponse staffChat(
            @Valid @RequestBody RagChatRequest request,
            @AuthenticationPrincipal BookaUserDetails user) {
        return ragService.staffChat(request, user);
    }

    @PostMapping("/documents")
    RagDocumentResponse ingestDocument(@Valid @RequestBody IngestDocumentRequest request) {
        return ragService.ingestDocument(request);
    }

    @GetMapping("/documents")
    List<RagDocumentResponse> listDocuments(
            @RequestParam RagAudience audience,
            @RequestParam(required = false) Long hotelId) {
        return ragService.listDocuments(audience, hotelId);
    }
}
