package apikeystore.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Key", description = "User OAuth credentials information.")
@Document(collection = "apikeystore")
public class Key implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2470946023554206502L;

	private OAuth2AccessToken authToken;

	private OAuth2RefreshToken refToken;

	@Id
	@ApiModelProperty(hidden = true)
	@JsonIgnore
	private final String id;

	@ApiModelProperty(value = "User ID", allowEmptyValue = false, example = "Sample_User_ID_Sw")
	private final String userId;

	@ApiModelProperty(value = "Client ID", allowEmptyValue = false, example = "Sample_Client_ID_Sw")
	private final String clientId;

	@ApiModelProperty(value = "Additional attributes")
	private Map<String, String> attributes;

	@ApiModelProperty(value = "Created timestamp")
	private final long created;

	@ApiModelProperty(value = "Modified timestamp")
	private long modified;

	@JsonCreator
	public Key(@JsonProperty("authToken") OAuth2AccessToken authToken,
			@JsonProperty("refToken") OAuth2RefreshToken refToken, @JsonProperty("userId") String userId,
			@JsonProperty("clientId") String clientId, @JsonProperty("created") long created,
			@JsonProperty("attributes") Map<String, String> attributes) {
		this.authToken = new OAuth2AccessToken(authToken);
		this.refToken = new OAuth2RefreshToken(refToken.getValue(), refToken.getExpiration());
		this.userId = userId;
		this.clientId = clientId;
		this.id = userId + "__" + clientId;
		if (attributes == null) {
			this.attributes = null;
		} else {
			this.attributes = new HashMap<String, String>();
			this.attributes.putAll(attributes);
		}
		if (created < 0) {
			this.created = (new Date().getTime() / 1000);
			this.modified = this.created;
		} else {
			this.created = created;
			this.modified = (new Date().getTime() / 1000);
		}
	}

	public Key(Key toAdd) {
		this.authToken = new OAuth2AccessToken(toAdd.authToken);
		this.refToken = new OAuth2RefreshToken(toAdd.getRefToken().getValue(), toAdd.getRefToken().getExpiration());
		this.userId = toAdd.getUserId();
		this.clientId = toAdd.getClientId();
		this.id = toAdd.getUserId() + "__" + toAdd.getClientId();
		if (toAdd.getAttributes() == null) {
			this.attributes = null;
		} else {
			this.attributes = new HashMap<String, String>();
			this.attributes.putAll(toAdd.getAttributes());
		}
		if (toAdd.getCreated() < 0) {
			this.created = (new Date().getTime() / 1000);
			this.modified = this.created;
		} else {
			this.created = toAdd.getCreated();
			this.modified = (new Date().getTime() / 1000);
		}
	}

	public Key() {
		this.authToken = new OAuth2AccessToken();
		this.refToken = new OAuth2RefreshToken();
		this.userId = null;
		this.clientId = null;
		this.id = null;
		this.attributes = null;
		this.created = (new Date().getTime() / 1000);
		this.modified = this.created;
	}

	public OAuth2AccessToken getAuthToken() {
		return authToken;
	}

	public void setAuthToken(OAuth2AccessToken token) {
		this.authToken = new OAuth2AccessToken(token);
	}

	public String getUserId() {
		return userId;
	}

	public String getClientId() {
		return clientId;
	}

	public String getId() {
		return id;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		if (this.attributes == null)
			this.attributes = new HashMap<String, String>();
		this.attributes.clear();
		this.attributes.putAll(attributes);
	}

	public long getCreated() {
		return created;
	}

	public long getModified() {
		return modified;
	}

	public void setModified(long modified) {
		this.modified = modified;
	}

	public OAuth2RefreshToken getRefToken() {
		return refToken;
	}

	public void setRefToken(OAuth2RefreshToken token) {
		this.refToken = new OAuth2RefreshToken(token);
	}

	@Override
	public String toString() {
		return "Key [authToken=" + this.authToken.getValue() + ", ATexp=" + this.authToken.getExpiration() + ", scopes="
				+ this.authToken.getScope().toString() + ", refreshToken=" + this.refToken.getValue() + ", RTexp="
				+ this.refToken.getExpiration() + ", userId=" + userId + ", clientId=" + clientId + ", created="
				+ this.created + ", modified=" + this.modified + "]";
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Key)) {
			return false;
		}

		Key key = (Key) obj;
		if (this.userId.equals(key.getUserId()) && this.clientId.equals(key.getClientId())
				&& this.attributes.equals(key.attributes) && this.authToken.equals(key.getAuthToken())
				&& this.refToken.equals(key.getRefToken())) {
			return true;
		}

		return false;
	}
}
