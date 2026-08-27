package com.booka.rag;

import com.booka.config.RagProperties;
import com.booka.domain.entity.RagChunk;
import com.booka.domain.entity.RagDocument;
import com.booka.domain.enums.RagAudience;
import com.booka.domain.enums.UserRole;
import com.booka.dto.RagDtos.*;
import com.booka.exception.BusinessException;
import com.booka.repository.RagChunkRepository;
import com.booka.repository.RagDocumentRepository;
import com.booka.security.BookaUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private static final String PUBLIC_SYSTEM_PROMPT = """
            You are a helpful hotel concierge assistant for Booka.
            Answer ONLY using the provided context about hotel policies, amenities, and booking information.
            If the context does not contain the answer, say "I don't have that information. Please contact the front desk."
            Do not invent room availability, prices, or policies.
            For availability questions, tell the guest you can check live inventory if they provide dates.
            Be friendly, concise, and professional.
            """;

    private static final String STAFF_SYSTEM_PROMPT = """
            You are an operational assistant for hotel staff at Booka.
            Answer ONLY using the provided internal context (SOPs, procedures, policies)
            or clearly labeled live property data when present.
            If the context does not contain the answer, say "I don't have that information in the knowledge base."
            Do not invent procedures, blackout dates, occupancy, or guest data.
            Be direct and actionable.
            """;

    private final RagRetrievalService retrievalService;
    private final GroqChatService groqChatService;
    private final RagDocumentRepository ragDocumentRepository;
    private final RagChunkRepository ragChunkRepository;
    private final EmbeddingService embeddingService;
    private final RagRateLimiter rateLimiter;
    private final RagProperties ragProperties;
    private final AvailabilityIntentDetector intentDetector;
    private final AvailabilityToolService availabilityToolService;
    private final StaffPropertyIntentDetector staffPropertyIntentDetector;
    private final StaffPropertyToolService staffPropertyToolService;

    public RagChatResponse publicChat(RagChatRequest request, String clientKey) {
        if (!rateLimiter.tryAcquire("public:" + clientKey, ragProperties.getPublicRateLimitPerMinute())) {
            throw new BusinessException("Rate limit exceeded. Please try again later.");
        }

        String historyText = flattenHistory(request);
        AvailabilityIntentDetector.ParsedAvailabilityQuery parsed =
                intentDetector.detect(request.message(), historyText);

        return switch (parsed.intent()) {
            case CLARIFY_DATES -> availabilityToolService.clarifyDates(parsed.numGuests(), parsed.roomTypeHint());
            case ROOM_TYPES -> availabilityToolService.listRoomTypes(request.hotelId(), parsed.roomTypeHint());
            case AVAILABILITY -> availabilityToolService.checkAvailability(
                    request.hotelId(),
                    parsed.checkIn(),
                    parsed.checkOut(),
                    parsed.numGuests(),
                    parsed.sampleDates(),
                    parsed.sampleLabel(),
                    parsed.roomTypeHint());
            case FAQ -> chat(request, Set.of(RagAudience.PUBLIC), PUBLIC_SYSTEM_PROMPT, false);
        };
    }

    public RagChatResponse staffChat(RagChatRequest request, BookaUserDetails user) {
        if (!rateLimiter.tryAcquire("staff:" + user.getId(), ragProperties.getStaffRateLimitPerMinute())) {
            throw new BusinessException("Rate limit exceeded. Please try again later.");
        }
        Long hotelId = user.getHotelId() != null ? user.getHotelId() : request.hotelId();
        var req = new RagChatRequest(request.message(), hotelId, request.history());

        StaffPropertyIntentDetector.ParsedStaffQuery staffIntent =
                staffPropertyIntentDetector.detect(req.message(), flattenHistory(req));

        return switch (staffIntent.intent()) {
            case BLACKOUT -> staffPropertyToolService.answerBlackouts(
                    hotelId, staffIntent.month(), staffIntent.roomTypeHint(), staffIntent.sampleLabel());
            case SUMMARY -> staffPropertyToolService.answerSummary(hotelId);
            case CLARIFY_BOOKINGS -> staffPropertyToolService.clarifyBookingsPeriod(
                    hotelId, staffIntent.month(), staffIntent.sampleLabel(), staffIntent.from(), staffIntent.to());
            case BOOKINGS -> staffPropertyToolService.answerBookings(
                    hotelId, staffIntent.from(), staffIntent.to(), staffIntent.sampleLabel());
            case AVAILABILITY -> {
                String historyText = flattenHistory(req);
                AvailabilityIntentDetector.ParsedAvailabilityQuery parsed =
                        intentDetector.detect(req.message(), historyText);
                if (parsed.intent() == AvailabilityIntentDetector.Intent.CLARIFY_DATES) {
                    yield availabilityToolService.clarifyDates(parsed.numGuests(), parsed.roomTypeHint());
                }
                if (parsed.intent() == AvailabilityIntentDetector.Intent.ROOM_TYPES) {
                    yield availabilityToolService.listRoomTypes(hotelId, parsed.roomTypeHint());
                }
                if (parsed.intent() == AvailabilityIntentDetector.Intent.AVAILABILITY) {
                    yield availabilityToolService.checkAvailability(
                            hotelId,
                            parsed.checkIn(),
                            parsed.checkOut(),
                            parsed.numGuests(),
                            parsed.sampleDates(),
                            parsed.sampleLabel(),
                            parsed.roomTypeHint());
                }
                yield availabilityToolService.clarifyDates(2);
            }
            case SOP -> {
                Set<RagAudience> audiences = user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.STAFF
                        ? Set.of(RagAudience.PUBLIC, RagAudience.STAFF)
                        : Set.of(RagAudience.PUBLIC);
                yield chat(req, audiences, STAFF_SYSTEM_PROMPT, true);
            }
        };
    }

    private RagChatResponse chat(RagChatRequest request, Set<RagAudience> audiences, String systemPrompt, boolean staffMode) {
        List<RagRetrievalService.RetrievedChunk> retrieved = retrievalService.retrieve(
                request.message(), audiences, request.hotelId());

        List<RagCitation> citations = retrieved.stream()
                .map(r -> new RagCitation(
                        r.document() != null ? r.document().getTitle() : "Unknown",
                        r.chunk().getSection(),
                        staffMode ? r.chunk().getHotelId() : null))
                .distinct()
                .toList();

        String context = retrieved.stream()
                .map(r -> "[Source: " + (r.document() != null ? r.document().getTitle() : "Doc")
                        + (r.chunk().getSection() != null ? " / " + r.chunk().getSection() : "") + "]\n"
                        + r.chunk().getContent())
                .collect(Collectors.joining("\n\n"));

        if (context.isBlank()) {
            return new RagChatResponse(
                    "I don't have that information. Please contact the front desk for assistance.",
                    List.of(), groqChatService.isConfigured());
        }

        String enrichedSystem = systemPrompt + "\n\nContext:\n" + context;
        List<Map<String, String>> messages = buildMessages(request);

        String answer = groqChatService.chat(enrichedSystem, messages);
        if (answer == null) {
            answer = fallbackAnswer(retrieved);
        }

        return new RagChatResponse(answer, citations, groqChatService.isConfigured());
    }

    private String flattenHistory(RagChatRequest request) {
        if (request.history() == null || request.history().isEmpty()) {
            return "";
        }
        return request.history().stream()
                .map(RagChatMessage::content)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    private String fallbackAnswer(List<RagRetrievalService.RetrievedChunk> retrieved) {
        if (retrieved.isEmpty()) {
            return "I don't have that information. Please contact the front desk for assistance.";
        }
        return retrieved.get(0).chunk().getContent();
    }

    private List<Map<String, String>> buildMessages(RagChatRequest request) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.history() != null) {
            for (RagChatMessage msg : request.history()) {
                if (msg.role() != null && msg.content() != null) {
                    messages.add(Map.of("role", msg.role(), "content", msg.content()));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", request.message()));
        return messages;
    }

    @Transactional
    public RagDocumentResponse ingestDocument(IngestDocumentRequest request) {
        RagDocument document = RagDocument.builder()
                .title(request.title())
                .audience(request.audience())
                .hotelId(request.hotelId())
                .sourceType(request.sourceType())
                .build();
        document = ragDocumentRepository.save(document);

        List<String> chunks = chunkText(request.content());
        for (String chunkContent : chunks) {
            float[] embedding = embeddingService.embed(chunkContent);
            RagChunk chunk = RagChunk.builder()
                    .documentId(document.getId())
                    .content(chunkContent)
                    .section(null)
                    .audience(request.audience())
                    .hotelId(request.hotelId())
                    .embedding(embeddingService.serialize(embedding))
                    .build();
            ragChunkRepository.save(chunk);
        }

        log.info("Ingested document '{}' with {} chunks", request.title(), chunks.size());
        return new RagDocumentResponse(document.getId(), document.getTitle(),
                document.getAudience(), document.getHotelId(), document.getSourceType());
    }

    public List<RagDocumentResponse> listDocuments(RagAudience audience, Long hotelId) {
        return ragDocumentRepository.findByAudienceAndHotelId(audience, hotelId).stream()
                .map(d -> new RagDocumentResponse(d.getId(), d.getTitle(), d.getAudience(), d.getHotelId(), d.getSourceType()))
                .toList();
    }

    private List<String> chunkText(String text) {
        int chunkSize = 500;
        int overlap = 50;
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String normalized = text.trim();
        for (int start = 0; start < normalized.length(); start += chunkSize - overlap) {
            int end = Math.min(start + chunkSize, normalized.length());
            chunks.add(normalized.substring(start, end));
            if (end >= normalized.length()) break;
        }
        return chunks;
    }
}
