package org.springframework.extra.web.handler;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.extra.web.handler.annotation.ExternalMethodExceptionHandlers;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerMethodExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

public class ExternalHandlerMethodExceptionResolver extends
		AbstractHandlerMethodExceptionResolver implements InitializingBean,
		ApplicationContextAware {

	private ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver;
	private List<ExternalMethodExceptionHandler> externalMethodExceptionHandlers;
	private ApplicationContext applicationContext;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.exceptionHandlerExceptionResolver = this.applicationContext
				.getBean(ExceptionHandlerExceptionResolver.class);
		Assert.notNull(this.exceptionHandlerExceptionResolver, String.format(
				"No registered bean of type %s",
				ExceptionHandlerExceptionResolver.class));

		final Map<String, ExternalMethodExceptionHandler> externalMethodExceptionHandlersMap = this.applicationContext
				.getBeansOfType(ExternalMethodExceptionHandler.class);
		if (!isEmpty(externalMethodExceptionHandlersMap)) {
			this.externalMethodExceptionHandlers = new ArrayList<ExternalMethodExceptionHandler>(
					externalMethodExceptionHandlersMap.values());
		}
	}

	@Override
	public void setApplicationContext(
			final ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public int getOrder() {
		return exceptionHandlerExceptionResolver.getOrder() + 1;
	}

	@Override
	protected ModelAndView doResolveHandlerMethodException(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final HandlerMethod handlerMethod, final Exception ex) {

		ModelAndView resultView = null;
		if (!isEmpty(this.externalMethodExceptionHandlers)) {
			resultView = resolveControllerException(request, response,
					handlerMethod, ex);
		}
		return resultView;
	}

	@Override
	protected boolean shouldApplyTo(final HttpServletRequest request,
			final Object handler) {
		return handler != null;
	}

	private ModelAndView resolveControllerException(
			final HttpServletRequest request,
			final HttpServletResponse response,
			final HandlerMethod handlerMethod, final Exception ex) {
		ModelAndView resultView = null;
		ExternalMethodExceptionHandlers annotation = handlerMethod.getMethod()
				.getAnnotation(ExternalMethodExceptionHandlers.class);
		if (annotation == null) {
			annotation = handlerMethod.getBeanType().getAnnotation(
					ExternalMethodExceptionHandlers.class);
		}
		if (annotation != null) {
			final ExceptionHandlersGroupComposite handlersGroupComposite = groupHandlers(
					this.externalMethodExceptionHandlers, annotation);
			resultView = tryToResolveException(
					handlersGroupComposite.getControllerHandlers(), request,
					response, ex);
			if (resultView == null && annotation.applyOthersIfNotFound()) {
				resultView = tryToResolveException(
						handlersGroupComposite.getOtherHandlers(), request,
						response, ex);
			}
		}
		return resultView;
	}

	private ModelAndView tryToResolveException(
			final List<ExternalMethodExceptionHandler> handlers,
			final HttpServletRequest request,
			final HttpServletResponse response, final Exception ex) {
		ModelAndView resultView = null;
		for (final ExternalMethodExceptionHandler handler : handlers) {
			final HandlerMethod handlerMethod = resolveAnyMethod(handler);
			if (handlerMethod != null) {
				resultView = this.exceptionHandlerExceptionResolver
						.resolveException(request, response, handlerMethod, ex);
				if (resultView != null) {
					break;
				}
			}
		}
		return resultView;
	}

	private static ExceptionHandlersGroupComposite groupHandlers(
			final List<ExternalMethodExceptionHandler> allHandlers,
			final ExternalMethodExceptionHandlers annotation) {
		Assert.notEmpty(allHandlers, "List of all handlers must not be null");
		Assert.notNull(annotation, ExternalMethodExceptionHandlers.class
				+ " annotation must not be null");

		final List<ExternalMethodExceptionHandler> controllerHandlers = new ArrayList<ExternalMethodExceptionHandler>();
		final List<ExternalMethodExceptionHandler> otherHandlers = new ArrayList<ExternalMethodExceptionHandler>();

		final Class<? extends ExternalMethodExceptionHandler>[] controllerHandlerClasses = annotation
				.value();
		if (controllerHandlerClasses.length != 0) {
			for (final Class<? extends ExternalMethodExceptionHandler> controllerHandlerClass : controllerHandlerClasses) {
				for (final ExternalMethodExceptionHandler handler : allHandlers) {
					if (controllerHandlerClass.equals(handler.getClass())) {
						controllerHandlers.add(handler);
					} else {
						otherHandlers.add(handler);
					}
				}
			}
		} else {
			otherHandlers.addAll(allHandlers);
		}
		return new ExceptionHandlersGroupComposite(controllerHandlers,
				otherHandlers);
	}

	private static HandlerMethod resolveAnyMethod(
			final ExternalMethodExceptionHandler exceptionHandler) {

		HandlerMethod handlerMethod = null;
		final Iterator<Method> methodsIterator = filterMethods(exceptionHandler
				.getClass().getMethods());
		if (methodsIterator != null && methodsIterator.hasNext()) {
			handlerMethod = new HandlerMethod(exceptionHandler,
					methodsIterator.next());
		}
		return handlerMethod;
	}

	private static Iterator<Method> filterMethods(final Method[] methods) {
		final List<Method> filteredMethods = new LinkedList<Method>();
		for (final Method method : methods) {
			if (!ReflectionUtils.isObjectMethod(method)
					&& Modifier.isPublic(method.getModifiers())) {
				filteredMethods.add(method);
			}
		}

		return filteredMethods.iterator();
	}

	private static final class ExceptionHandlersGroupComposite {
		private final List<ExternalMethodExceptionHandler> controllerHandlers;
		private final List<ExternalMethodExceptionHandler> otherHandlers;

		public ExceptionHandlersGroupComposite(
				final List<ExternalMethodExceptionHandler> controllerHandlers,
				final List<ExternalMethodExceptionHandler> otherHandlers) {
			this.controllerHandlers = controllerHandlers;
			this.otherHandlers = otherHandlers;
		}

		public List<ExternalMethodExceptionHandler> getControllerHandlers() {
			return controllerHandlers;
		}

		public List<ExternalMethodExceptionHandler> getOtherHandlers() {
			return otherHandlers;
		}
	}
}
