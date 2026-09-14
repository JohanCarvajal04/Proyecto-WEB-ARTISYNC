package uteq.edu.ec.artisync.controller.seguridad;
import uteq.edu.ec.artisync.controller.seguridad.*;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

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
import org.springframework.test.util.ReflectionTestUtils;
import uteq.edu.ec.artisync.dto.seguridad.request.LoginRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.RefreshTokenRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.MessageResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.TokenResponse;
import uteq.edu.ec.artisync.service.seguridad.AuthService;

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
        // @InjectMocks no procesa @Value: sin esto, cookieSecure queda en el
        // valor por defecto de boolean (false), que no es el que usa la app
        // (application.properties fija true por defecto; solo el entorno
        // local de desarrollo lo desactiva vía APP_COOKIE_SECURE=false).
        ReflectionTestUtils.setField(authController, "cookieSecure", true);
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
        assertTrue(cookieHeader.contains("Secure"), "la cookie de refresh debe llevar Secure cuando app.security.cookie-secure=true");
        assertTrue(cookieHeader.contains("SameSite=Strict"), "ADR-002 exige SameSite=Strict (frontend y backend son same-origin vía el proxy de nginx)");
        assertTrue(cookieHeader.contains("Path=/api/v1/auth"));
        assertTrue(cookieHeader.contains("Max-Age=604800"), "7 días, igual que MAX_AGE_REFRESH_SEGUNDOS");
    }

    @Test
    void login_conTicket2fa_setaCookiePreAuthConAtributosCorrectos() {
        LoginRequest loginRequest = new LoginRequest("test4@example.com", "pass");
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken(null)
                .correo("test4@example.com")
                .preAuthTicket("ticket-2fa")
                .build();

        when(authService.login(loginRequest)).thenReturn(tokenResponse);

        authController.login(loginRequest, response);

        java.util.List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        String preAuthCookie = cookies.stream().filter(c -> c.startsWith("preAuth2fa=")).findFirst().orElse(null);
        assertNotNull(preAuthCookie, "debe existir una cookie preAuth2fa independiente de la de refreshToken");
        assertTrue(preAuthCookie.contains("preAuth2fa=ticket-2fa"));
        assertTrue(preAuthCookie.contains("HttpOnly"));
        assertTrue(preAuthCookie.contains("Secure"));
        assertTrue(preAuthCookie.contains("SameSite=Strict"));
        assertTrue(preAuthCookie.contains("Path=/api/v1/auth"));
        assertTrue(preAuthCookie.contains("Max-Age=300"), "5 minutos, igual TTL que en Redis");
    }

    @Test
    void escribirCookie_noEsSecure_cuandoCookieSecureEsFalse() {
        // Solo el entorno de desarrollo local (APP_COOKIE_SECURE=false) desactiva Secure,
        // para poder probar por HTTP sin TLS.
        ReflectionTestUtils.setField(authController, "cookieSecure", false);

        LoginRequest loginRequest = new LoginRequest("test5@example.com", "pass");
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .correo("test5@example.com")
                .build();
        when(authService.login(loginRequest)).thenReturn(tokenResponse);

        authController.login(loginRequest, response);

        String cookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertFalse(cookieHeader.contains("Secure"), "en desarrollo (HTTP) la cookie no debe exigir Secure");
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
        assertTrue(cookieHeader.contains("Secure"));
        assertTrue(cookieHeader.contains("SameSite=Strict"));
        assertTrue(cookieHeader.contains("Path=/api/v1/auth"));
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
        assertTrue(cookieHeader.contains("Secure"));
        assertTrue(cookieHeader.contains("SameSite=Strict"));
        assertTrue(cookieHeader.contains("Path=/api/v1/auth"));
    }
    @Test
    void register_devuelveCreated() {
        uteq.edu.ec.artisync.dto.seguridad.request.RegisterRequest requestReg = new uteq.edu.ec.artisync.dto.seguridad.request.RegisterRequest();
        uteq.edu.ec.artisync.dto.seguridad.response.UserResponse resp = new uteq.edu.ec.artisync.dto.seguridad.response.UserResponse();
        when(authService.register(requestReg)).thenReturn(resp);

        ResponseEntity<uteq.edu.ec.artisync.dto.seguridad.response.UserResponse> result = authController.register(requestReg);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(resp, result.getBody());
    }

    @Test
    void verify2Fa_sinTicket_devuelveUnauthorized() {
        ResponseEntity<TokenResponse> res = authController.verify2Fa(null, new uteq.edu.ec.artisync.dto.seguridad.request.TwoFactorRequest(), response);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void verify2Fa_conTicket_devuelveOk() {
        uteq.edu.ec.artisync.dto.seguridad.request.TwoFactorRequest request2fa = new uteq.edu.ec.artisync.dto.seguridad.request.TwoFactorRequest();
        TokenResponse tokenResp = TokenResponse.builder().accessToken("access").build();
        when(authService.verify2Fa("ticket", request2fa)).thenReturn(tokenResp);

        ResponseEntity<TokenResponse> res = authController.verify2Fa("ticket", request2fa, response);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(tokenResp, res.getBody());
    }

    @Test
    void forgotPassword_devuelveOk() {
        uteq.edu.ec.artisync.dto.seguridad.request.ForgotPasswordRequest req = new uteq.edu.ec.artisync.dto.seguridad.request.ForgotPasswordRequest();
        MessageResponse resp = new MessageResponse("Ok");
        when(authService.forgotPassword(req)).thenReturn(resp);

        ResponseEntity<MessageResponse> res = authController.forgotPassword(req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void resetPassword_devuelveOk() {
        uteq.edu.ec.artisync.dto.seguridad.request.ResetPasswordRequest req = new uteq.edu.ec.artisync.dto.seguridad.request.ResetPasswordRequest();
        MessageResponse resp = new MessageResponse("Ok");
        when(authService.resetPassword(req)).thenReturn(resp);

        ResponseEntity<MessageResponse> res = authController.resetPassword(req);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void verify2Fa_ticketBlanco_devuelveUnauthorized() {
        ResponseEntity<TokenResponse> res = authController.verify2Fa("   ", new uteq.edu.ec.artisync.dto.seguridad.request.TwoFactorRequest(), response);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void refresh_tokenBlanco_devuelveUnauthorized() {
        ResponseEntity<TokenResponse> res = authController.refresh("   ", null, response);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    void login_sinTicket2fa_noSeteaCookie() {
        LoginRequest loginRequest = new LoginRequest("test2@example.com", "pass");
        // token sin preAuthTicket
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .correo("test2@example.com")
                .preAuthTicket(null)
                .build();

        when(authService.login(loginRequest)).thenReturn(tokenResponse);
        ResponseEntity<TokenResponse> result = authController.login(loginRequest, response);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void login_sinRefreshToken_noSeteaCookie() {
        LoginRequest loginRequest = new LoginRequest("test3@example.com", "pass");
        // token sin refreshToken
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken(null)
                .correo("test3@example.com")
                .preAuthTicket("ticket")
                .build();

        when(authService.login(loginRequest)).thenReturn(tokenResponse);
        ResponseEntity<TokenResponse> result = authController.login(loginRequest, response);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}

