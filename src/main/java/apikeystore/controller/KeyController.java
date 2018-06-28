package apikeystore.controller;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;

import apikeystore.config.YAMLConfig;
import apikeystore.exception.ATExpiredException;
import apikeystore.exception.AddFailureException;
import apikeystore.exception.BadParameterException;
import apikeystore.exception.DoesNotExistException;
import apikeystore.exception.JwtParsingException;
import apikeystore.exception.KeyExpiredException;
import apikeystore.exception.MissingRequiredParameterException;
import apikeystore.exception.RTExpiredException;
import apikeystore.exception.SignatureMismatchException;
import apikeystore.exception.TokenDoesNotExistException;
import apikeystore.model.Key;
import apikeystore.model.KeyRequest;
import apikeystore.model.OAuth2AccessToken;
import apikeystore.model.OAuth2RefreshToken;
import apikeystore.model.QKey;
import apikeystore.model.Resource;
import apikeystore.repository.KeyRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping(value = "keys")
public class KeyController {

	@Autowired
	private KeyRepository keyRepository;

	// YAML config object for retrieving variables stored in the application.yml
	// file.
	@Autowired
	private YAMLConfig config;

	private static final Logger logger = LoggerFactory.getLogger(KeyController.class);

	// Page findAll(Map<String, String> requestParams)
	// Finds all keys in the DB
	// Returns a page object containing a list of all keys in the repository (that
	// meet the filter criteria), and Status.OK
	@SuppressWarnings("rawtypes")
	@RequestMapping(method = RequestMethod.GET, produces = { "application/json" })
	@ApiOperation(value = "Get all keys.", notes = "Returns all keys in the keystore.")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Page findAll(@RequestParam Map<String, String> requestParams) throws BadParameterException {

		// Check for bad parameter keys.
		if (requestParams != null && !requestParams.isEmpty()) {
			logger.debug("findAll method called with parameters: " + requestParams.toString());
			paramCheck(requestParams.keySet());
		} else {
			logger.debug("findAll method called with no parameters.");
		}

		// Execute query, construct result using predicate, page and (optionally)
		// projection setup methods, and return the resulting paginated list.
		if (requestParams.containsKey("fields") && !requestParams.get("fields").isEmpty()) {
			List<Key> result = keyRepository.findAll(predSetup(requestParams), pageSetup(requestParams)).getContent();
			return new PageImpl<Map<String, Object>>(projectFields(result, requestParams.get("fields")),
					pageSetup(requestParams), result.size());
		} else {
			return keyRepository.findAll(predSetup(requestParams), pageSetup(requestParams));
		}

	}

	// void paramCheck(Set<String> paramKeys)
	// Throws a BadParameterException (returning Status.BAD_REQUEST) if any of the
	// passed-in parameter keys is invalid.
	private void paramCheck(Set<String> paramKeys) throws BadParameterException {
		String[] acceptable = { "page", "size", "sortBy", "sortOrder", "fields", "userId", "clientId", "created",
				"modified", "agencyCode", "attributesAgencyCode", "authTokenValue", "authTokenScope",
				"authTokenExpiration", "authTokenExpired", "refTokenValue", "refTokenExpiration", "refTokenExpired" };

		for (String pkey : paramKeys) {
			if ((!Arrays.asList(acceptable).contains(pkey))
					&& (!(pkey.startsWith("attributes") && !pkey.endsWith("attributes")))) {
				logger.debug("Bad parameter exception thrown: '{}' is not recognized.", pkey);
				throw new BadParameterException();
			}
		}

		return;
	}

	// List<Map<String, Object>> projectFields(List<Key> fullResult, String,
	// fieldParams)
	// Helper function for handling field projection / cherry-picking on
	// collections.
	// Cherry-picks the Key object fields returned by the findAll method based on
	// the passed-in field parameters.
	// Returns a List of HashMaps of field names and values.
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<Map<String, Object>> projectFields(List<Key> fullResult, String fieldParams) {

		List<Map<String, Object>> result = new ArrayList<>();
		String[] params = fieldParams.split(",");
		Map<String, Object> entry;
		Map<String, Object> ptr;

		logger.debug("Field cherry-picking on: {}", fieldParams);

		// Go through each key object in the full result, pick out the fields the user
		// wants and construct a new list to return.
		for (Key key : fullResult) {
			entry = new HashMap<String, Object>();

			// The easy ones.
			if (Arrays.asList(params).contains("userId"))
				entry.put("userId", key.getUserId());
			if (Arrays.asList(params).contains("clientId"))
				entry.put("clientId", key.getClientId());
			if (Arrays.asList(params).contains("created"))
				entry.put("created", key.getCreated());
			if (Arrays.asList(params).contains("modified"))
				entry.put("modified", key.getModified());

			// May need to change this in the future to allow picking specific attribute
			// fields other than agencyCode.
			if (Arrays.asList(params).contains("attributes")) {
				entry.put("attributes", key.getAttributes());
			} else {
				if (Arrays.asList(params).contains("agencyCode")
						|| Arrays.asList(params).contains("attributesAgencyCode")) {
					entry.put("attributes", new HashMap<String, Object>());
					ptr = (HashMap) entry.get("attributes");
					ptr.put("agencyCode", key.getAttributes().get("agencyCode"));
				}
			}

			// Auth token: whole thing or subfields.
			if (Arrays.asList(params).contains("authToken")) {
				entry.put("authToken", key.getAuthToken());
			} else {
				if (Arrays.asList(params).contains("authTokenValue")) {
					entry.put("authToken", new HashMap<String, Object>());
					ptr = (HashMap) entry.get("authToken");
					ptr.put("value", key.getAuthToken().getValue());
				}
				if (Arrays.asList(params).contains("authTokenExpiration")) {
					if (entry.get("authToken") == null)
						entry.put("authToken", new HashMap<String, Object>());
					ptr = (Map) entry.get("authToken");
					ptr.put("expiration", key.getAuthToken().getExpiration());
				}
				if (Arrays.asList(params).contains("authTokenScope")) {
					if (entry.get("authToken") == null)
						entry.put("authToken", new HashMap<String, Object>());
					ptr = (Map) entry.get("authToken");
					ptr.put("scope", key.getAuthToken().getScope());
				}
				if (Arrays.asList(params).contains("authTokenExpired")) {
					if (entry.get("authToken") == null)
						entry.put("authToken", new HashMap<String, Object>());
					ptr = (Map) entry.get("authToken");
					ptr.put("expired", key.getAuthToken().isExpired());
				}
			}

			// Refresh token: whole thing or subfields.
			if (Arrays.asList(params).contains("refToken")) {
				entry.put("refToken", key.getRefToken());
			} else {
				if (Arrays.asList(params).contains("refTokenValue")) {
					entry.put("refToken", new HashMap<String, Object>());
					ptr = (HashMap) entry.get("refToken");
					ptr.put("value", key.getRefToken().getValue());
				}
				if (Arrays.asList(params).contains("refTokenExpiration")) {
					if (entry.get("refToken") == null)
						entry.put("refToken", new HashMap<String, Object>());
					ptr = (Map) entry.get("refToken");
					ptr.put("expiration", key.getRefToken().getExpiration());
				}
				if (Arrays.asList(params).contains("refTokenExpired")) {
					if (entry.get("refToken") == null)
						entry.put("refToken", new HashMap<String, Object>());
					ptr = (Map) entry.get("refToken");
					ptr.put("expired", key.getRefToken().isExpired());
				}
			}

			result.add(entry);
		}

		return result;

	}

	// Predicate predSetup(Map<String, String> requestParams)
	// Helper function for handling filtering on collections.
	// Returns a Predicate object constructed from the passed-in parameters using
	// Querydsl.
	private Predicate predSetup(Map<String, String> requestParams) {
		QKey key = new QKey("key");
		Predicate predicate = null;
		BooleanExpression temp = null;
		Long now = ((new Date().getTime()) / 1000);
		String param;
		String[] params;
		String attKey;

		// Short circuit if no parameters were passed in.
		if (requestParams == null || requestParams.isEmpty())
			return predicate;

		if (requestParams.containsKey("userId")) {
			predicate = predBuildString(requestParams.get("userId"), predicate, key.userId);
		}

		if (requestParams.containsKey("clientId")) {
			predicate = predBuildString(requestParams.get("clientId"), predicate, key.clientId);
		}

		// The attributes map has to be handled differently, since the keys are not
		// explicitly set.
		for (Entry<String, String> entry : requestParams.entrySet()) {
			attKey = entry.getKey();
			if (attKey.startsWith("attributes")) {

				param = entry.getValue();

				attKey = attKey.replace("attributes", "");
				char c[] = attKey.toCharArray();
				c[0] = Character.toLowerCase(c[0]);
				attKey = new String(c);

				if (param.endsWith("*")) {
					predicate = key.attributes.containsKey(attKey).and(key.attributes.get(attKey)
							.containsIgnoreCase(param.substring(0, param.length() - 1)).and(predicate));
				} else if (param.startsWith("$exists:true")) {
					predicate = key.attributes.containsKey(attKey)
							.and(key.attributes.get(attKey).isNotNull().and(predicate));
				} else if (param.startsWith("$exists:false")) {
					predicate = (key.attributes.get(attKey).isNull()).and(predicate);
				} else if (param.startsWith("$in:")) {
					param = param.substring(4, param.length());
					params = param.split(",");
					for (String i : params) {
						temp = (key.attributes.containsKey(attKey).and(key.attributes.get(attKey).eq(i))).or(temp);
					}
					predicate = temp.and(predicate);
					temp = null;
				} else {
					predicate = key.attributes.containsKey(attKey).and((key.attributes.get(attKey).eq(param)))
							.and(predicate);
				}
			}
		}

		// Provides special alias for agency code (since it will be the attribute most
		// frequently filtered on). Thus, it can be filtered with or without the
		// "attributes" prefix.
		if (requestParams.containsKey("agencyCode")) {
			param = requestParams.get("agencyCode");
			if (param.endsWith("*")) {
				predicate = key.attributes.containsKey("agencyCode").and(key.attributes.get("agencyCode")
						.containsIgnoreCase(param.substring(0, param.length() - 1)).and(predicate));
			} else if (param.startsWith("$exists:true")) {
				predicate = key.attributes.containsKey("agencyCode")
						.and(key.attributes.get("agencyCode").isNotNull().and(predicate));
			} else if (param.startsWith("$exists:false")) {
				predicate = (key.attributes.get("agencyCode").isNull()).and(predicate);
			} else if (param.startsWith("$in:")) {
				param = param.substring(4, param.length());
				params = param.split(",");
				for (String i : params) {
					temp = (key.attributes.containsKey("agencyCode").and(key.attributes.get("agencyCode").eq(i)))
							.or(temp);
				}
				predicate = temp.and(predicate);
				temp = null;
			} else {
				predicate = key.attributes.containsKey("agencyCode").and((key.attributes.get("agencyCode").eq(param)))
						.and(predicate);
			}
		}

		if (requestParams.containsKey("created")) {
			predicate = predBuildLong(requestParams.get("created"), predicate, key.created);
		}

		if (requestParams.containsKey("modified")) {
			predicate = predBuildLong(requestParams.get("modified"), predicate, key.modified);
		}

		if (requestParams.containsKey("authTokenValue")) {
			predicate = predBuildString(requestParams.get("authTokenValue"), predicate, key.authToken.value);
		}

		// TODO: Changing the scope field from Set<String> to Set<Resource> broke
		// filtering on scope. I think fixing it will involve getting the querydsl
		// plugin to generate a QResource class and then tweaking the commented out
		// section below to use it, but I haven't been able to figure that out yet, and
		// I may not have time to get back to it (hence this note).

		// The other special case (it sometimes needs .any() on the stringpath, since
		// it's a Set).
		/*
		 * if (requestParams.containsKey("authTokenScope")) { param =
		 * requestParams.get("authTokenScope"); if (param.endsWith("*")) { predicate =
		 * key.authToken.scope.any().containsIgnoreCase(param.substring(0,
		 * param.length() - 1)) .and(predicate); } else if
		 * (param.startsWith("$exists:true")) { predicate =
		 * key.authToken.scope.isNotEmpty().and(predicate); } else if
		 * (param.startsWith("$exists:false")) { predicate =
		 * key.authToken.scope.isEmpty().and(predicate); } else if
		 * (param.startsWith("$in:")) { param = param.substring(4, param.length());
		 * params = param.split(","); for (String i : params) { temp =
		 * key.authToken.scope.any().eq(i).or(temp); } predicate = temp.and(predicate);
		 * temp = null; } else { predicate =
		 * key.authToken.scope.contains(param).and(predicate); } }
		 */

		if (requestParams.containsKey("authTokenExpiration")) {
			predicate = predBuildLong(requestParams.get("authTokenExpiration"), predicate, key.authToken.expiration);
		}

		if (requestParams.containsKey("authTokenExpired")) {
			if (requestParams.get("authTokenExpired").toLowerCase().equals("true")) {
				predicate = key.authToken.expiration.lt(now).and(predicate);
			} else {
				predicate = key.authToken.expiration.gt(now).and(predicate);
			}
		}

		if (requestParams.containsKey("refTokenValue")) {
			predicate = predBuildString(requestParams.get("refTokenValue"), predicate, key.refToken.value);
		}

		if (requestParams.containsKey("refTokenExpiration")) {
			predicate = predBuildLong(requestParams.get("refTokenExpiration"), predicate, key.refToken.expiration);
		}

		if (requestParams.containsKey("refTokenExpired")) {
			if (requestParams.get("refTokenExpired").toLowerCase().equals("true")) {
				predicate = key.refToken.expiration.lt(now).and(predicate);
			} else {
				predicate = key.refToken.expiration.gt(now).and(predicate);
			}
		}

		logger.debug("Query predicate constructed: {}", predicate);

		return predicate;
	}

	// Predicate predBuildGeneral(String, Predicate, StringPath)
	// Helper function for constructing predicate portions involving string
	// parameters.
	// Implementation for the $exists, $in and wildcard collection filtering
	// functionality.
	private Predicate predBuildString(String param, Predicate predicate, StringPath path) {
		String[] params = null;
		BooleanExpression temp = null;

		if (param.endsWith("*")) {
			predicate = path.containsIgnoreCase(param.substring(0, param.length() - 1)).and(predicate);
		} else if (param.startsWith("$exists:true")) {
			predicate = path.isNotNull().and(predicate);
		} else if (param.startsWith("$exists:false")) {
			predicate = path.isNull().and(predicate);
		} else if (param.startsWith("$in:")) {
			param = param.substring(4, param.length());
			params = param.split(",");
			for (String i : params) {
				temp = path.eq(i).or(temp);
			}
			predicate = temp.and(predicate);
			temp = null;
		} else {
			predicate = path.eq(param).and(predicate);
		}

		return predicate;
	}

	// Predicate predBuildGeneral(String, Predicate, NumberPath<Long>)
	// Helper function for constructing predicate portions involving long integer
	// parameters.
	// Implementation for the $exists and numeric operator collection filtering
	// functionality.
	private Predicate predBuildLong(String param, Predicate predicate, NumberPath<Long> path) {
		String[] params = null;
		BooleanExpression temp = null;

		if (param.startsWith("$gt:")) {
			predicate = path.gt(Long.parseLong(param.substring(4, param.length()))).and(predicate);
		} else if (param.startsWith("$lt:")) {
			predicate = path.lt(Long.parseLong(param.substring(4, param.length()))).and(predicate);
		} else if (param.startsWith("$eq:")) {

			predicate = path.eq(Long.parseLong(param.substring(4, param.length()))).and(predicate);
		} else if (param.startsWith("$exists:true")) {
			predicate = path.isNotNull().and(predicate);
		} else if (param.startsWith("$exists:false")) {
			predicate = path.isNull().and(predicate);
		} else if (param.startsWith("$in:")) {
			param = param.substring(4, param.length());
			params = param.split(",");
			for (String i : params) {
				long j = Long.parseLong(i);
				temp = path.eq(j).or(temp);
			}
			predicate = temp.and(predicate);
			temp = null;
		} else {
			predicate = path.eq(Long.parseLong(param)).and(predicate);
		}

		return predicate;
	}

	// Pageable pageSetup(Map<String, String> requestParams)
	// Helper function for handling pagination and sorting on collections.
	// Returns a Pageable object constructed from the passed-in parameters.
	private Pageable pageSetup(Map<String, String> requestParams) {
		// Default paging values
		int page = 0;
		int size = 20;
		String sortBy = null;
		String[] sortByMulti = null;
		String sortOrder = "ASC";
		String[] sortOrderMulti = null;
		PageRequest result;
		String param;

		// Get any sorting or filtering parameters passed in from the request.
		if (requestParams != null) {
			if (requestParams.containsKey("page")) {
				page = Integer.parseInt(requestParams.get("page"));
			}

			if (requestParams.containsKey("size")) {
				size = Integer.parseInt(requestParams.get("size"));
			}

			if (requestParams.containsKey("sortBy")) {
				param = sortAlias(requestParams.get("sortBy"));
				if (param.contains(",")) {
					sortByMulti = param.split(",");
				} else {
					sortBy = param;
				}
			}

			if (requestParams.containsKey("sortOrder")) {
				param = requestParams.get("sortOrder");
				if (param.contains(",")) {
					sortOrderMulti = param.split(",");
				} else {
					sortOrder = param;
				}
			}
		}

		// Ascending order by default.
		Sort.Direction direction = Sort.Direction.ASC;

		if (sortOrder.toUpperCase().equals("DESC")) {
			direction = Sort.Direction.DESC;
		}

		if (sortByMulti != null) {
			if (sortOrderMulti != null) { // Multiple fields and directions.
				Sort.Order[] order = new Sort.Order[sortByMulti.length];
				for (int i = 0; i < order.length; ++i) {
					if (sortOrderMulti.length > i && sortOrderMulti[i].toUpperCase().equals("DESC")) {
						order[i] = new Sort.Order(Sort.Direction.DESC, sortByMulti[i]);
					} else {
						order[i] = new Sort.Order(Sort.Direction.ASC, sortByMulti[i]);
					}
				}

				Sort sort = new Sort(order);
				result = new PageRequest(page, size, sort);

			} else { // Multiple fields, one direction.
				Sort sort = new Sort(direction, sortByMulti);
				result = new PageRequest(page, size, sort);
			}

		} else if (sortBy != null) { // One field, one direction
			Sort sort = new Sort(direction, sortBy);
			result = new PageRequest(page, size, sort);

		} else { // No sort applied
			result = new PageRequest(page, size);
		}

		logger.debug("Page request constructed: {}", result.toString());

		return result;
	}

	// String sortAlias(String param)
	// Quick fix for inconsistency between expected formats for sorting parameters
	// and filtering/cherry-picking/everything else parameters.
	private String sortAlias(String param) {
		param = param.replaceAll("TokenValue", "Token.value");
		param = param.replaceAll("TokenExpiration", "Token.expiration");
		param = param.replaceAll("TokenExpired", "Token.expired");
		param = param.replaceAll("TokenScope", "Token.scope.resource");
		param = param.replaceAll("agencyCode", "attributes.agencyCode");
		param = param.replaceAll("attributes\\.attributes", "attributes");
		return param;
	}

	// List<Key> findAll()
	// Vanilla, non-paginated, non-filtering version of findAll.
	protected List<Key> findAll() {
		return keyRepository.findAll();
	}

	// Page findAllFromUserId(String userId, Map<String, String> requestParams)
	// Finds all keys for a specific userID
	// Returns Status.OK and any keys matching the supplied userId.
	@SuppressWarnings("rawtypes")
	@RequestMapping(value = "user/{userId:.+}", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Get all keys matching the passed-in user ID (JSON objects)", notes = "Get all keys from the keystore that match the provided user ID.  Keys are returned as a paginated list of JSON objects.")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Page findAllFromUserID(
			@ApiParam(required = true, value = "Example: Sample_Added_User_ID_Swagger") @PathVariable("userId") String userId,
			@RequestParam Map<String, String> requestParams) throws BadParameterException {

		// Check for bad parameter keys.
		if (requestParams != null && !requestParams.isEmpty()) {
			logger.debug("findAllFromUserID method called for user: {} with parameters: {}", userId,
					requestParams.toString());
			paramCheck(requestParams.keySet());
		} else {
			logger.debug("findAllFromUserID method called with no parameters.");
		}

		if (requestParams.containsKey("fields") && !requestParams.get("fields").isEmpty()) {
			List<Key> result = keyRepository.findAllByUserId(userId, pageSetup(requestParams)).getContent();
			return new PageImpl<Map<String, Object>>(projectFields(result, requestParams.get("fields")),
					pageSetup(requestParams), result.size());
		} else {
			return keyRepository.findAllByUserId(userId, pageSetup(requestParams));
		}
	}

	// Page findAllFromClientId(String clientId, Map<String, String> requestParams)
	// Finds all keys for a specific clientID
	// Returns Status.OK and any keys matching the supplied clientId.
	@SuppressWarnings("rawtypes")
	@RequestMapping(value = "client/{clientId:.+}", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Get all keys matching the passed-in client ID (JSON objects)", notes = "Get all keys from the keystore that match the provided client ID.  Keys are returned as a paginated list of JSON objects.")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Page findAllFromClientID(
			@ApiParam(required = true, value = "Example: Sample_Added_Client_ID_Swagger") @PathVariable("clientId") String clientId,
			@RequestParam Map<String, String> requestParams) throws BadParameterException {

		if (requestParams != null && !requestParams.isEmpty()) {
			logger.debug("findAllFromClientID method called for client: {} with parameters: {}", clientId,
					requestParams.toString());
			paramCheck(requestParams.keySet());
		} else {
			logger.debug("findAllFromClientID method called with no parameters.");
		}

		if (requestParams.containsKey("fields") && !requestParams.get("fields").isEmpty()) {
			List<Key> result = keyRepository.findAllByClientId(clientId, pageSetup(requestParams)).getContent();
			return new PageImpl<Map<String, Object>>(projectFields(result, requestParams.get("fields")),
					pageSetup(requestParams), result.size());
		} else {
			return keyRepository.findAllByClientId(clientId, pageSetup(requestParams));
		}
	}

	// Page findAllFromAgencyCode(String agencyCode, Map<String, String>
	// requestParams)
	// Finds and returns all keys matching the passed in agency code attribute.
	// Returns Status.OK and any keys matching the supplied agency code.
	@SuppressWarnings("rawtypes")
	@RequestMapping(value = "agency/{agencyCode:.+}", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Get all keys matching the passed-in agency ID (JSON objects)", notes = "Get all keys from the keystore that match the provided agency ID.  Keys are returned as a paginated list of JSON objects.")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody Page findAllFromAgencyCode(
			@ApiParam(required = true, value = "Example: Sample_Agency") @PathVariable("agencyCode") String agencyCode,
			@RequestParam Map<String, String> requestParams) throws BadParameterException {

		if (requestParams != null && !requestParams.isEmpty()) {
			logger.debug("findAllFromAgencyCode method called for agency code: {} with parameters: {}", agencyCode,
					requestParams.toString());
			paramCheck(requestParams.keySet());
		} else {
			logger.debug("findAllFromAgencyCode method called with no parameters.");
		}

		if (requestParams.containsKey("fields") && !requestParams.get("fields").isEmpty()) {
			List<Key> result = keyRepository.findAllByAgencyCode(agencyCode, pageSetup(requestParams)).getContent();
			return new PageImpl<Map<String, Object>>(projectFields(result, requestParams.get("fields")),
					pageSetup(requestParams), result.size());
		} else {
			return keyRepository.findAllByAgencyCode(agencyCode, pageSetup(requestParams));
		}
	}

	// HttpEntity<Key> generateAndAdd(Map<String, String> request)
	// Generate and add a key to the repository. If a key already exists with the
	// provided userID and clientID, replace it.
	// Returns Status.CREATED if successful.
	@RequestMapping(method = RequestMethod.POST)
	@ApiOperation(value = "Add a key from a user ID, client ID and scope.", notes = "Creates a new key for the supplied user and client IDs and scope and adds it to the keystore.")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public @ResponseBody HttpEntity<Key> generateAndAdd(
			@ApiParam(required = true, name = "request", value = "JSON object containing the userId, clientId and scope for the key to be added.") @RequestBody KeyRequest request)
			throws MissingRequiredParameterException, AddFailureException {

		if (request.getUserId() == null || request.getClientId() == null || request.getUserId().isEmpty()
				|| request.getClientId().isEmpty()) {
			throw new MissingRequiredParameterException();
		}

		String userId = new String(request.getUserId());
		String clientId = new String(request.getClientId());

		Key oldKey = null;
		Key key;
		long created = -1; // Sets created date to now (for a new key)

		// If a key already exists with the given userId and clientId, delete and
		// replace it.
		key = keyRepository.findById(userId + "__" + clientId);
		if (key != null) {
			oldKey = new Key(key);
			created = key.getCreated(); // Sets created date to original timestamp (if "updating")
			keyRepository.removeKey(key);
			logger.debug("generateAndAdd: Replacing old key {}", oldKey.getId());
		}

		try {
			OAuth2AccessToken token = new OAuth2AccessToken(userId, request.getScope());
			OAuth2RefreshToken ref = new OAuth2RefreshToken(userId);

			HashMap<String, String> atts = new HashMap<String, String>();
			if (request.getAttributes() != null) {
				atts.putAll(request.getAttributes());
			}

			key = new Key(token, ref, userId, clientId, created, atts);

			keyRepository.saveKey(key);
		} catch (Exception e) {
			if (oldKey != null) {
				keyRepository.saveKey(oldKey);
				logger.error("generateAndAdd failure: Re-added old key {}", oldKey.getId());
			}
			logger.error("Add failure in generateAndAdd(), attempted to add: " + key.toString());
			throw new AddFailureException();
		}

		return new ResponseEntity<Key>(key, HttpStatus.CREATED);
	}

	// HttpEntity<Key> addKey(Key request)
	// Add an existing key object to the repository. If a key already exists with
	// the provided userID and clientID, replace it.
	// Returns Status.CREATED if successful.
	@RequestMapping(value = "/obj", method = RequestMethod.POST)
	@ApiOperation(value = "Add a key from a JSON object.", notes = "Adds a key (passed as a JSON object) to the keystore.")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public @ResponseBody HttpEntity<Key> addKey(
			@ApiParam(required = true, name = "key", value = "JSON object containing the key data to be stored.") @RequestBody Key request)
			throws MissingRequiredParameterException, AddFailureException {

		if (request.getUserId() == null || request.getClientId() == null || request.getUserId().isEmpty()
				|| request.getClientId().isEmpty()) {
			throw new MissingRequiredParameterException();
		}

		Key oldKey = null;
		Key key;
		long created = -1; // Sets created date to now (for a new key)

		// If a key already exists with the given userId and clientId, delete and
		// replace it.
		key = keyRepository.findById(request.getId());
		if (key != null) {
			oldKey = new Key(key);
			created = key.getCreated(); // Sets created date to original timestamp (if "updating")
			keyRepository.removeKey(key);
			logger.debug("addKey: Replacing old key {}", oldKey.getId());
		}

		try {
			OAuth2AccessToken token = new OAuth2AccessToken(request.getAuthToken());
			OAuth2RefreshToken ref = new OAuth2RefreshToken(request.getRefToken());

			HashMap<String, String> atts = new HashMap<String, String>();
			if (request.getAttributes() != null) {
				atts.putAll(request.getAttributes());
			}

			key = new Key(token, ref, request.getUserId(), request.getClientId(), created, atts);

			keyRepository.saveKey(key);
		} catch (Exception e) {
			if (oldKey != null) {
				keyRepository.saveKey(oldKey);
				logger.error("addKey failure: Re-added old key {}", oldKey.getId());
			}
			logger.error("Add failure in addKey(), attempted to add: " + key.toString());
			throw new AddFailureException();
		}

		return new ResponseEntity<Key>(key, HttpStatus.CREATED);
	}

	// HttpEntity<Key> addKey(String request)
	// Add a key to the repository. If a key already exists with the provided
	// userID and clientID, replace it. Same as the above 'add' method, but accepts
	// a JSON web
	// token version instead of a JSON object.
	// Returns Status.CREATED if successful.
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/jwt", method = RequestMethod.POST)
	@ApiOperation(value = "Add a key from a JSON Web Token.", notes = "Adds a key (passed as a JSON Web Token) to the keystore.")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public @ResponseBody HttpEntity<Key> addJwt(
			@ApiParam(required = true, name = "key", value = "JWT containing the key data to be stored.  Example: eyJhbGciOiJIUzI1NiJ9.eyJ1aWQiOiJTYW1wbGVfQWRkZWRfSldUX1VzZXJfSURfU3dhZ2dlciIsImNpZCI6IlNhbXBsZV9BZGRlZF9KV1RfQ2xpZW50X0lEX1N3YWdnZXIiLCJjcmVhdGVkIjoxNTIwMDMxMDY3OCwibW9kaWZpZWQiOjE1MjAwMzEwNjc4LCJhdFZhbHVlIjoiU2FtcGxlQWRkZWRKV1RBY2Nlc3NUb2tlbl9Td2FnZ2VyIiwiYXRFeHAiOjE1MTkzNDM4MzAwLCJzY29wZSI6WyJTY29wZSAxIiwiU2NvcGUgMiJdLCJyZWZWYWx1ZSI6IlNhbXBsZUFkZGVkSldUUmVmcmVzaFRva2VuX1N3YWdnZXIiLCJyZWZFeHAiOjE1MjAyODAzOTEwLCJqdGkiOiJTYW1wbGVfQWRkZWRfVXNlcl9JRCIsImlhdCI6MTYwMDAwMDAwMCwiZXhwIjoxNjAwMDAwMDAwfQ.xYmgsCw5RVcYRGtp_SwrODK6zaxUECBPuKe0lzARarE") @RequestBody String request)
			throws SignatureMismatchException, MissingRequiredParameterException, AddFailureException,
			JwtParsingException {

		Key oldKey = null;
		Key key;
		Claims claims = null;

		try {

			claims = Jwts.parser().setSigningKey(config.getJwtSigningKey().getBytes("UTF-8")).parseClaimsJws(request)
					.getBody();

		} catch (SignatureException e) {
			logger.error("JWT signing key mismatch: {}", e.getMessage());
			throw new SignatureMismatchException();
		} catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | IllegalArgumentException
				| UnsupportedEncodingException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new JwtParsingException();
		}

		if (claims.get("userId") == null || claims.get("clientId") == null
				|| claims.get("userId", String.class).isEmpty() || claims.get("clientId", String.class).isEmpty()) {
			throw new MissingRequiredParameterException();
		}

		key = keyRepository.findById(claims.get("userId", String.class) + "__" + claims.get("clientId", String.class));
		long created = -1; // Sets created date to now (for a new key)

		// If a key already exists with the given userId and clientId, delete and
		// replace it.
		if (key != null) {
			oldKey = new Key(key);
			created = key.getCreated(); // Sets created date to original timestamp (if "updating")
			keyRepository.removeKey(key);
			logger.debug("addJwt: Replacing old key {}", oldKey.getId());
		}

		try {
			Set<Resource> scope = new HashSet<Resource>((List<Resource>) claims.get("authTokenScope"));
			logger.debug(claims.get("authTokenScope").toString());
			OAuth2AccessToken token = new OAuth2AccessToken(claims.get("authTokenValue", String.class),
					claims.get("authTokenExpiration", Long.class), scope);
			OAuth2RefreshToken ref = new OAuth2RefreshToken(claims.get("refTokenValue", String.class),
					claims.get("refTokenExpiration", Long.class));

			HashMap<String, String> atts = new HashMap<String, String>();
			if (claims.get("attributes") != null) {
				atts.putAll(claims.get("attributes", HashMap.class));
			}

			key = new Key(token, ref, claims.get("userId", String.class), claims.get("clientId", String.class), created,
					atts);

			keyRepository.saveKey(key);
		} catch (Exception e) {
			if (oldKey != null) {
				keyRepository.saveKey(oldKey);
				logger.error("addJwt failure: Re-added old key {}", oldKey.getId());
			}
			logger.error("Add failure in addJwt(), attempted to add: " + key.toString());
			throw new AddFailureException();
		}

		return new ResponseEntity<Key>(key, HttpStatus.CREATED);
	}

	// HttpEntity<Key> findToken(String authValue)
	// Finds a specific key by the auth token value. If the AT is expired and the RT
	// is still valid, both tokens are refreshed.
	// Returns Status.OK and the key if the key is found and the tokens are not
	// expired,
	// returns Status.NOT_FOUND if it isn't found, and
	// returns Status.GONE and deletes the key from the repository if the key is
	// found but both tokens are expired.
	@RequestMapping(value = "/token/{authValue}", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Get a key by auth token value (JSON object)", notes = "Get a single key from the keystore that matches the provided auth token value.  Key is returned as a JSON object.  Found keys with an expired access token but valid refresh token are refreshed before being returned.  Found keys with an expired access token and refresh token are removed from the keystore.")
	public @ResponseBody HttpEntity<Key> findToken(
			@ApiParam(required = true) @PathVariable("authValue") String authValue)
			throws TokenDoesNotExistException, KeyExpiredException, ATExpiredException {

		Key key = keyRepository.findByAuthTokenValue(authValue);
		if (key == null) {
			throw new TokenDoesNotExistException();
		}

		if (key.getAuthToken().isExpired() && (key.getRefToken() == null || (key.getRefToken().isExpired()))) {
			keyRepository.removeKey(key);
			throw new KeyExpiredException();
		}

		if (key.getAuthToken().isExpired() && !key.getRefToken().isExpired()) {
			throw new ATExpiredException();
		}

		return new ResponseEntity<Key>(key, HttpStatus.OK);
	}

	// HttpEntity<Key> find(String userId, String clientId)
	// Finds a specific key by userID and clientID. If the AT is expired and the RT
	// is still valid, both tokens are refreshed.
	// Returns Status.OK and the key if the key is found and the tokens are not
	// expired,
	// returns Status.NOT_FOUND if it isn't found, and
	// returns Status.GONE and deletes the key from the repository if the key is
	// found but both tokens are expired.
	@RequestMapping(value = "/{userId:.+}/{clientId:.+}", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Get a key by user ID and clientId (JSON object)", notes = "Get a single key from the keystore that matches the provided user and client IDs.  Key is returned as a JSON object.  Found keys with an expired access token but valid refresh token are refreshed before being returned.  Found keys with an expired access token and refresh token are removed from the keystore.")
	public @ResponseBody HttpEntity<Key> find(
			@ApiParam(required = true, value = "Example: Sample_Added_User_ID_Swagger") @PathVariable("userId") String userId,
			@ApiParam(required = true, value = "Example: Sample_Added_Client_ID_Swagger") @PathVariable("clientId") String clientId)
			throws DoesNotExistException, KeyExpiredException, ATExpiredException {

		Key key = keyRepository.findById(userId + "__" + clientId);
		if (key == null) {
			throw new DoesNotExistException();
		}

		if (key.getAuthToken().isExpired() && (key.getRefToken() == null || (key.getRefToken().isExpired()))) {
			keyRepository.removeKey(key);
			throw new KeyExpiredException();
		}

		if (key.getAuthToken().isExpired() && !key.getRefToken().isExpired()) {
			throw new ATExpiredException();
		}

		return new ResponseEntity<Key>(key, HttpStatus.OK);
	}

	// HttpEntity<String> findJwt(String userId, String clientId)
	// Same as the above find method, but returns a JSON web token (JWT) version of
	// the
	// key. If the AT is expired and the RT is still valid, both tokens are
	// refreshed.
	// Returns Status.OK and the key if the key is found and the tokens are not
	// expired,
	// returns Status.NOT_FOUND if it isn't found, and
	// returns Status.GONE and deletes the key from the repository if the key is
	// found but both tokens are expired.
	@RequestMapping(value = "/jwt/{userId:.+}/{clientId:.+}", method = RequestMethod.GET)
	@ApiOperation(value = "Get a key by user ID and client ID (JWT)", notes = "Get a single key from the keystore that matches the provided user ID and client ID.  Key is returned as a JSON Web Token.  Found keys with an expired access token but valid refresh token are refreshed before being returned.  Found keys with an expired access token and refresh token are removed from the keystore.")
	public @ResponseBody HttpEntity<String> findJwt(
			@ApiParam(required = true, value = "Example: Sample_Added_JWT_User_ID_Swagger") @PathVariable("userId") String userId,
			@ApiParam(required = true, value = "Example: Sample_Added_JWT_Client_ID_Swagger") @PathVariable("clientId") String clientId)
			throws KeyExpiredException, ATExpiredException, JwtParsingException, ExpiredJwtException,
			UnsupportedJwtException, MalformedJwtException, SignatureException, IllegalArgumentException,
			UnsupportedEncodingException {
		Key key;
		String jwt = null;
		key = keyRepository.findById(userId + "__" + clientId);
		if (key == null) {
			throw new DoesNotExistException();
		}
		if (key.getAuthToken().isExpired() && (key.getRefToken() == null || (key.getRefToken().isExpired()))) {
			keyRepository.removeKey(key);
			throw new KeyExpiredException();
		}

		if (key.getAuthToken().isExpired() && !key.getRefToken().isExpired()) {
			throw new ATExpiredException();
		}

		// Build the JWT response.
		try {
			jwt = createJwt(key);
		} catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | IllegalArgumentException
				| UnsupportedEncodingException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new JwtParsingException();
		}

		// Check that the JWT was built correctly and can be decoded.
		if (!key.getUserId().equals(Jwts.parser().setSigningKey(config.getJwtSigningKey().getBytes("UTF-8"))
				.parseClaimsJws(jwt).getBody().getId())) {
			logger.error("Exception thrown in findJwt: JWT cannot be decoded or decoded UID is incorrect.");
			throw new JwtParsingException();
		}

		return new ResponseEntity<String>(jwt, HttpStatus.OK);
	}

	// HttpEntity<String> findTokenJwt(String authValue)
	// Same as the above findByAT method, but returns a JSON web token (JWT) version
	// of the key. If the AT is expired and the RT is still valid, both tokens are
	// refreshed.
	// Returns Status.OK and the key if the key is found and the tokens are not
	// expired,
	// returns Status.NOT_FOUND if it isn't found, and
	// returns Status.GONE and deletes the key from the repository if the key is
	// found but both tokens are expired.
	@RequestMapping(value = "/jwt/{authValue}", method = RequestMethod.GET)
	@ApiOperation(value = "Get a key by auth token value (JWT)", notes = "Get a single key from the keystore that matches the provided auth token value.  Key is returned as a JSON Web Token.  Found keys with an expired access token but valid refresh token are refreshed before being returned.  Found keys with an expired access token and refresh token are removed from the keystore.")
	public @ResponseBody HttpEntity<String> findTokenJwt(
			@ApiParam(required = true, value = "Example: Sample_Access_Token_Value_Sw") @PathVariable("authValue") String authValue)
			throws KeyExpiredException, JwtParsingException, ExpiredJwtException, UnsupportedJwtException,
			MalformedJwtException, SignatureException, IllegalArgumentException, UnsupportedEncodingException,
			ATExpiredException {
		Key key;
		String jwt = null;

		key = keyRepository.findByAuthTokenValue(authValue);
		if (key == null) {
			throw new TokenDoesNotExistException();
		}
		if (key.getAuthToken().isExpired() && (key.getRefToken() == null || (key.getRefToken().isExpired()))) {
			keyRepository.removeKey(key);
			throw new KeyExpiredException();
		}

		if (key.getAuthToken().isExpired() && !key.getRefToken().isExpired()) {
			throw new ATExpiredException();
		}

		// Build the JWT response.
		try {
			jwt = createJwt(key);
		} catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | IllegalArgumentException
				| UnsupportedEncodingException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			throw new JwtParsingException();
		}

		// Check that the JWT was built correctly and can be decoded.
		if (!key.getUserId().equals(Jwts.parser().setSigningKey(config.getJwtSigningKey().getBytes("UTF-8"))
				.parseClaimsJws(jwt).getBody().getId())) {
			logger.error("Exception thrown in findJwt: JWT cannot be decoded or decoded UID is incorrect.");
			throw new JwtParsingException();
		}

		return new ResponseEntity<String>(jwt, HttpStatus.OK);
	}

	// String createJwt(Key key)
	// Helper function, creates a JWT from a key.
	// Returns a string representing a JSON Web Token.
	protected String createJwt(Key key) throws UnsupportedEncodingException {

		SignatureAlgorithm alg = SignatureAlgorithm.HS256;
		JwtBuilder builder = Jwts.builder().claim("authTokenValue", key.getAuthToken().getValue())
				.claim("authTokenExpiration", key.getAuthToken().getExpiration())
				.claim("authTokenScope", key.getAuthToken().getScope())
				.claim("authTokenType", key.getAuthToken().getTokenType())
				.claim("authTokenExpired", key.getAuthToken().isExpired())
				.claim("refTokenValue", key.getRefToken().getValue())
				.claim("refTokenExpiration", key.getRefToken().getExpiration())
				.claim("refTokenExpired", key.getRefToken().isExpired()).claim("userId", key.getUserId())
				.claim("clientId", key.getClientId()).claim("attributes", key.getAttributes())
				.claim("created", key.getCreated()).claim("modified", key.getModified())
				.signWith(alg, config.getJwtSigningKey().getBytes("UTF-8"));

		builder.setId(key.getUserId()).setIssuedAt(Date.from(Instant.ofEpochSecond(1600000000)))
				.setExpiration(Date.from(Instant.ofEpochSecond(1600000000)));

		return builder.compact();
	}

	// Key refresh(Key key)
	// Helper function, updates the auth and refresh tokens of a parameter key.
	// Saves the updated key object to the repository (and removes the old entry).
	// Returns the updated key.
	protected Key refresh(Key key) {

		keyRepository.removeKey(key);

		// Create new auth token.
		key.setAuthToken(new OAuth2AccessToken(key.getUserId(), key.getAuthToken().getScope()));

		// Create new refresh token.
		key.setRefToken(new OAuth2RefreshToken(key.getUserId()));

		// Update modified timestamp.
		key.setModified(new Date().getTime() / 1000);

		// Remove the old key, then save the updated key to the repository and return
		// it.
		keyRepository.saveKey(key);
		return key;
	}

	// Removed update method, decided to go the replacement route instead.
	//
	// HttpEntity<Key> updateToken(String userId, String clientId)
	// Updates a specific key by the auth token value.
	// Returns Status.OK and the updated key if successful, or
	// Status.NOT_FOUND if the key is not in the repository.
	/*
	 * @RequestMapping(value = "/token/{authValue}", method = RequestMethod.PUT,
	 * produces = "application/json")
	 * 
	 * @ApiOperation(value = "Update a key by auth token value (JSON object)", notes
	 * =
	 * "Update a key from the keystore that matches the provided auth token value.  Updated key is returned as a JSON object."
	 * ) public @ResponseBody HttpEntity<Key> updateToken(
	 * 
	 * @ApiParam(required = true) @PathVariable("authValue") String authValue,
	 * 
	 * @ApiParam(required = true, name = "request") @RequestBody HashMap<String,
	 * String> newVals) throws DoesNotExistException, KeyExpiredException {
	 * 
	 * Key key = keyRepository.findByAuthTokenValue(authValue); if (key == null) {
	 * throw new TokenDoesNotExistException(); }
	 * 
	 * keyRepository.removeKey(key); long created = key.getCreated();
	 * 
	 * if (newVals.containsKey("scope") && !newVals.get("scope").isEmpty()) {
	 * Set<String> newScope = null; // TODO? }
	 * 
	 * key.setModified(new Date().getTime() / 1000); keyRepository.saveKey(key);
	 * 
	 * return new ResponseEntity<Key>(key, HttpStatus.OK); }
	 */

	// HttpEntity<Key> forceRefresh(String authValue)
	// Forces a refresh of the auth and refresh tokens for the key matching the
	// supplied refresh token value.
	// Returns Status.OK and the refreshed key if the key is found and the refresh
	// token is not expired,
	// returns Status.NOT_FOUND if it isn't found,
	// returns Status.GONE and deletes the key from the repository if the key is
	// found but both tokens are expired, and
	// returns Status.FORBIDDEN if the token is found and auth token is valid but
	// the refresh token is expired.
	@RequestMapping(value = "/refresh", method = RequestMethod.POST, produces = "application/json")
	@ApiOperation(value = "Force a refresh of a key's OAuth tokens", notes = "Forces a (potentially) early refresh of a key's tokens.  Found keys with a valid refresh token are refreshed before being returned.  Found keys with an expired access token and refresh token are removed from the keystore.")
	public @ResponseBody HttpEntity<Key> forceRefresh(@ApiParam(required = true) @RequestBody String refValue)
			throws TokenDoesNotExistException, KeyExpiredException, RTExpiredException {

		Key key = keyRepository.findByRefTokenValue(refValue);

		logger.debug("Refresh endpoint hit, refresh token value: {}", refValue);

		if (key == null) {
			throw new TokenDoesNotExistException();
		}

		if (key.getAuthToken().isExpired() && (key.getRefToken() == null || (key.getRefToken().isExpired()))) {
			keyRepository.removeKey(key);
			throw new KeyExpiredException();
		}

		if (key.getRefToken() == null || key.getRefToken().isExpired()) {
			throw new RTExpiredException();
		}

		key = refresh(key);

		logger.debug("Key refreshed, new auth token value: {}", key.getAuthToken().getValue());
		return new ResponseEntity<Key>(key, HttpStatus.OK);
	}

	// void delete(String id)
	// Delete the key with the passed user ID and client ID.
	// Returns Status.OK if successful, Status.NOT_FOUND otherwise
	@RequestMapping(value = "/{userId:.+}/{clientId:.+}", method = RequestMethod.DELETE)
	@ApiOperation(value = "Remove a specific key from the keystore", notes = "Removes a single key from the keystore that matches the provided user ID and client ID.")
	@ResponseStatus(HttpStatus.OK)
	public void delete(
			@ApiParam(required = true, value = "Example: Sample_User_ID_Sw") @PathVariable("userId") String userId,
			@ApiParam(required = true, value = "Example: Sample_Client_ID_Sw") @PathVariable("clientId") String clientId)
			throws DoesNotExistException {
		Key toDelete = keyRepository.findById(userId + "__" + clientId);
		if (toDelete == null) {
			throw new DoesNotExistException();
		}
		keyRepository.removeKey(toDelete);
	}

	// void deleteByAuthValue(String authValue)
	// Delete the key with the passed auth token value.
	// Returns Status.OK if successful, Status.NOT_FOUND otherwise
	@RequestMapping(value = "/token/{authValue}", method = RequestMethod.DELETE)
	@ApiOperation(value = "Remove a specific key from the keystore", notes = "Removes a single key from the keystore that matches the provided auth token value.")
	@ResponseStatus(HttpStatus.OK)
	public void deleteByAuthValue(@ApiParam(required = true) @PathVariable("authValue") String authValue)
			throws DoesNotExistException {
		Key toDelete = keyRepository.findByAuthTokenValue(authValue);
		if (toDelete == null) {
			throw new TokenDoesNotExistException();
		}
		keyRepository.removeKey(toDelete);
	}

	// void revokeClient(String cid)
	// Delete all keys with the passed client ID.
	// Returns Status.OK if successful, Status.NOT_FOUND otherwise
	@RequestMapping(value = "client/{clientId:.+}", method = RequestMethod.DELETE)
	@ApiOperation(value = "Remove all keys for a specific client from the keystore", notes = "Removes all keys from the keystore that match the provided client ID.")
	@ResponseStatus(HttpStatus.OK)
	public void revokeClient(
			@ApiParam(required = true, example = "Sample_Added_Client_ID_Swagger") @PathVariable("clientId") String cid)
			throws DoesNotExistException {
		List<Key> toDelete = keyRepository.findAllByClientId(cid);
		if (toDelete.isEmpty()) {
			throw new DoesNotExistException();
		}
		for (Key key : toDelete) {
			keyRepository.removeKey(key);
		}
	}

	// void revokeUser(String uid)
	// Delete all keys with the passed user ID.
	// Returns Status.OK if successful, Status.NOT_FOUND otherwise
	@RequestMapping(value = "user/{userId:.+}", method = RequestMethod.DELETE)
	@ApiOperation(value = "Remove all keys for a specific user from the keystore", notes = "Removes all keys from the keystore that match the provided user ID.")
	@ResponseStatus(HttpStatus.OK)
	public void revokeUser(
			@ApiParam(required = true, example = "Sample_Added_User_ID_Swagger") @PathVariable("userId") String uid)
			throws DoesNotExistException {
		List<Key> toDelete = keyRepository.findAllByUserId(uid);
		if (toDelete.isEmpty()) {
			throw new DoesNotExistException();
		}
		for (Key key : toDelete) {
			keyRepository.removeKey(key);
		}
	}

	// void revokeAgency(String agencyCode)
	// Delete all keys with the passed agency code.
	// Returns Status.OK if successful, Status.NOT_FOUND otherwise
	@RequestMapping(value = "agency/{agencyCode:.+}", method = RequestMethod.DELETE)
	@ApiOperation(value = "Remove all keys for a specific agency from the keystore", notes = "Removes all keys from the keystore that match the provided agency code.")
	@ResponseStatus(HttpStatus.OK)
	public void revokeAgency(
			@ApiParam(required = true, example = "Sample_Agency") @PathVariable("agencyCode") String agencyCode)
			throws DoesNotExistException {
		List<Key> toDelete = keyRepository.findAllByAgencyCode(agencyCode);
		if (toDelete.isEmpty()) {
			throw new DoesNotExistException();
		}
		for (Key key : toDelete) {
			keyRepository.removeKey(key);
		}
	}

	// HttpEntity<String> authenticateToken(String authValue)
	// Finds a specific key by the auth token value.
	// Returns:
	// Status.OK if the key is found and the tokens are not expired,
	// Status.UNAUTHORIZED if the auth token is expired but
	// the ref token is still valid,
	// Status.NOT_FOUND if it isn't found, and
	// Status.GONE and deletes the key from the repository if the key is
	// found but both tokens are expired.
	@RequestMapping(value = "/auth/{authValue}", method = RequestMethod.GET, produces = "application/json")
	@ApiOperation(value = "Authenticate a passed-in auth token value", notes = "Returns a status code corresponding to the state of the passed-in auth token: 200 for valid, 404 for not found, 401 for expired, 410 for invalid/completely expired (410 also removes the key from the keystore).")
	public @ResponseBody HttpEntity<String> authenticateToken(
			@ApiParam(required = true) @PathVariable("authValue") String authValue)
			throws TokenDoesNotExistException, KeyExpiredException, ATExpiredException {

		Key key = keyRepository.findByAuthTokenValue(authValue);
		if (key == null) {
			throw new TokenDoesNotExistException();
		}

		if (key.getAuthToken().isExpired() && (key.getRefToken() == null || (key.getRefToken().isExpired()))) {
			keyRepository.removeKey(key);
			throw new KeyExpiredException();
		}

		if (key.getAuthToken().isExpired() && !key.getRefToken().isExpired()) {
			throw new ATExpiredException();
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	// void status()
	// Status check endpoint.
	// Returns Status.OK
	@RequestMapping(value = "status", method = RequestMethod.GET)
	@ApiOperation(hidden = true, value = "Status check endpoint.")
	@ResponseStatus(HttpStatus.OK)
	public HttpEntity<String> status() {
		logger.debug("Status check!");
		return new ResponseEntity<String>(HttpStatus.OK);
	}
}
