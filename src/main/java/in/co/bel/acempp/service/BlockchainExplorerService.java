package in.co.bel.acempp.service;

import in.co.bel.acempp.enums.ErrorCode;
import in.co.bel.acempp.exception.AcemException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BlockchainExplorerService {

    private final RestTemplate restTemplate;

    @Value("${blockchain.explorer.url}")
    private String explorerBaseUrl;

    @Value("${blockchain.explorer.username}")
    private String username;

    @Value("${blockchain.explorer.password}")
    private String password;

    @Value("${blockchain.explorer.network}")
    private String network;

    private String token;

    private String login() {
        if (token != null) {
            return token;
        }

        try {
            String loginUrl = explorerBaseUrl + "/auth/login";

            Map<String, String> body = new HashMap<>();
            body.put("user", username);
            body.put("password", password);
            body.put("network", network);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody == null || responseBody.get("token") == null) {
                throw new AcemException(ErrorCode.RESOURCE_NOT_FOUND, "Explorer token not found");
            }

            token = (String) responseBody.get("token");
            return token;

        } catch (HttpClientErrorException.Unauthorized ex) {
            throw new AcemException(ErrorCode.UNAUTHORIZED, "Invalid explorer credentials" + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new AcemException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to login to blockchain explorer" + ex.getMessage(), ex);
        }
    }

    public Object get(String endpoint) {
        try {
            return doGet(login(), endpoint);
        } catch (HttpClientErrorException.Unauthorized ex) {
            token = null;
            try {
                return doGet(login(), endpoint);
            } catch (Exception retryEx) {
                throw new AcemException(ErrorCode.UNAUTHORIZED, "Blockchain explorer authorization failed: " + retryEx.getMessage(), retryEx);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new AcemException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to fetch blockchain explorer data: " + ex.getMessage(), ex);
        }
    }

    private Object doGet(String authToken, String endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
                explorerBaseUrl + endpoint,
                HttpMethod.GET,
                entity,
                Object.class
        );

        return response.getBody();
    }
}