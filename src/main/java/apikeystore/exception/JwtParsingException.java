package apikeystore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "JWT encoding/decoding error; please check that your JWT is valid, correctly signed and not expired.")
public class JwtParsingException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6820452120207504699L;

}
