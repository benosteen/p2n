class PSNNode {
	String node_id = "";
	String node_url = "";
	String url_base = "";
	int allocated_space = 0;

	public PSNNode(String node_url) {
		this.node_url = node_url;
	}

	public void set_node_id(String node_id) {
		this.node_id = node_id;
	}

	public String get_node_id() {
		return node_id;
	}

	public void set_node_url(String node_url) {
		this.node_url = node_url;
	}

	public String get_node_url() {
		return node_url;
	}

	public void set_url_base(String url_base) {
		this.url_base = url_base;
	}

	public String get_url_base() {
		return url_base;
	}

	public void set_allocated_space(int allocated_space) {
		this.allocated_space = allocated_space;
	}

	public int get_allocated_space() {
		return allocated_space;
	}
}
