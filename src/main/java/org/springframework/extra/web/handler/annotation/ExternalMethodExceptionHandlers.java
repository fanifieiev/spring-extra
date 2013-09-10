package org.springframework.extra.web.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.extra.web.handler.ExternalMethodExceptionHandler;

@Target({ java.lang.annotation.ElementType.TYPE,
		java.lang.annotation.ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExternalMethodExceptionHandlers {
	Class<? extends ExternalMethodExceptionHandler>[] value();

	boolean applyOthersIfNotFound() default false;
}
