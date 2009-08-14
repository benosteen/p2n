class PSNObject {

	String access_id = null;	
	String requested_path = null;
	String uuid = null;
	boolean local_copy = false;
	boolean psn_copy = false;
	int psn_distribution = 0;
	int psn_resiliance = 0;
	String acl = null;
	String local_path = null;
	String md5_sum = null;
	String mime_type = null;

	public PSNObject() {
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
	}

	public String getUUID() {
		return uuid;
	}

	public void setAccessId(String access_id) {
		this.access_id = access_id;
	}
	
	public String getAccessId() {
		return access_id;
	}

	public void setRequestedPath(String requested_path) {
		this.requested_path = requested_path;
	}

	public String getRequestedPath() {
		return requested_path;
	}

	public void setLocalCopy(boolean local_copy) {
		this.local_copy = local_copy;
	}

	public boolean getLocalCopy() {
		return local_copy;
	}

	public void setPSNCopy(boolean psn_copy) {
		this.psn_copy = psn_copy;
	}

	public boolean getPSNCopy() {
		return psn_copy;
	}

	public void setPSNDistribution(int dist) {
		this.psn_distribution = dist;
	}

	public int getPSNDistribution() {
		return psn_distribution;
	}

	public void setPSNResiliance(int res) {
		this.psn_resiliance = res;
	}

	public int getPSNResiliance() {
		return psn_resiliance;
	}

	public void setACL(String acl) {
		this.acl = acl;
	}

	public String getACL() {
		return acl;
	}

	public void setLocalPath(String path) {
		this.local_path = path;
	}

	public String getLocalPath() {
		return local_path;
	}

	public void setMD5Sum(String sum) {
		this.md5_sum = sum;
	}

	public String getMD5Sum() {
		return md5_sum;
	}

	public void setMimeType(String type) {
		this.mime_type = type;
	}

	public String getMimeType() {
		return mime_type;
	}

}
