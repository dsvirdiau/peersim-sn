package example.sn.cyclon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import example.cyclon.CyclonEntry;
import example.cyclon.CyclonMessage;
import example.newscast.NodeEntry;
import example.sn.linkable.LinkableSN;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

public class CyclonSN extends LinkableSN implements EDProtocol, CDProtocol
{
	private static final String PAR_CACHE = "cache";
	private static final String PAR_L = "l";
	private static final String PAR_TRANSPORT = "transport";
	private static final String PAR_IDLE = "idle";
	private static final long TIMEOUT = 2000;

	private int size;
	private final int l;
	private final int tid;
	private final int idle;

	private List<CyclonEntry> cache = null;

	public CyclonSN(String n)
	{
		this.l = Configuration.getInt(n + "." + PAR_L);
		this.tid = Configuration.getPid(n + "." + PAR_TRANSPORT);
		this.idle = Configuration.getPid(n + "." + PAR_IDLE);

		this.size = Configuration.getInt(n + "." + PAR_CACHE);
		cache = new ArrayList<CyclonEntry>(size);
	}

	//-------------------------------------------------------------------
	private void increaseAgeAndSort()
	{
		//for (CyclonEntry ce : cache)
		//	ce.increase();

		Collections.sort(cache, new CyclonEntry());
	}

	private CyclonEntry selectNeighbor()
	{
		try{			
			int i = cache.size()-1;

			if (cache.get(i).removed && (CommonState.getTime() - cache.get(i).timeRemoved) >= TIMEOUT){
				cache.remove(i);
				i--;
			}

			while (cache.get(i).removed)
				i--;
			return cache.get(i);
		} catch (Exception e){
			return null;
		}
	}

	/**
	 * Return a list with all the friends of rnode I know up in the network
	 * 
	 * @param rnode
	 * @return
	 */
	private List<CyclonEntry> initList(Node lnode, Node rnode)
	{
		Linkable lNodeIdleProtocol = (Linkable)lnode.getProtocol(idle);
		boolean isRNodeFriend = lNodeIdleProtocol.contains(rnode);

		Linkable rNodeIdleProtocol = (Linkable)rnode.getProtocol(idle);

		List<CyclonEntry> list = new ArrayList<CyclonEntry>();
		for (CyclonEntry ce : cache){
			if (ce.removed && (CommonState.getTime() - ce.timeRemoved) >= TIMEOUT){
				//System.out.println("TIMEOUT " + CommonState.getTime());
				ce.reuseNode();
			}
			//ce.n is a friend of rNode or rNode is a friend of mine and ce.n is my friend
			if ((rNodeIdleProtocol.contains(ce.n) || (isRNodeFriend && lNodeIdleProtocol.contains(ce.n))) && !ce.removed)
				list.add(ce);
		}

		return list;
	}

	/**
	 * Send to rnode the list of nodes I know that are also
	 * its friends
	 * 
	 * @param l
	 * @param rnode
	 * @return
	 */
	private List<CyclonEntry> selectNeighbors(int l, Node lnode, Node rnode, boolean selectedAtRequest)
	{
		int dim = Math.min(l, cache.size());
		List<CyclonEntry> list = initList(lnode, rnode);

		while (list.size() > dim)
			list.remove(CommonState.r.nextInt(list.size()));

		for (CyclonEntry ce : list)
			ce.removeNode(rnode, selectedAtRequest);

		return list;
	}

	private List<CyclonEntry> discardEntries(Node n, List<CyclonEntry> list)
	{
		int index = 0;
		List<CyclonEntry> newList = new ArrayList<CyclonEntry>();
		for (CyclonEntry ce : list)
			if (!ce.n.equals(n) && (index = indexOf(ce.n)) < 0)
				newList.add(ce);
			//Duplicate, take the newest one
			else if (index >= 0)
				cache.get(index).age = Math.max(ce.age, cache.get(index).age);

		return newList;
	}

	private int getFirstDeleted(Node rnode, boolean selectedAtRequest)
	{
		for (int i = cache.size()-1 ; i >= 0; i--)
			if (cache.get(i).removed && cache.get(i).nodeSended.equals(rnode) && cache.get(i).selectedAtRequest == selectedAtRequest)
				return i;

		return -1;
	}

	private int indexOf(Node rnode)
	{
		for (int i = cache.size()-1; i >= 0; i--)
			if (cache.get(i).n.equals(rnode))
				return i;

		return -1;
	}

	private void insertReceivedItems(List<CyclonEntry> list, Node rnode, boolean selectedAtRequest)
	{
		if (list.isEmpty()){
			//			System.err.println("Empty " + cache.size() + " " + 
			//					CommonState.getNode().getID() + " " + ((SNNode)CommonState.getNode()).getRealID() + " " +
			//					rnode.getID() + " " + ((SNNode)rnode).getRealID());
			if (selectedAtRequest)
				cache.remove(indexOf(rnode));
			return;
		}

		if (selectedAtRequest){
			try{
				cache.set(indexOf(rnode) , new CyclonEntry(list.remove(0)));
			} catch (Exception e){
				System.err.println(CommonState.getNode().getID() + " " + rnode.getID());
				e.printStackTrace();
			}
		}

		for (CyclonEntry ce : list){
			// firstly using empty cache slots
			if (cache.size() < size)
				cache.add(new CyclonEntry(ce.n, ce.age));
			// secondly replacing entries among the ones sent to rnode
			else{
				int index = getFirstDeleted(rnode, selectedAtRequest);
//				if (index < 0){
//					System.err.println("PROBLEM " + CommonState.getNode().getID() + " " + cache.size() + " " + rnode.getID());
//					return;
//				}
				if (index >= 0)
					cache.set(index, new CyclonEntry(ce.n, ce.age));
			}
		}

		for (CyclonEntry ce : cache){
			if (ce.nodeSended != null && ce.nodeSended.equals(rnode) && ce.selectedAtRequest == selectedAtRequest){
				ce.reuseNode();
			}
		}
	}
	//-------------------------------------------------------------------


	public Object clone()
	{
		CyclonSN cyclon = null;
		try { cyclon = (CyclonSN) super.clone(); }
		catch( CloneNotSupportedException e ) {} // never happens
		cyclon.cache = new ArrayList<CyclonEntry>();

		return cyclon;
	}

	public boolean addNeighbor(Node neighbour)
	{
		if (contains(neighbour))
			return false;

		if (cache.size() >= size)
			return false;

		CyclonEntry ce = new CyclonEntry(neighbour, 0);
		cache.add(ce);

		return true;
	}

	public boolean contains(Node neighbour)
	{
		for (CyclonEntry ne : cache)
			if (ne.n.equals(neighbour))
				return true;

		return false;
	}

	public int degree()
	{
		return cache.size();
	}

	public Node getNeighbor(int i)
	{
		return cache.get(i).n;
	}

	public void pack() {}

	public void onKill() {}

	public void processEvent(Node node, int pid, Object event)
	{
		CyclonMessage message = (CyclonMessage) event;

		List<CyclonEntry> nodesToSend = null;
		if (message.isResuest){
			nodesToSend = selectNeighbors(message.list.size(), node, message.node, false);

			CyclonMessage msg = new CyclonMessage(node, nodesToSend, false);
			Transport tr = (Transport) node.getProtocol(tid);
			tr.send(node, message.node, msg, pid);

//			if (node.getID() == 2387){
//				System.err.println("------------------ SEND ---------------------------");
//				System.err.println(node.getID() + "<-" + message.node.getID() + " " + cache.size() + " " + nodesToSend.size());
//				for (CyclonEntry ce1 : nodesToSend){
//					if (ce1.removed)
//						System.err.print(ce1.n.getID() + "," + ce1.nodeSended.getID() + " " + ce1.removed + " ");
//				}
//				System.err.println("\n---------------------------------------------------");
//			}
		}

		// 5. Discard entries pointing to P, and entries that are already in P’s cache.
		List<CyclonEntry> list = discardEntries(node, message.list);

//		if (node.getID() == 2387){
//			System.err.println("------------------ AFDISCARD ----------------------");
//			System.err.println(message.isResuest + " " + node.getID() + " " + message.node.getID() + " " + cache.size() + " " + message.list.size() + " " + size);
//			for (CyclonEntry ce1 : cache){
//				if (ce1.removed)
//					System.err.print(ce1.n.getID() + "," + ce1.nodeSended.getID() + " " + ce1.removed + " ");
//			}
//			System.err.println("\n---------------------------------------------------");
//		}

		// 6. Update P’s cache to include all remaining entries, by firstly using empty
		//    cache slots (if any), and secondly replacing entries among the ones originally
		//    sent to Q.
		insertReceivedItems(list, message.node, !message.isResuest);

//		if (node.getID() == 2387){
//			System.err.println("------------------ END ----------------------------");
//			System.err.println(node.getID() + " " + message.node.getID() + " " + cache.size());
//			for (CyclonEntry ce1 : cache){
//				if (ce1.removed)
//					System.err.print(ce1.n.getID() + "," + ce1.nodeSended.getID() + " " + ce1.removed + " ");
//			}
//			System.err.println("\n---------------------------------------------------");
//		}

		// 1. Increase by one the age of all neighbors.
		increaseAgeAndSort();
	}

	public void nextCycle(Node node, int protocolID)
	{
		// 1. Increase by one the age of all neighbors.
		//increaseAgeAndSort();

		// 2. Select neighbor Q with the highest age among all neighbors...
		CyclonEntry ce = selectNeighbor();
		if (ce == null){
			//System.err.println("No Peer");
			return;
		}
		ce.removeNode(ce.n, true);

		//    and l − 1 other random neighbors.
		List<CyclonEntry> nodesToSend = selectNeighbors(l-1, node, ce.n, true);

		// 3. Replace Q’s entry with a new entry of age 0 and with P’s address.
		nodesToSend.add(0, new CyclonEntry(node, CommonState.getTime()));

		// 4. Send the updated subset to peer Q.
		CyclonMessage message = new CyclonMessage(node, nodesToSend, true);
		Transport tr = (Transport) node.getProtocol(tid);
		tr.send(node, ce.n, message, protocolID);
	}

	@Override
	public boolean containsAsFriend(Node lnode, Node n) {
		LinkableSN linkable = (LinkableSN)lnode.getProtocol(idle);
		return linkable.containsAsFriend(lnode, n);
	}

	@Override
	public Node getFriendPeer(Node lnode, Node n)
	{		
		//LinkableSN lLinkable = (LinkableSN)lnode.getProtocol(idle);
		LinkableSN rLinkable = (LinkableSN)n.getProtocol(idle);
		Set<Node> set = new HashSet<Node>();
		
		/*//n is my friend
		if (lLinkable.containsAsFriend(lnode, n)){	
			//search in cache all the friends I know to be up
			for (CyclonEntry ce : cache)
				if (rLinkable.contains(n))
					list.add(ce.n);
		}
		else{
			for (CyclonEntry ce : cache){
				rLinkable = (LinkableSN)ce.n.getProtocol(idle);
				if (rLinkable.contains(n))
					list.add(ce.n);
			}
		}*/
		
		for (CyclonEntry ce : cache)
			//lnode is a friend of n of ce.n is a friend of n
			if (rLinkable.containsAsFriend(n, lnode) || ((LinkableSN)ce.n.getProtocol(idle)).containsAsFriend(lnode, n))
				set.add(ce.n);
		
		if (set.size() > 0)
			return set.toArray(new Node[0])[CommonState.r.nextInt(set.size())];
		
		return null;
	}

	@Override
	public NodeEntry[] getFriends(Node lnode, Node n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node[] getNodes(Node node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node getPeer(Node node)
	{
		try{
			return cache.get(CommonState.r.nextInt(cache.size())).n;
		} catch (Exception e){
			return null;
		}
	}
}