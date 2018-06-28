package apikeystore.model;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.springframework.data.mongodb.core.index.Indexed;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "OAuth2 Refresh Token", description = "Value and expiration of the OAuth2 refresh token.")
public class OAuth2RefreshToken {

	@ApiModelProperty(value = "Refresh Token Value", allowEmptyValue = false, example = "Sample_Refresh_Token_Value_Sw")
	@Indexed
	private final String value;

	@ApiModelProperty(value = "Refresh Token Expiration", allowEmptyValue = false, example = "1590000000")
	private final long expiration;

	private static final long lifetimeSeconds = 604800;

	public OAuth2RefreshToken() {
		this.value = null;
		this.expiration = 0;
	}

	public OAuth2RefreshToken(String userId) {
		Base64.Encoder encoder = Base64.getEncoder();

		value = (new String(
				encoder.encode((("ref" + userId + (new Date().getTime()) / 1000 + UUID.randomUUID()).getBytes()))))
						.replace("=", "").replace("/", "");
		expiration = (((new Date().getTime()) / 1000) + lifetimeSeconds);
	}

	public OAuth2RefreshToken(String value, long expiration) {
		this.value = value;
		this.expiration = expiration;
	}

	public OAuth2RefreshToken(OAuth2RefreshToken source) {
		this.value = source.getValue();
		this.expiration = source.getExpiration();
	}

	public String getValue() {
		return value;
	}

	public long getExpiration() {
		return expiration;
	}

	public Boolean isExpired() {
		Date now = new Date();
		if ((now.getTime() / 1000) > expiration) {
			return true;
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof OAuth2RefreshToken)) {
			return false;
		}

		OAuth2RefreshToken token = (OAuth2RefreshToken) obj;
		if (this.value.equals(token.value) && this.expiration == token.expiration) {
			return true;
		}

		return false;
	}
}
