spring-extra
============

This project contains some not so important missimg features in spring framework

1. Exception Handling
  
    New custom HandlerExceptionResolver impelemntation was added to be able to define exception handler methods in
    different classes(beans) with the ability to define concrete ones in controller classes with the help of new annotation.
		
		EXAMPLE:
     
		@Component
		public class SystemExceptionsHandler implements ExternalMethodExceptionHandler {
		
			@ResponseStatus(value = HttpStatus.BAD_REQUEST)
			@ResponseBody
			@ExceptionHandler({ ClassNotFoundException.class })
			public String handleClassNotFoundException(final Exception excp) {
				return excp.getClass().getName();
			}
		
			@ResponseStatus(value = HttpStatus.BAD_REQUEST)
			@ResponseBody
			@ExceptionHandler({ RuntimeException.class })
			public String handleRuntimeException(final Exception excp) {
				return excp.getClass().getName();
			}
		
			@ResponseStatus(value = HttpStatus.BAD_REQUEST)
			@ResponseBody
			@ExceptionHandler({ MissingServletRequestParameterException.class })
			public String handleMissingServletRequestParameterException(
					final Exception excp) {
				return excp.getClass().getName();
			}
		}

		
		@Controller
		@RequestMapping("/createInstance")
		@ExternalMethodExceptionHandlers({ SystemExceptionsHandler.class })
		public class CreateJavaInstanceController {
		
			@ResponseBody
			@RequestMapping(method = RequestMethod.GET)
			public Object raiseException(
								@RequestParam("className") final String className) throws Exception {
				final Class<?> clazz = Class.forName(className);
				if (Exception.class.isAssignableFrom(clazz)) {
					throw (Exception) clazz.newInstance();
				} else {
					return clazz.newInstance();
				}
			}
		}
