package apikeystore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Access token is invalid.")
public class ATExpiredException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2740257484045313173L;

}
