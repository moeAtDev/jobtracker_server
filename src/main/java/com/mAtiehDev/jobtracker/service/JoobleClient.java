package com.mAtiehDev.jobtracker.service;

import com.mAtiehDev.jobtracker.dto.JoobleResponse;
import com.mAtiehDev.jobtracker.dto.JobDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class JoobleClient {

    private final RestTemplate restTemplate;

    @Value("${jooble.api.key}")
    private String apiKey;

   /* public List<JobDTO> searchJobs(String keywords, Integer page) {
        String url = "https://jooble.org/api/" + apiKey;

        Map<String, Object> body = new HashMap<>();
        body.put("keywords", keywords == null ? "" : keywords);
        if (page != null && page > 0) body.put("page", page);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<JoobleResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, JoobleResponse.class
        );

       
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            List<JobDTO> jobs = Optional.ofNullable(response.getBody().getJobs()).orElse(Collections.emptyList());
            jobs.forEach(job -> job.setUrl(resolveFinalUrl(job.getUrl()))); // 🔹 update each job
            return jobs;
        }
        return Collections.emptyList();
    }*/
    
    public List<JobDTO> searchJobs(String keywords, String location, Integer page) {
        String url = "https://jooble.org/api/" + apiKey;

        Map<String, Object> body = new HashMap<>();
        body.put("keywords", keywords == null ? "" : keywords);
        if (location != null && !location.isBlank()) {
            body.put("location", location);
        }
        if (page != null && page > 0) {
            body.put("page", page);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<JoobleResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, JoobleResponse.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            List<JobDTO> jobs = Optional.ofNullable(response.getBody().getJobs()).orElse(Collections.emptyList());
            jobs.forEach(job -> job.setUrl(resolveFinalUrl(job.getUrl())));
            return jobs;
        }
        return Collections.emptyList();
    }

    
    //create url can navigate through to orignal job post -in jooble -
    private String resolveFinalUrl(String joobleLink) {
        if (joobleLink == null || joobleLink.isBlank()) return null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(joobleLink).openConnection();
            conn.setInstanceFollowRedirects(false); // we only want the redirect target
            conn.setRequestMethod("HEAD"); // HEAD is faster than GET
            conn.connect();
            String redirectUrl = conn.getHeaderField("Location");
            conn.disconnect();
            return redirectUrl != null ? redirectUrl : joobleLink;
        } catch (Exception e) {
            return joobleLink; // fallback to original
        }
    }
    
}
