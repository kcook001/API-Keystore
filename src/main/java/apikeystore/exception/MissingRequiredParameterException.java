package apikeystore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "The userId and clientId values may not be null or empty.")
public class MissingRequiredParameterException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7337282137052431923L;

}
