package uteq.edu.ec.artisync.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import uteq.edu.ec.artisync.dto.request.LoginRequest;
import uteq.edu.ec.artisync.dto.request.RefreshTokenRequest;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.TokenResponse;
import uteq.edu.ec.artisync.service.AuthService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthController authController;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
    }

    @Test
    void login_ShouldReturnOkAndSetCookie() {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "pass");
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .correo("test@example.com")
                .build();

        when(authService.login(loginRequest)).thenReturn(tokenResponse);

        ResponseEntity<TokenResponse> result = authController.login(loginRequest, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("access-token", result.getBody().getAccessToken());
        
        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("refreshToken=refresh-token"));
        assertTrue(cookieHeader.contains("HttpOnly"));
    }

    @Test
    void refresh_ShouldReturnUnauthorized_WhenNoCookie() {
        ResponseEntity<TokenResponse> result = authController.refresh(null, null, response);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void refresh_ShouldReturnNewTokensAndSetCookie_WhenCookieExists() {
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .build();

        when(authService.refreshToken("old-refresh")).thenReturn(tokenResponse);

        ResponseEntity<TokenResponse> result = authController.refresh("old-refresh", null, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("new-access", result.getBody().getAccessToken());

        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("refreshToken=new-refresh"));
    }

    @Test
    void refresh_ShouldReturnNewTokens_WhenBodyExistsAndCookieIsNull() {
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .build();

        when(authService.refreshToken("body-refresh")).thenReturn(tokenResponse);

        RefreshTokenRequest req = new RefreshTokenRequest("body-refresh");
        ResponseEntity<TokenResponse> result = authController.refresh(null, req, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("new-access", result.getBody().getAccessToken());
    }

    @Test
    void logout_ShouldReturnNoContentAndClearCookie() {
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(authService.logout("Bearer access-token", "refresh-token")).thenReturn(new MessageResponse("Sesión cerrada exitosamente"));

        ResponseEntity<Void> result = authController.logout(request, "refresh-token", response);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());

        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("Max-Age=0"));
    }
}
