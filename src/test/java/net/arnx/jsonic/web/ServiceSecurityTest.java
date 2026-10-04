package net.arnx.jsonic.web;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import net.arnx.jsonic.JSONException;
import net.arnx.jsonic.web.extension.SpringContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.ServletContextAware;

class ServiceSecurityTest {
    public static class Service implements ServletContextAware, BeanNameAware {
        HttpServletRequest request;
        ServletContext context;
        public void setRequest(HttpServletRequest request) { this.request = request; }
        public void setServletContext(ServletContext context) { this.context = context; }
        public void setBeanName(String name) { }
        public String find() { return "data"; }
        public String admin() { return "private"; }
    }

    @Test
    void doesNotExposeInfrastructureOrUnlistedMethods() throws Exception {
        Container container = new Container();
        Service service = new Service();
        assertNull(container.getMethod(service, "setServletContext", Collections.singletonList(null)));
        assertNull(container.getMethod(service, "setRequest", Collections.singletonList(null)));
        container.allowedMethods = Set.of("find");
        assertNotNull(container.getMethod(service, "find", List.of()));
        assertNull(container.getMethod(service, "admin", List.of()));
    }

    @Test
    void refusesRequestInjectionIntoSingletonsAndKeepsPrototypeRequestsSeparate() throws Exception {
        MockServletContext context = new MockServletContext();
        try (StaticWebApplicationContext application = new StaticWebApplicationContext()) {
            application.setServletContext(context);
            application.registerSingleton("shared", Service.class);
            RootBeanDefinition definition = new RootBeanDefinition(Service.class);
            definition.setScope("prototype");
            application.registerBeanDefinition("isolated", definition);
            application.refresh();
            context.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, application);
            MockServletConfig config = new MockServletConfig(context);
            HttpServlet servlet = new HttpServlet() { private static final long serialVersionUID = 1L; };
            servlet.init(config);
            SpringContainer container = new SpringContainer();
            container.init(servlet);
            MockHttpServletRequest first = new MockHttpServletRequest(context);
            MockHttpServletRequest second = new MockHttpServletRequest(context);
            try {
                ExternalContext.start(config, context, first, null);
                assertThrows(IllegalStateException.class, () -> container.getComponent("shared"));
                assertNull(((Service)application.getBean("shared")).request);
                Service a = (Service)container.getComponent("isolated");
                assertNull(container.getMethod(a, "setBeanName", List.of("attacker")));
                assertNull(container.getMethod(a, "setServletContext", Collections.singletonList(null)));
                ExternalContext.start(config, context, second, null);
                Service b = (Service)container.getComponent("isolated");
                assertNotSame(a, b);
                assertSame(first, a.request);
                assertSame(second, b.request);
            } finally { ExternalContext.end(); }
        }
    }

    @Test
    void boundsFormNestingBeforeBuildingOrBindingTheTree() {
        for (String name : List.of("c.".repeat(3000) + "value", "c[".repeat(3000) + "value")) {
            assertThrows(JSONException.class, () -> RESTServlet.RouteMapping.parseParameter(
                    Map.of(name, new String[]{"test"}), new LinkedHashMap<>()));
        }
        Map<Object,Object> params = new LinkedHashMap<>();
        RESTServlet.RouteMapping.parseParameter(Map.of("a.b", new String[]{"test"}), params, 3);
        assertEquals(Map.of("a", Map.of("b", "test")), params);
        assertThrows(JSONException.class, () -> RESTServlet.RouteMapping.parseParameter(
                Map.of("a.b.c", new String[]{"test"}), new LinkedHashMap<>(), 3));
    }
}
