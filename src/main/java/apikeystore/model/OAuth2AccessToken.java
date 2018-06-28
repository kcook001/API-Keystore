package apikeystore.model;

import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.mongodb.core.index.Indexed;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "OAuth2 Access Token", description = "Value, scope and expiration of the OAuth2 access token.")
public class OAuth2AccessToken {

	@ApiModelProperty(value = "Access Token Value", allowEmptyValue = false, example = "Sample_Access_Token_Value_Sw")
	@Indexed
	private final String value;

	@ApiModelProperty(value = "Access Token Expiration", allowEmptyValue = false, example = "1590000000")
	private final long expiration;

	private static final long lifetimeSeconds = 86400;

	@ApiModelProperty(hidden = true)
	private final static String tokenType = "BEARER";

	@ApiModelProperty(value = "Access Token Scope")
	private Set<Resource> scope;

	public OAuth2AccessToken() {
		value = null;
		expiration = 0;
		scope = null;
	}

	public OAuth2AccessToken(String userId, Set<Resource> scope) {
		Base64.Encoder encoder = Base64.getEncoder();

		value = (new String(
				encoder.encode((("at" + userId + ((new Date().getTime()) / 1000) + UUID.randomUUID()).getBytes()))))
						.replace("=", "").replace("/", "");
		expiration = (((new Date().getTime()) / 1000) + lifetimeSeconds);
		if (scope == null) {
			this.scope = null;
		} else {
			this.scope = new HashSet<Resource>();
			this.scope.addAll(scope);
		}
	}

	public OAuth2AccessToken(String value, long expiration, Set<Resource> scope) {
		this.value = value;
		this.expiration = expiration;
		if (scope == null) {
			this.scope = null;
		} else {
			this.scope = new HashSet<Resource>();
			this.scope.addAll(scope);
		}
	}

	public OAuth2AccessToken(OAuth2AccessToken source) {
		this.value = source.getValue();
		this.expiration = source.getExpiration();
		if (source.getScope() == null) {
			this.scope = null;
		} else {
			this.scope = new HashSet<Resource>();
			this.scope.addAll(source.getScope());
		}
	}

	public String getValue() {
		return value;
	}

	public String getTokenType() {
		return tokenType;
	}

	public long getExpiration() {
		return expiration;
	}

	public Set<Resource> getScope() {
		return scope;
	}

	public void setScope(Set<Resource> scope) {
		if (this.scope == null)
			this.scope = new HashSet<Resource>();
		this.scope.clear();
		this.scope.addAll(scope);
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

		if (!(obj instanceof OAuth2AccessToken)) {
			return false;
		}

		OAuth2AccessToken token = (OAuth2AccessToken) obj;
		if (scopeComp(this.scope, token.scope) && this.value.equals(token.value)
				&& this.expiration == token.expiration) {
			return true;
		}

		return false;
	}

	private boolean scopeComp(Set<Resource> sc1, Set<Resource> sc2) {

		if (sc1.size() != sc2.size())
			return false;

		for (Resource r : sc1)
			if (!sc2.contains(r))
				return false;

		for (Resource r : sc2)
			if (!sc1.contains(r))
				return false;

		return true;
	}
}
