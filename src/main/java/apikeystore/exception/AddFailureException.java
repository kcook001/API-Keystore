package apikeystore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Add operation failed, transaction aborted.")
public class AddFailureException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3446993732321141404L;

}
