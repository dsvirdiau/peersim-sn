package example.cyclon;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

public class Cyclon implements Linkable, EDProtocol, CDProtocol
{
	private static final String PAR_CACHE = "cache";
	private static final String PAR_L = "l";
	private static final String PAR_TRANSPORT = "transport";

	private final int size;
	private final int l;
	private final int tid;

	private List<CyclonEntry> cache = null;

	public Cyclon(String n)
	{
		this.size = Configuration.getInt(n + "." + PAR_CACHE);
		this.l = Configuration.getInt(n + "." + PAR_L);
		this.tid = Configuration.getPid(n + "." + PAR_TRANSPORT);

		cache = new ArrayList<CyclonEntry>(size);
	}

	//-------------------------------------------------------------------
	private void increaseAgeAndSort()
	{
		//for (CyclonEntry ce : cache)
		//	ce.increase();

		Collections.sort(cache, new CyclonEntry());
	}

	private Node selectNeighbor()
	{
		try{
			return cache.remove(cache.size()-1).n;
		} catch (Exception e){
			return null;
		}
	}

	private List<CyclonEntry> selectNeighbors(int l, Node rnode)
	{
		int dim = Math.min(l, cache.size()-1);
		List<CyclonEntry> list = new ArrayList<CyclonEntry>(l);

		List<Integer> tmp = new ArrayList<Integer>();
		for (int i = 0; i < cache.size()-1; i++)
			if (!cache.get(i).removed)
				tmp.add(i);
			else if ((CommonState.getTime() - cache.get(i).timeRemoved) >= 5000){
				cache.get(i).removed = false;
				cache.get(i).nodeSended = null;
				cache.get(i).timeRemoved = Long.MAX_VALUE;
				tmp.add(i);
				System.out.println("QUI");
			}

		int sup = Math.min(dim, tmp.size());
		for (int i = 0; i < sup; i++){
			CyclonEntry ce = cache.get((tmp.remove(CommonState.r.nextInt(tmp.size())).intValue()));
			ce.removed = true;
			ce.timeRemoved = CommonState.getTime();
			ce.nodeSended = rnode;
			list.add(ce);
		}
//
//		if (CommonState.getNode().getID() == 801){
//			for (CyclonEntry ce : list)
//				System.out.print(ce.n.getID() + " ");
//			System.out.println();
//			
//			for (CyclonEntry ce1 : cache){
//				System.err.print(ce1.n.getID() + " " + ce1.removed + " ");
//			}
//			System.err.println();
//		}
		
		return list;
	}

	private List<CyclonEntry> discardEntries(Node n, List<CyclonEntry> list)
	{
		List<CyclonEntry> newList = new ArrayList<CyclonEntry>();
		for (CyclonEntry ce : list){
			if (!ce.n.equals(n) && !contains(ce.n))
				newList.add(ce);
		}

		return newList;
	}

	private int getFirstDeleted(Node rnode)
	{
		for (int i = cache.size()-1 ; i >= 0; i--)
			if (cache.get(i).removed && cache.get(i).nodeSended.equals(rnode))
				return i;

		return -1;
	}

	private void insertReceivedItems(List<CyclonEntry> list, Node rnode)
	{
		for (CyclonEntry ce : list){
			//firstly using empty cache slots
			if (cache.size() < size)
				cache.add(new CyclonEntry(ce.n, ce.age));
			//secondly replacing entries among the ones sent to Q
			else{
				int index = getFirstDeleted(rnode);
				if (index < 0){
					System.err.println("PROBLEM " + CommonState.getNode().getID() + " " + cache.size() + " " + rnode.getID());
					for (CyclonEntry ce1 : cache){
						System.err.print(ce1.n.getID() + " " + ce1.removed + " ");
					}
					System.err.println();
					return;
				}
				//cache.set(indexOf(sentList.remove(index)), new CyclonEntry(ce.n, ce.age));
				cache.set(index, new CyclonEntry(ce.n, ce.age));
			}
		}
		

//		if (CommonState.getNode().getID() == 801){
//			for (CyclonEntry ce : list)
//				System.out.print(ce.n.getID() + " ");
//			System.out.println();
			
//			for (CyclonEntry ce1 : cache){
//				System.err.print(ce1.n.getID() + " " + ce1.removed + " ");
//			}
//			System.err.println();
//		}

		for (CyclonEntry ce : cache){
			if (ce.nodeSended != null && ce.nodeSended.equals(rnode)){
				ce.nodeSended = null;
				ce.removed = false;
				ce.timeRemoved = Long.MAX_VALUE;
			}
		}
		
//		if (CommonState.getNode().getID() == 801){
////			for (CyclonEntry ce : list)
////				System.out.print(ce.n.getID() + " ");
////			System.out.println();
//			
//			for (CyclonEntry ce1 : cache){
//				System.err.print(ce1.n.getID() + " " + ce1.removed + " ");
//			}
//			System.err.println();
//		}

		/*//Add received items to the cache
		insertItems(list);

		if (cache.size()-1 >= size)
			return;

		//I've received less data that one I've sent, so
		//reuse some sent entries
		int cacheSize = cache.size();
		int sListSize = sList.size();
		for (int i = 0; i < Math.min(size-cacheSize-1, sListSize-1); i++){
			int pos = CommonState.r.nextInt(sList.size());
			CyclonEntry ce = sList.remove(pos);
			cache.add(ce);
		}*/
	}
	//-------------------------------------------------------------------


	public Object clone()
	{
		Cyclon cyclon = null;
		try { cyclon = (Cyclon) super.clone(); }
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

		CyclonEntry ce = new CyclonEntry(neighbour, CommonState.getTime());
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

	public void pack() {
		// TODO Auto-generated method stub

	}

	public void onKill() {}

	public void processEvent(Node node, int pid, Object event)
	{
		CyclonMessage message = (CyclonMessage) event;

		List<CyclonEntry> nodesToSend = null;
		if (message.isResuest){
			nodesToSend = selectNeighbors(message.list.size(), message.node);

			CyclonMessage msg = new CyclonMessage(node, nodesToSend, false, message.list);
			Transport tr = (Transport) node.getProtocol(tid);
			tr.send(node, message.node, msg, pid);
			
			if (CommonState.getNode().getID() == 801){
				System.out.println("CONTACTED");
			}
		}
		//else
			//nodesToSend = message.sentList;


		// 5. Discard entries pointing to P, and entries that are already in P’s cache.
		List<CyclonEntry> list = discardEntries(node, message.list);

		// 6. Update P’s cache to include all remaining entries, by firstly using empty
		//    cache slots (if any), and secondly replacing entries among the ones originally
		//    sent to Q.
		insertReceivedItems(list, message.node);

		// 1. Increase by one the age of all neighbors.
		increaseAgeAndSort();
	}

	public void nextCycle(Node node, int protocolID)
	{
		// 1. Increase by one the age of all neighbors.
		//increaseAgeAndSort();

		// 2. Select neighbor Q with the highest age among all neighbors...
		Node q = selectNeighbor();
		if (q == null){
			System.err.println("No Peer");
			return;
		}
		//    and l − 1 other random neighbors.
		List<CyclonEntry> nodesToSend = selectNeighbors(l-1, q);

		// 3. Replace Q’s entry with a new entry of age 0 and with P’s address.
		nodesToSend.add(0, new CyclonEntry(node, CommonState.getTime()));

		// 4. Send the updated subset to peer Q.
		CyclonMessage message = new CyclonMessage(node, nodesToSend, true, null);
		Transport tr = (Transport) node.getProtocol(tid);
		tr.send(node, q, message, protocolID);
//		if (CommonState.getNode().getID() == 801){
//			System.out.println("INITIATOR");
//		}
	}
}
