package apikeystore.model;

import java.util.HashSet;
import java.util.Set;

public class Resource {
	private String resource;
	private Set<String> verbs;

	public Resource() {
		resource = null;
		verbs = new HashSet<>();
	}

	public Resource(String resource, Set<String> verbs) {
		this.resource = resource;
		this.verbs = verbs;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public Set<String> getVerbs() {
		return verbs;
	}

	public void setVerbs(Set<String> verbs) {
		this.verbs = verbs;
	}

	public String toString() {
		String str = "{ resource: " + resource + ", verbs: [ ";
		for (String v : verbs)
			str = str.concat(v + ", ");
		if (str.endsWith(", "))
			str = str.substring(0, str.length() - 2);
		str = str.concat(" ] }");
		return str;
	}

	@Override
	public int hashCode() {
		int hashCode = verbs.hashCode();
		hashCode = 31 * hashCode + resource.hashCode();
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof Resource)) {
			return false;
		}

		Resource resource = (Resource) obj;
		if (this.resource.equals(resource.resource) && this.verbs.equals(resource.verbs)) {
			return true;
		}

		return false;
	}
}
