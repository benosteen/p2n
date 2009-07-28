class Keypair {
	String access_id = "";
	String key = "";
	public Keypair(String access_id, String key) {
		this.access_id = access_id;
		this.key = key;
	}

	public String get_access_id() {
		return access_id;
	}

	public String get_private_key() {
		return key;
	}

}
