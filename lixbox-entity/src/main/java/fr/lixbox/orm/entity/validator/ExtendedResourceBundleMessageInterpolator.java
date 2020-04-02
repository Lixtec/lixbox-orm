/*******************************************************************************
 *    
 *                           FRAMEWORK Lixbox
 *                          ==================
 *      
 *   Copyrigth - LIXTEC - Tous droits reserves.
 *   
 *   Le contenu de ce fichier est la propriete de la societe Lixtec.
 *   
 *   Toute utilisation de ce fichier et des informations, sous n'importe quelle
 *   forme necessite un accord ecrit explicite des auteurs
 *   
 *   @AUTHOR Ludovic TERRAL
 *
 ******************************************************************************/
package fr.lixbox.orm.entity.validator;

import java.security.AccessController;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.MessageInterpolator;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.internal.util.privilegedactions.GetClassLoader;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;


/**
 * Cette classe ameliore la gestion des erreurs via validator.
 *
 * @author virgile.delacerda
 * @author ludovic.teral
 */
public class ExtendedResourceBundleMessageInterpolator implements MessageInterpolator 
{
    // ----------- Attributs -----------      
	private static final String DEFAULT_VALIDATION_MESSAGES = "org.hibernate.validator.ValidationMessages";
	private static final String USER_VALIDATION_MESSAGES = "ValidationMessages";
	private static final String VALUE_KEY = "actualvalue"; // modified: GRO
	private static final String ROOTBEAN_KEY = "rootBean"; // modified: GRO
	private static final String LEAFBEAN_KEY = "leafBean"; // modified: GRO
	private static final String INVALIDVALUE_KEY = "invalidValue"; // modified: GRO
	private static final Log LOG = LogFactory.getLog(ExtendedResourceBundleMessageInterpolator.class);
	
		

    // ----------- Methodes -----------
	/**
	 * Regular expression used to do message interpolation.
	 */
	private static final Pattern messageParameterPattern = Pattern
			.compile("(\\{[^\\}]+?\\})");

	/**
	 * The default locale for the current user.
	 */
	private final Locale defaultLocale;

	/**
	 * User specified resource bundles hashed against their locale.
	 */
	private final Map<Locale, ResourceBundle> userBundlesMap = new ConcurrentHashMap<>();

	/**
	 * Built-in resource bundles hashed against there locale.
	 */
	private final Map<Locale, ResourceBundle> defaultBundlesMap = new ConcurrentHashMap<>();

	/**
	 * Step 1-3 of message interpolation can be cached. We do this in this map.
	 */
	private final Map<LocalisedMessage, String> resolvedMessages = new WeakHashMap<>();

	public ExtendedResourceBundleMessageInterpolator() {
		this(null);
	}

	public ExtendedResourceBundleMessageInterpolator(ResourceBundle resourceBundle) {

		defaultLocale = Locale.getDefault();

		if (resourceBundle == null) {
			ResourceBundle bundle = getFileBasedResourceBundle(defaultLocale);
			if (bundle != null) {
				userBundlesMap.put(defaultLocale, bundle);
			}

		} else {
			userBundlesMap.put(defaultLocale, resourceBundle);
		}

		defaultBundlesMap.put(defaultLocale, ResourceBundle.getBundle(
				DEFAULT_VALIDATION_MESSAGES, defaultLocale));
	}

	private Map<String, Object> createAttributesMap(Context context) {
		Map<String, Object> map = new HashMap<>();
		map.putAll(context.getConstraintDescriptor().getAttributes());
		map.put(VALUE_KEY, context.getValidatedValue());
		if (context instanceof IExtendedMessageInterpolatorContext) {
			IExtendedMessageInterpolatorContext extendedContext = (IExtendedMessageInterpolatorContext) context;
			map.put(ROOTBEAN_KEY, extendedContext.getRootBean());
			map.put(LEAFBEAN_KEY, extendedContext.getLeafBean());
			map.put(INVALIDVALUE_KEY, extendedContext.getInvalidValue());			
		}
		return map;
	}
	
	public String interpolate(String message, Context context) {
		// probably no need for caching, but it could be done by parameters
		// since the map
		// is immutable and uniquely built per Validation definition, the
		// comparison has to be based on == and not equals though
		return interpolateMessage(message, createAttributesMap(context), defaultLocale);
	}

	public String interpolate(String message, Context context, Locale locale) {
		return interpolateMessage(message, createAttributesMap(context), locale);
	}

	/**
	 * Runs the message interpolation according to algorithm specified in JSR
	 * 303. <br/>
	 * Note: <br/>
	 * Look-ups in user bundles is recursive whereas look-ups in default bundle
	 * are not!
	 * 
	 * @param message
	 *            the message to interpolate
	 * @param parameters
	 *            the parameters of the annotation for which to interpolate this
	 *            message and the validatedValue
	 * @param locale
	 *            the <code>Locale</code> to use for the resource bundle.
	 * 
	 * @return the interpolated message.
	 */
	private String interpolateMessage(String message,
			Map<String, Object> parameters, Locale locale) {
		LocalisedMessage localisedMessage = new LocalisedMessage(message,
				locale);
		String resolvedMessage = resolvedMessages.get(localisedMessage);

		// if the message is not already in the cache we have to run step 1-3 of
		// the message resolution
		if (resolvedMessage == null) {
			ResourceBundle userResourceBundle = findUserResourceBundle(locale);
			ResourceBundle defaultResourceBundle = findDefaultResourceBundle(locale);

			String userBundleResolvedMessage;
			resolvedMessage = message;
			boolean evaluatedDefaultBundleOnce = false;
			do {
				// search the user bundle recursive (step1)
				userBundleResolvedMessage = replaceVariables(resolvedMessage,
						userResourceBundle, locale, true);

				// exit condition - we have at least tried to validate against
				// the default bundle and there was no
				// further replacements
				if (evaluatedDefaultBundleOnce
						&& !hasReplacementTakenPlace(userBundleResolvedMessage,
								resolvedMessage)) {
					break;
				}

				// search the default bundle non recursive (step2)
				resolvedMessage = replaceVariables(userBundleResolvedMessage,
						defaultResourceBundle, locale, false);
				evaluatedDefaultBundleOnce = true;
				resolvedMessages.put(localisedMessage, resolvedMessage);
			} while (true);
		}

		// resolve annotation attributes (step 4)
		resolvedMessage = replaceAnnotationAttributes(resolvedMessage,
				parameters);

		// last but not least we have to take care of escaped literals
		resolvedMessage = resolvedMessage.replace("\\{", "{");
		resolvedMessage = resolvedMessage.replace("\\}", "}");
		resolvedMessage = resolvedMessage.replace("\\\\", "\\");
		return resolvedMessage;
	}

	private boolean hasReplacementTakenPlace(String origMessage,
			String newMessage) {
		return !origMessage.equals(newMessage);
	}

	/**
	 * Search current thread classloader for the resource bundle. If not found,
	 * search validator (this) classloader.
	 * 
	 * @param locale
	 *            The locale of the bundle to load.
	 * 
	 * @return the resource bundle or <code>null</code> if none is found.
	 */
	private ResourceBundle getFileBasedResourceBundle(Locale locale) {
		ResourceBundle rb = null;
		boolean isSecured = System.getSecurityManager() != null;
		GetClassLoader action = GetClassLoader.fromContext();
		ClassLoader classLoader = isSecured ? AccessController
				.doPrivileged(action) : action.run();

		if (classLoader != null) {
			rb = loadBundle(classLoader, locale, USER_VALIDATION_MESSAGES
					+ " not found by thread local classloader");
		}
		if (rb == null) {
			action = GetClassLoader.fromClass(ResourceBundleMessageInterpolator.class);
			classLoader = isSecured ? AccessController.doPrivileged(action)
					: action.run();
			rb = loadBundle(classLoader, locale, USER_VALIDATION_MESSAGES
					+ " not found by validator classloader");
		}
		if (LOG.isDebugEnabled()) {
			if (rb != null) {
				LOG.debug(USER_VALIDATION_MESSAGES + " found");
			} else {
				LOG.debug(USER_VALIDATION_MESSAGES
						+ " not found. Delegating to "
						+ DEFAULT_VALIDATION_MESSAGES);
			}
		}
		return rb;
	}

	private ResourceBundle loadBundle(ClassLoader classLoader, Locale locale,
			String message) {
		ResourceBundle rb = null;
		try {
			rb = ResourceBundle.getBundle(USER_VALIDATION_MESSAGES, locale,
					classLoader);
		} catch (MissingResourceException e) {
			LOG.trace(message);
		}
		return rb;
	}

	private String replaceVariables(String message, ResourceBundle bundle,
			Locale locale, boolean recurse) {
		Matcher matcher = messageParameterPattern.matcher(message);
		StringBuffer sb = new StringBuffer();
		String resolvedParameterValue;
		while (matcher.find()) {
			String parameter = matcher.group(1);
			resolvedParameterValue = resolveParameter(parameter, bundle,
					locale, recurse);

			matcher.appendReplacement(sb,
					escapeMetaCharacters(resolvedParameterValue));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private String replaceAnnotationAttributes(String message,
			Map<String, Object> annotationParameters) {
		Matcher matcher = messageParameterPattern.matcher(message);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String resolvedParameterValue;
			String parameter = matcher.group(1);
			String parameterName = removeCurlyBrace(parameter);
			Object variable = annotationParameters.get(parameterName);
			if (variable != null) {
				resolvedParameterValue = escapeMetaCharacters(variable
						.toString());
			} else {
				if (isExtendedParameter(annotationParameters, parameterName)) {
					try {
						resolvedParameterValue = String.valueOf(PropertyUtils.getProperty(annotationParameters, parameterName));
					} catch (Exception ex) {
						LOG.warn(ex.toString(), ex);
						resolvedParameterValue = parameter;
					}
				}
				else {
					resolvedParameterValue = parameter;
				}
			}
			matcher.appendReplacement(sb, resolvedParameterValue);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private boolean isExtendedParameter(Map<String, Object> annotationParameters, String parameterName) {
		String[] extendedParameterNames = {VALUE_KEY, ROOTBEAN_KEY, LEAFBEAN_KEY, INVALIDVALUE_KEY};
		for (String extendedParameterName : extendedParameterNames) {
			if (parameterName.equals(extendedParameterName) || (parameterName.startsWith(extendedParameterName + "."))) {
				return annotationParameters.containsKey(extendedParameterName);
			}
		}
		return false;
	}

	private String resolveParameter(String parameterName,
			ResourceBundle bundle, Locale locale, boolean recurse) {
		String parameterValue;
		try {
			if (bundle != null) {
				parameterValue = bundle
						.getString(removeCurlyBrace(parameterName));
				if (recurse) {
					parameterValue = replaceVariables(parameterValue, bundle,
							locale, recurse);
				}
			} else {
				parameterValue = parameterName;
			}
		} catch (MissingResourceException e) {
			// return parameter itself
			parameterValue = parameterName;
		}
		return parameterValue;
	}

	private String removeCurlyBrace(String parameter) {
		return parameter.substring(1, parameter.length() - 1);
	}

	private ResourceBundle findDefaultResourceBundle(Locale locale) {
		if (defaultBundlesMap.containsKey(locale)) {
			return defaultBundlesMap.get(locale);
		}

		ResourceBundle bundle = ResourceBundle.getBundle(
				DEFAULT_VALIDATION_MESSAGES, locale);
		defaultBundlesMap.put(locale, bundle);
		return bundle;
	}

	private ResourceBundle findUserResourceBundle(Locale locale) {
		if (userBundlesMap.containsKey(locale)) {
			return userBundlesMap.get(locale);
		}

		ResourceBundle bundle = getFileBasedResourceBundle(locale);
		if (bundle != null) {
			userBundlesMap.put(locale, bundle);
		}
		return bundle;
	}

	/**
	 * @param s
	 *            The string in which to replace the meta characters '$' and
	 *            '\'.
	 * 
	 * @return A string where meta characters relevant for
	 *         {@link Matcher#appendReplacement} are escaped.
	 */
	private String escapeMetaCharacters(String s) {
		String escapedString = s.replace("\\", "\\\\");
		escapedString = escapedString.replace("$", "\\$");
		return escapedString;
	}

	private static class LocalisedMessage {
		private final String message;
		private final Locale locale;

		LocalisedMessage(String message, Locale locale) {
			this.message = message;
			this.locale = locale;
		}

		@SuppressWarnings("unused")
		public String getMessage() {
			return message;
		}

		@SuppressWarnings("unused")
		public Locale getLocale() {
			return locale;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			LocalisedMessage that = (LocalisedMessage) o;

			if (locale != null ? !locale.equals(that.locale) : that.locale != null) 
			{
				return false;
			}
			if (message != null ? !message.equals(that.message)	: that.message != null) 
			{
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = message != null ? message.hashCode() : 0;
			result = 31 * result + (locale != null ? locale.hashCode() : 0);
			return result;
		}
	}
}