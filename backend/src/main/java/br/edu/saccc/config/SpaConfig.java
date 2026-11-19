package br.edu.saccc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Artefato único (#T4/ADR-002): quando a SPA está embutida em static/,
 * o Spring Boot serve index.html para rotas de navegação da SPA
 * (/app, /login, /cadastro...) — sem isso o React Router recebe 404
 * em refresh/direct-link. Arquivos reais (assets, /api, /ws) intactos.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // location explícita: sem ela o handler /** sombreia o static/ do Spring Boot
        // (recurso não resolvido → 404 → /error → 403 enganoso).
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource solicitado = location.createRelative(resourcePath);
                        // arquivo existe (js/css/imagens)? entrega
                        if (solicitado.exists() && solicitado.isReadable()) {
                            return solicitado;
                        }
                        // rota da SPA (sem extensão) → index.html; API/WS caem fora do handler
                        if (!resourcePath.startsWith("api/") && !resourcePath.startsWith("ws")) {
                            return new ClassPathResource("/static/index.html");
                        }
                        return null;
                    }
                });
    }
}
