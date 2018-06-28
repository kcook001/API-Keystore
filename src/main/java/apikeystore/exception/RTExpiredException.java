package apikeystore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Could not refresh OAuth tokens; the refresh token has expired.")
public class RTExpiredException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1369676230805153578L;

}
