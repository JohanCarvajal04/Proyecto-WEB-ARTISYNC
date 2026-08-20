package uteq.edu.ec.artisync.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthRateLimitFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private FilterChain filterChain;

    private AuthRateLimitFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new AuthRateLimitFilter(redisTemplate, null);
        response = new MockHttpServletResponse();
    }

    private MockHttpServletRequest peticionLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/auth/login");
        return request;
    }

    @Test
    void doFilterInternal_ShouldPassThrough_WhenRouteNotCovered() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/catalogo");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void doFilterInternal_ShouldReturn429WithProblemDetail_WhenLimitExceeded() throws ServletException, IOException {
        MockHttpServletRequest request = peticionLogin();
        request.setRemoteAddr("10.0.0.5");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rl:login:10.0.0.5")).thenReturn(11L); // limite login = 10

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertEquals(429, response.getStatus());
        assertTrue(response.getContentAsString().contains("cuota-excedida"));
        assertEquals("application/json;charset=UTF-8", response.getContentType());
    }

    @Test
    void doFilterInternal_ShouldSetRetryAfterHeader() throws ServletException, IOException {
        MockHttpServletRequest request = peticionLogin();
        request.setRemoteAddr("10.0.0.5");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rl:login:10.0.0.5")).thenReturn(11L);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("60", response.getHeader("Retry-After")); // ventana de /login = 60s
    }

    @Test
    void doFilterInternal_ShouldUseIndependentBucketsPerRoute() throws ServletException, IOException {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        MockHttpServletRequest peticionLogin = peticionLogin();
        peticionLogin.setRemoteAddr("10.0.0.5");
        filter.doFilterInternal(peticionLogin, new MockHttpServletResponse(), filterChain);

        MockHttpServletRequest peticion2fa = new MockHttpServletRequest();
        peticion2fa.setMethod("POST");
        peticion2fa.setRequestURI("/api/v1/auth/2fa/verify");
        peticion2fa.setRemoteAddr("10.0.0.5");
        filter.doFilterInternal(peticion2fa, new MockHttpServletResponse(), filterChain);

        verify(valueOperations).increment("rl:login:10.0.0.5");
        verify(valueOperations).increment("rl:2fa:10.0.0.5");
    }

    @Test
    void doFilterInternal_ShouldFailOpen_WhenRedisUnavailable() throws ServletException, IOException {
        MockHttpServletRequest request = peticionLogin();
        request.setRemoteAddr("10.0.0.5");

        DataAccessException fallo = new QueryTimeoutException("Redis no disponible");
        when(redisTemplate.opsForValue()).thenThrow(fallo);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus()); // sin escritura de 429
    }

    @Test
    void doFilterInternal_ShouldUseRemoteAddr_ForBucketKey() throws ServletException, IOException {
        MockHttpServletRequest request = peticionLogin();
        request.setRemoteAddr("203.0.113.9");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rl:login:203.0.113.9")).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        verify(valueOperations).increment("rl:login:203.0.113.9");
        verify(filterChain).doFilter(request, response);
    }
}
