import java.util.*;

class PSNNodeSelector {
	
	DatabaseConnector_Mysql dbm;

	public PSNNodeSelector(DatabaseConnector_Mysql dbm) {
		this.dbm = dbm;
	}

	public PSNNode getNode(Vector nodes_done) {
		Vector active_nodes = dbm.getActiveNodes();
		for (int i=0;i<active_nodes.size();i++) {
			boolean done_flag = false;
			PSNNode in = (PSNNode)active_nodes.get(i);
			String in_url = in.get_node_url();
			System.out.println("DONE NODES SIZE : " + nodes_done.size());
			for (int j=0;j<nodes_done.size();j++) {
				PSNNode done = (PSNNode)nodes_done.get(j);
				String done_url = done.get_node_url();
				if (done_url.equals(in_url)) {
					done_flag = true;
				}
			}
			if (!done_flag) {
				return in;
			} 
		}
		return null;
	}


}
