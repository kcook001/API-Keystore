package apikeystore.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Key Request", description = "Payload sent to add a key to the keystore")
public class KeyRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4649909483337628494L;

	@ApiModelProperty(value = "User ID", allowEmptyValue = false, example = "Sample_User_ID_Sw")
	@JsonProperty("userId")
	private final String userId;

	@ApiModelProperty(value = "Client ID", allowEmptyValue = false, example = "Sample_Client_ID_Sw")
	@JsonProperty("clientId")
	private final String clientId;

	@ApiModelProperty(value = "Scope")
	@JsonProperty("scope")
	private final Set<Resource> scope;

	@ApiModelProperty(value = "Additional attributes")
	@JsonProperty("attributes")
	private final Map<String, String> attributes;

	@JsonCreator
	public KeyRequest(@JsonProperty("userId") String userId, @JsonProperty("clientId") String clientId,
			@JsonProperty("scope") Set<Resource> scope, @JsonProperty("attributes") Map<String, String> attributes) {
		this.userId = userId;
		this.clientId = clientId;

		this.scope = new HashSet<Resource>();
		this.scope.addAll(scope);

		this.attributes = new HashMap<String, String>();
		this.attributes.putAll(attributes);
	}

	public KeyRequest() {
		this.userId = null;
		this.clientId = null;
		this.scope = null;
		this.attributes = null;
	}

	public String getUserId() {
		return userId;
	}

	public String getClientId() {
		return clientId;
	}

	public Set<Resource> getScope() {
		return scope;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

}
