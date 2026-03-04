package com.example.facesapp.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Bootstraps the Spring ApplicationContext alongside the Jakarta EE CDI context.
 * Spring is used exclusively for Spring Batch job definitions and the
 * {@code @Scheduled} cleanup trigger.
 */
@WebListener
public class SpringContextListener implements ServletContextListener {

    private static AnnotationConfigApplicationContext springContext;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        springContext = new AnnotationConfigApplicationContext(SpringBatchConfig.class);
        sce.getServletContext().setAttribute("springContext", springContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (springContext != null && springContext.isRunning()) {
            springContext.close();
        }
    }

    public static AnnotationConfigApplicationContext getSpringContext() {
        return springContext;
    }
}
