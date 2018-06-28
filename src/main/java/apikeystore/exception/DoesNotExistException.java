package apikeystore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Key with provided value does not exist")
public class DoesNotExistException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8855213053645838143L;

}
