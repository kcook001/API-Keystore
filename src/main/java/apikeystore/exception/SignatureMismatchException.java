package apikeystore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "The signature on the provided JWT does not match; validity cannot be asserted, so the token cannot be trusted.")
public class SignatureMismatchException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8927901135221525029L;

}
