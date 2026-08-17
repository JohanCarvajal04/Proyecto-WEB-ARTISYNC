package uteq.edu.ec.artisync.config;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OBS-08 (A02 OWASP, Bloque C.2): conector HTTPS adicional, activo solo bajo
 * el perfil "medicion", para poder auditar TLS sin apagar el puerto 8080
 * plano que usan el resto de las mediciones (k6, A01, A03, A05, A07). No usar
 * `server.ssl.*` directamente lo haría — esas propiedades reemplazan el único
 * conector del servidor en vez de sumar uno adicional.
 */
@Configuration
@Profile("medicion")
public class TlsMedicionConfig {

    @Value("${app.medicion.tls.port:8443}")
    private int puertoTls;

    @Value("${app.medicion.tls.keystore}")
    private String rutaKeystore;

    @Value("${app.medicion.tls.keystore-password}")
    private String passwordKeystore;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> conectorTlsAdicional() {
        return factory -> {
            Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            connector.setScheme("https");
            connector.setSecure(true);
            connector.setPort(puertoTls);
            connector.setProperty("SSLEnabled", "true");

            SSLHostConfig sslHostConfig = new SSLHostConfig();
            sslHostConfig.setProtocols("TLSv1.3");
            sslHostConfig.setCiphers("TLS_AES_256_GCM_SHA384");

            SSLHostConfigCertificate certificado =
                    new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.UNDEFINED);
            certificado.setCertificateKeystoreFile(rutaKeystore);
            certificado.setCertificateKeystorePassword(passwordKeystore);
            certificado.setCertificateKeystoreType("PKCS12");
            sslHostConfig.addCertificate(certificado);

            connector.addSslHostConfig(sslHostConfig);
            factory.addAdditionalConnectors(connector);
        };
    }
}
