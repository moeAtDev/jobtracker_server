package com.mAtiehDev.jobtracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import org.springframework.http.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mAtiehDev.jobtracker.dto.JobApplicationDTO;
import com.mAtiehDev.jobtracker.model.GmailToken;
import com.mAtiehDev.jobtracker.model.TrackedApplication;
import com.mAtiehDev.jobtracker.repository.GmailTokenRepository;
import com.mAtiehDev.jobtracker.repository.TrackedApplicationRepository;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.RestClientException;

@Service
public class GmailService {
    private final GmailTokenRepository tokenRepo;
    private final TrackedApplicationRepository appRepo;

    @Value("${gmail.client-id}")
    private String clientId;

    @Value("${gmail.client-secret}")
    private String clientSecret;

    @Value("${gmail.redirect-uri}")
    private String redirectUri;
    
    @Value("${together.api.key}")
    private String togetherApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GmailService(GmailTokenRepository tokenRepo, TrackedApplicationRepository appRepo) {
        this.tokenRepo = tokenRepo;
        this.appRepo = appRepo;
    }

    public List<TrackedApplication> loadOrSyncApps(String userId) {
        List<TrackedApplication> apps = appRepo.findByUserId(userId);
        if (!apps.isEmpty()) return apps;

        GmailToken token = tokenRepo.findById(userId).orElseThrow(() -> new RuntimeException("AUTH_REQUIRED"));
        
        ///here refresh token expired issue 
       /* if (token.getExpiryTime().isBefore(Instant.now())) {
            token = refreshAccessToken(token);
        }*/
        
        try {
            GmailToken refreshedToken = refreshAccessToken(token);
            // continue normally...
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Refresh token expired or revoked")) {
                // Return HTTP 401 Unauthorized with a JSON containing the OAuth URL
                String authUrl = buildAuthUrl(userId);
                return (List<TrackedApplication>) ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authUrl", authUrl));
            } else {
                throw e;
            }
        }

        List<TrackedApplication> newApps = fetchAndParseApplications(token);
        appRepo.saveAll(newApps);
        
        //update lastmodified data in gmail token table
        Optional<GmailToken> optionalToken = tokenRepo.findById(userId);
        if (optionalToken.isPresent()) {
            token.setLastModifiedTime(Instant.now());
            tokenRepo.save(token);
        } else {
            throw new RuntimeException("Token not found for user: " + userId);
        }
        
        
        return newApps;
    }

    public String buildAuthUrl(String userId) {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "scope=https%3A//www.googleapis.com/auth/gmail.readonly&" +
                "access_type=offline&" +
                "include_granted_scopes=true&" +
                "prompt=consent&" +
                "response_type=code&" +
                "redirect_uri=" + redirectUri + "&" +
                "client_id=" + clientId + "&" +
                "state=" + userId;
    }

    public void saveAuthCode(String userId, String code) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        // Use MultiValueMap instead of Map for form-urlencoded content
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> response = restTemplate.postForObject(tokenUrl, request, Map.class);

        GmailToken token = new GmailToken();
        token.setUserId(userId);
        token.setAccessToken((String) response.get("access_token"));
        token.setRefreshToken((String) response.get("refresh_token"));
        token.setExpiryTime(Instant.now().plusSeconds(((Number) response.get("expires_in")).longValue()));
        token.setLastModifiedTime(Instant.now());

        tokenRepo.save(token);
    }

/*
    private GmailToken refreshAccessToken(GmailToken token) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", token.getRefreshToken());
        params.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(tokenUrl, request, Map.class);

            if (response == null || !response.containsKey("access_token") || !response.containsKey("expires_in")) {
                throw new RuntimeException("Invalid token response: " + response);
            }

            String accessToken = (String) response.get("access_token");
            Number expiresIn = (Number) response.get("expires_in");

            token.setAccessToken(accessToken);
            token.setExpiryTime(Instant.now().plusSeconds(expiresIn.longValue()));

            return tokenRepo.save(token);

        } catch (RestClientException | ClassCastException e) {
            throw new RuntimeException("Failed to refresh Gmail access token", e);
        }
    }
*/
    
    @Transactional
    private GmailToken refreshAccessToken(GmailToken token) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", token.getRefreshToken());
        params.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(tokenUrl, request, Map.class);
            System.out.println("response "+response);
            if (response == null || !response.containsKey("access_token") || !response.containsKey("expires_in")) {
                throw new RuntimeException("Invalid token response: " + response);
            }

            String accessToken = (String) response.get("access_token");
            Number expiresIn = (Number) response.get("expires_in");

            token.setAccessToken(accessToken);
            token.setExpiryTime(Instant.now().plusSeconds(expiresIn.longValue()));

            return tokenRepo.save(token);

        }catch (BadRequest ex) {
            String responseBody = ex.getResponseBodyAsString();
            if (responseBody != null && responseBody.contains("\"error\": \"invalid_grant\"")) {
                // Delete the token and stop processing
                tokenRepo.deleteById(token.getUserId());
                throw new RuntimeException("Refresh token expired or revoked. Token deleted from DB.", ex);
            } else {
                throw new RuntimeException("BadRequest during token refresh: " + responseBody, ex);
            }
        } catch (RestClientException | ClassCastException e) {
            throw new RuntimeException("Failed to refresh Gmail access token", e);
        }
    }
    
    

    private List<TrackedApplication> fetchAndParseApplications(GmailToken token) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        //String query = "subject:(applied OR application)";
       // String query = "subject:(application OR applied OR job) OR body:(application OR resume OR cv OR career) OR from:(careers noreply)";
        String query= buildGmailQuery();
        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages?q=" + query;

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        List<Map<String, Object>> messages = (List<Map<String, Object>>) response.getBody().get("messages");
        if (messages == null) return List.of();

        List<TrackedApplication> apps = new ArrayList<>();
        for (Map<String, Object> msg : messages.subList(0, Math.min(messages.size(), 10))) {
            String messageId = (String) msg.get("id");
            ResponseEntity<Map> msgDetail = restTemplate.exchange(
                    "https://gmail.googleapis.com/gmail/v1/users/me/messages/" + messageId,
                    HttpMethod.GET, entity, Map.class);

            Map<String, Object> messageData = msgDetail.getBody();
            System.out.println("here  "+messageData);
            TrackedApplication app = parseMessage(token.getUserId(), messageData);
            if (app != null) apps.add(app);
        }

        return apps;
    }

    private TrackedApplication parseMessage(String userId, Map<String, Object> messageData) {
        Map<String, Object> payload = (Map<String, Object>) messageData.get("payload");
        if (payload == null) return null;

        List<Map<String, String>> headersList = (List<Map<String, String>>) payload.get("headers");
        if (headersList == null) return null;

        String subject = getHeader(headersList, "Subject").orElse("No subject");
        String dateStr = getHeader(headersList, "Date").orElse(null);
        String snippet = (String) messageData.getOrDefault("snippet", "");

        String body = extractEmailBody(payload);
        if (body != null && body.length() > 100) snippet = body.substring(0, 300);

        Instant appliedAt = parseDate(dateStr); // fallback
        Map<String, String> aiExtracted = extractUsingAI(subject, body);

        // Check if AI recommends ignoring this message
        Object ignoreValue = aiExtracted.get("ignore");
        if (ignoreValue instanceof Boolean && (Boolean) ignoreValue) {
            return null; // Skip this message
        }

       

        String company = aiExtracted.getOrDefault("company", "Unknown");
        String jobTitle = aiExtracted.getOrDefault("jobTitle", subject);
        Instant aiAppliedAt = null;
        try {
            if (aiExtracted.containsKey("appliedDate")) {
                aiAppliedAt = LocalDate.parse(aiExtracted.get("appliedDate")).atStartOfDay().toInstant(ZoneOffset.UTC);
            }
        } catch (Exception ignored) {}

        TrackedApplication app = new TrackedApplication();
        app.setUserId(userId);
        app.setJobTitle(jobTitle);
        app.setCompany(company);
        app.setSource("Gmail");
        app.setAppliedAt(aiAppliedAt != null ? aiAppliedAt : (appliedAt != null ? appliedAt : Instant.now()));
        app.setEmailSnippet(snippet);

        return app;
    }


    private Map<String, String> extractUsingAI(String subject, String body) {
        String url = "https://api.together.xyz/v1/chat/completions";

        String prompt = String.format("""
        		You are an AI assistant that tracks job application confirmation emails. ONLY return a result if the email clearly confirms a job application submission. 

        		✅ Examples of confirmation emails:
        		- "Your application was submitted to Amazon."
        		- "We’ve received your application for Software Engineer."
        		- "Thank you for applying to Tree Top Staffing LLC."

        		🚫 Examples of non-confirmations:
        		- "You appeared in 2 searches"
        		- "New jobs posted near you"
        		- "Your job alert from LinkedIn"
        		- "Career news from Indeed"

        		If the email is a job application confirmation, extract and return valid JSON:

        		{
        		  "jobTitle": "...",
        		  "company": "...",
        		  "appliedDate": "YYYY-MM-DD"
        		}

        		If it's NOT a confirmation, return exactly:
        		{ "ignore": true }

        		Input:
        		Subject: %s
        		Body: %s

        		Return JSON only. DO NOT add any explanation or text.
        		""", subject, body);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(togetherApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "mistralai/Mixtral-8x7B-Instruct-v0.1");
        requestBody.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                System.err.println("AI response failed: " + response.getStatusCode());
                return Map.of();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode contentNode = root.path("choices").get(0).path("message").path("content");

            if (contentNode.isMissingNode() || contentNode.isNull()) {
                System.err.println("Missing content from AI response");
                return Map.of();
            }

            String contentText = contentNode.asText().trim();

         // Clean up the response if it includes code fences or extra text
         contentText = contentText
                 .replaceAll("(?i)^```json\\s*", "")  // Remove ```json
                 .replaceAll("^```\\s*", "")          // Remove ```
                 .replaceAll("```$", "")              // Remove trailing ```
                 .trim();

         // Remove function wrappers like Function({...})
         if (contentText.matches("^\\s*\\w+\\s*\\(.*\\)\\s*$")) {
             int start = contentText.indexOf('(');
             int end = contentText.lastIndexOf(')');
             if (start != -1 && end != -1 && start < end) {
                 contentText = contentText.substring(start + 1, end).trim();
             }
         }

         // Now safely parse the cleaned JSON content
         return objectMapper.readValue(contentText, Map.class);


        } catch (Exception e) {
            System.err.println("AI parse failed: " + e.getMessage());
            return Map.of();
        }
    }






    private Optional<String> getHeader(List<Map<String, String>> headers, String name) {
        return headers.stream()
                .filter(h -> name.equalsIgnoreCase(h.get("name")))
                .map(h -> h.get("value"))
                .findFirst();
    }

    private Instant parseDate(String dateStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
            return ZonedDateTime.parse(dateStr, formatter).toInstant();
        } catch (DateTimeParseException e) {
            return Instant.now(); // fallback if parsing fails
        }
    }


   
    
    
    private String extractEmailBody(Map<String, Object> payload) {
        try {
            if (payload.containsKey("parts")) {
                List<Map<String, Object>> parts = (List<Map<String, Object>>) payload.get("parts");
                for (Map<String, Object> part : parts) {
                    if ("text/plain".equals(part.get("mimeType"))) {
                        Map<String, Object> body = (Map<String, Object>) part.get("body");
                        String data = (String) body.get("data");
                        if (data != null) {
                            byte[] decodedBytes = Base64.getUrlDecoder().decode(data);
                            return new String(decodedBytes, StandardCharsets.UTF_8);
                        }
                    }
                    // Recursively check sub-parts (for multipart/alternative etc.)
                    if (part.containsKey("parts")) {
                        String subBody = extractEmailBody(part);
                        if (subBody != null) return subBody;
                    }
                }
            } else if ("text/plain".equals(payload.get("mimeType"))) {
                Map<String, Object> body = (Map<String, Object>) payload.get("body");
                String data = (String) body.get("data");
                if (data != null) {
                    byte[] decodedBytes = Base64.getUrlDecoder().decode(data);
                    return new String(decodedBytes, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to extract email body: " + e.getMessage());
        }
        return null;
    }




    
    public String buildGmailQuery() {
        List<String> keywords = List.of("application", "applied", "job", "position", "resume", "cv");

        // Build query like: (subject:(application) OR body:(application)) OR ...
        String keywordQuery = keywords.stream()
            .map(word -> String.format("(subject:%s OR body:%s)", word, word))
            .collect(Collectors.joining(" OR "));

        // Add common sender patterns
        String fromQuery = "(from:careers OR from:noreply OR from:recruiter)";

        // Gmail expects date as YYYY/MM/DD, not UNIX timestamp in query string
        String dateQuery = "after:" + LocalDate.now().minusDays(7).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        // Final combined query
        return String.format("(%s) OR %s %s", keywordQuery, fromQuery, dateQuery);
    }

    //refresh applications 
    public List<TrackedApplication> refreshApplications(String userId) {
        GmailToken token = tokenRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("AUTH_REQUIRED"));

        ///here refresh token expired issue 
        /*// Refresh token if expired
        if (token.getExpiryTime().isBefore(Instant.now())) {
            token = refreshAccessToken(token);
        }*/
        
        try {
            GmailToken refreshedToken = refreshAccessToken(token);
            // continue normally...
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Refresh token expired or revoked")) {
                // Return HTTP 401 Unauthorized with a JSON containing the OAuth URL
                String authUrl = buildAuthUrl(userId);
                return (List<TrackedApplication>) ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("authUrl", authUrl));
            } else {
                throw e;
            }
        }

        // Add filter based on last modified time
        Instant lastFetched = token.getLastModifiedTime();

        List<TrackedApplication> newApps = fetchAndParseApplicationsSince(token, lastFetched);
        appRepo.saveAll(newApps);

        token.setLastModifiedTime(Instant.now());
        tokenRepo.save(token);

        return newApps;
    }

    private List<TrackedApplication> fetchAndParseApplicationsSince(GmailToken token, Instant sinceTime) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String query = buildGmailQuery() + " after:" + sinceTime.getEpochSecond(); // Gmail uses UNIX timestamp
        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages?q=" + query;

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        List<Map<String, Object>> messages = (List<Map<String, Object>>) response.getBody().get("messages");
        if (messages == null) return List.of();

        List<TrackedApplication> apps = new ArrayList<>();
        for (Map<String, Object> msg : messages.subList(0, Math.min(messages.size(), 10))) {
            String messageId = (String) msg.get("id");
            ResponseEntity<Map> msgDetail = restTemplate.exchange(
                "https://gmail.googleapis.com/gmail/v1/users/me/messages/" + messageId,
                HttpMethod.GET, entity, Map.class);

            Map<String, Object> messageData = msgDetail.getBody();
            TrackedApplication app = parseMessage(token.getUserId(), messageData);
            if (app != null) apps.add(app);
        }

        return apps;
    }

    
   /* private List<TrackedApplication> fetchAndParseApplicationsSince(GmailToken token, Instant sinceTime) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.getAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String query = buildGmailQuery(sinceTime);
        String url = "https://gmail.googleapis.com/gmail/v1/users/me/messages?q=" + UriUtils.encodeQuery(query, StandardCharsets.UTF_8);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        List<Map<String, Object>> messages = (List<Map<String, Object>>) response.getBody().get("messages");
        if (messages == null) return List.of();

        List<TrackedApplication> apps = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            String messageId = (String) msg.get("id");

            ResponseEntity<Map> msgDetail = restTemplate.exchange(
                "https://gmail.googleapis.com/gmail/v1/users/me/messages/" + messageId,
                HttpMethod.GET, entity, Map.class);

            Map<String, Object> messageData = msgDetail.getBody();

            // ✨ Filter by internalDate
            Long internalDateMillis = ((Number) messageData.get("internalDate")).longValue();
            Instant internalDate = Instant.ofEpochMilli(internalDateMillis);
            if (internalDate.isBefore(sinceTime)) continue; // 🔥 skip old emails

            TrackedApplication app = parseMessage(token.getUserId(), messageData);
            if (app != null) apps.add(app);
        }

        return apps;
    }*/

    
    public String buildGmailQuery(Instant sinceTime) {
        List<String> keywords = List.of("application", "applied", "job", "position", "resume", "cv");
        String keywordQuery = keywords.stream()
            .map(word -> String.format("(subject:%s OR body:%s)", word, word))
            .collect(Collectors.joining(" OR "));

        // Optional: tighten sender filter using known job platforms
        String fromQuery = "(from:@linkedin.com OR from:@indeed.com OR from:@greenhouse.io OR from:@workday.com)";

        // Gmail's `after:` supports epoch seconds too
        String dateQuery = "after:" + sinceTime.getEpochSecond();

        return String.format("(%s) OR %s %s", keywordQuery, fromQuery, dateQuery);
    }
    
    
    //save and update data 
    

    @Transactional
    public void updateApplications(List<JobApplicationDTO> updatedApps, String userIdFromToken) {
        for (JobApplicationDTO dto : updatedApps) {

            // New row → save as new
            if (dto.getId() == null) {
                TrackedApplication newApp = new TrackedApplication();
                newApp.setUserId(userIdFromToken);
                newApp.setJobTitle(dto.getJobTitle());
                newApp.setCompany(dto.getCompany());
                newApp.setSource(dto.getSource());
                newApp.setAppliedAt(dto.getAppliedAt());
                newApp.setEmailSnippet(dto.getEmailSnippet());
                appRepo.save(newApp);
            }
            // Edited row → update existing
            else {
            	appRepo.findById(dto.getId()).ifPresent(existing -> {
                    existing.setJobTitle(dto.getJobTitle());
                    existing.setCompany(dto.getCompany());
                    existing.setSource(dto.getSource());
                    existing.setAppliedAt(dto.getAppliedAt());
                    existing.setEmailSnippet(dto.getEmailSnippet());
                    appRepo.save(existing);
                });
            }
        }
    }

   //delete
    @Transactional
    public void deleteApplicationsByIdsAndUser(List<Long> ids, String userId) {
    	appRepo.deleteByIdInAndUserId(ids, userId);
    }
    
    
    //application details services 
    public TrackedApplication getApplicationById(Long id, String userId) {
        return appRepo.findByIdAndUserId(id, userId).orElse(null);
    }

 /*   public void updateSingleApplication(Long id, TrackedApplication updatedApp, String userId) {
        TrackedApplication existing = appRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Not found"));
        existing.setJobTitle(updatedApp.getJobTitle());
        existing.setCompany(updatedApp.getCompany());
        existing.setSource(updatedApp.getSource());
        existing.setAppliedAt(updatedApp.getAppliedAt());
        existing.setEmailSnippet(updatedApp.getEmailSnippet());
        existing.setJobDescription(updatedApp.getJobDescription());
        existing.setResumeText(updatedApp.getResumeText());
        appRepo.save(existing);
    }*/
    
    public void updateSingleApplication(Long id, TrackedApplication updatedApp, String userId) throws Exception {
        TrackedApplication existingApp = appRepo.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new RuntimeException("Application not found"));

        existingApp.setJobTitle(updatedApp.getJobTitle());
        existingApp.setCompany(updatedApp.getCompany());
        existingApp.setSource(updatedApp.getSource());
        existingApp.setAppliedAt(updatedApp.getAppliedAt());
        existingApp.setEmailSnippet(updatedApp.getEmailSnippet());
        existingApp.setJobDescription(updatedApp.getJobDescription());

        if (updatedApp.getResumeText() != null && !updatedApp.getResumeText().isEmpty()) {
            existingApp.setResumeText(updatedApp.getResumeText());
        }

        // Save binary file data if present
        if (updatedApp.getResumeFileData() != null && updatedApp.getResumeFileData().length > 0) {
            existingApp.setResumeFileData(updatedApp.getResumeFileData());
            existingApp.setResumeFileName(updatedApp.getResumeFileName());
            existingApp.setResumeFileType(updatedApp.getResumeFileType());
        }

        appRepo.save(existingApp);
    }



    public void deleteSingleApplication(Long id, String userId) {
    	appRepo.findByIdAndUserId(id, userId)
                .ifPresent(appRepo::delete);
    }


    
  



    

}
