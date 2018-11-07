package orca.shirako.util;

import orca.shirako.container.Globals;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.FrameworkServlet;

public class SpringWSUtils {
	public static final String SpringContextKey = FrameworkServlet.SERVLET_CONTEXT_PREFIX
			+ "spring-ws";
	public static final Object SpringLock = new Object();

	/**
	 * Obtains the Orca Spring-WS application context.
	 * 
	 * @return Orca Spring-WS application context
	 */
	public static XmlWebApplicationContext getSpringApplicationContext() {
		if (Globals.ServletContext == null) {
			return null;
		}
		return (XmlWebApplicationContext) Globals.ServletContext
				.getAttribute(SpringContextKey);
	}

	/**
	 * Adds a new bean definition to the specified application context.
	 * 
	 * @param context
	 *            spring application context
	 * @param bean
	 *            bean
     * @param name name
	 */
	public static void addBean(XmlWebApplicationContext context, Class<?> bean,
			String name) {
		synchronized (SpringLock) {
			DefaultListableBeanFactory f = (DefaultListableBeanFactory) context
					.getBeanFactory();
			AbstractBeanDefinition def = BeanDefinitionBuilder
					.rootBeanDefinition(bean.getName()).getBeanDefinition();
			f.registerBeanDefinition(name, def);
		}
	}

	public static void registerEndpoint(Class<?> endpoint) {
		synchronized (SpringLock) {
			XmlWebApplicationContext ctx = getSpringApplicationContext();
			if (ctx == null) {
				throw new RuntimeException(
						"Could not obtain the Spring application context");
			}
			Object temp = ctx.getBean("endpointMapper");
			if (temp == null) {
				throw new RuntimeException(
						"No endpointMapper bean is defined: cannot register endpoint");
			}
			if (!(temp instanceof DynamicPayloadRootAnnotationMethodEndpointMapping)) {
				throw new RuntimeException("Invalid endpointMapper class:"
						+ temp.getClass().getName());
			}
			DynamicPayloadRootAnnotationMethodEndpointMapping mapper = (DynamicPayloadRootAnnotationMethodEndpointMapping) temp;
			if (!ctx.containsBeanDefinition(endpoint.getName())) {
				addBean(ctx, endpoint, endpoint.getName());
			}
			mapper.registerEndpoint(endpoint.getName());
		}
	}
}
