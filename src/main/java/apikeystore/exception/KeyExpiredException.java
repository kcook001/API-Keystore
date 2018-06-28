package apikeystore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.GONE, reason = "Requested access token is expired and has been removed.")
public class KeyExpiredException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8458142112948861010L;

}
