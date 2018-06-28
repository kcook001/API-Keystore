package apikeystore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "One or more query parameters is not recognized.")
public class BadParameterException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4780182969184504015L;

}
