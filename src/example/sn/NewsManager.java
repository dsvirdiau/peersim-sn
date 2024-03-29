package example.sn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.stylesheets.LinkStyle;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

import example.sn.epidemic.message.News;
import example.sn.epidemic.message.NewsComment;
import example.sn.epidemic.message.NewsFriendship;
import example.sn.epidemic.message.NewsStatusChange;
import example.sn.linkable.LinkableSN;
import example.sn.newscast.NewscastED;

/**
 * Class to manage all news.
 * 
 * @author Roberto Zandonati
 *
 */
public class NewsManager implements EDProtocol
{
	private static final String PAR_IDLE_MANAGER = "idle";
	private static final String PAR_EPIDEMIC = "epidemic";
	
	private static final String PAR_PRINT_STATS = "stats";
	private static final String PAR_GOSSIP = "gossip";

	protected final int pidIdle;
	protected final int pidGossip;
	protected final int pidEpidemic;
	protected final boolean stats;

	private List<News> news = null;

	public NewsManager(String n)
	{
		this.pidIdle = Configuration.getPid(n + "." + PAR_IDLE_MANAGER);
		this.pidEpidemic = Configuration.getPid(n + "." + PAR_EPIDEMIC);
		
		this.pidGossip = Configuration.getPid(n + "." + PAR_GOSSIP, -1);
		this.stats = Configuration.getBoolean(n + "." + PAR_PRINT_STATS, true);
		
		this.news = new ArrayList<News>();
	}

	public Object clone()
	{
		NewsManager nm = null;
		try { nm = (NewsManager) super.clone(); }
		catch( CloneNotSupportedException e ) {} // never happens
		nm.news = new ArrayList<News>();

		return nm;
	}

	public boolean contains(News n)
	{
		return news.contains(n);
	}


	public void addNews(News news, Node n)
	{
		this.news.add(news);
		if (news instanceof NewsFriendship)
			((LinkableSN)n.getProtocol(pidIdle)).addNeighbor(Network.get(((NewsFriendship)news).getDestId()));

		((EpidemicNews)n.getProtocol(pidEpidemic)).setInfected(true);
	}

	/**
	 * 
	 * @return the whole list of messages
	 */
	public List<News> getNews()
	{
		return news;
	}

	private boolean isInterestingNews(News n, Node lnode, Node rnode)
	{
		LinkableSN lSourceNode =  (LinkableSN)n.getSourceNode().getProtocol(pidIdle);
		LinkableSN lDestNode =  (LinkableSN)n.getDestNode().getProtocol(pidIdle);
		LinkableSN linkable = null;

		if (n instanceof NewsComment || n instanceof NewsFriendship){
			//rnode amico di sourceNode o destNode
			if ((lSourceNode.containsAsFriend(null, rnode)) || (lDestNode.containsAsFriend(null, rnode)))
				return true;

			//rnode amico di un amico di sourceNode
			for (int i = 0; i < lSourceNode.degree(); i++){
				linkable =  (LinkableSN)lSourceNode.getNeighbor(i).getProtocol(pidIdle);
				if ((linkable.containsAsFriend(null, rnode)) || (linkable.containsAsFriend(null, rnode)))
					return true;
			}

			//rnode amico di un amico di destNode
			for (int i = 0; i < lDestNode.degree(); i++){
				linkable =  (LinkableSN)lDestNode.getNeighbor(i).getProtocol(pidIdle);
				if ((linkable.containsAsFriend(null, rnode)) || (linkable.containsAsFriend(null, rnode)))
					return true;
			}
		}
		else if (n instanceof NewsStatusChange)
			//rnode amico di sourceNode o destNode
			if ((lSourceNode.containsAsFriend(null, rnode)) || (lDestNode.containsAsFriend(null, rnode)))
				return true;

		return false;
	}
	
	public List<News> getNews(Node lnode, Node rnode)
	{		
		List<News> list = new ArrayList<News>();

		//		System.err.println(((SNNode)lnode).getRealID() + " " + ((SNNode)rnode).getRealID());

		for (News n: news)
			//			if (((LinkableSN)n.getSourceNode().getProtocol(pidIdle)).containsAsFriend(n.getSourceNode(), rnode) ||
			//((LinkableSN)n.getDestNode().getProtocol(pidIdle)).containsAsFriend(n.getSourceNode(), rnode))
			if (isInterestingNews(n, lnode, rnode)){
				list.add(n);
			}
		/*if (n.getNode().getID() == lnode.getID() || ((LinkableSN)lnode.getProtocol(pidNetworkManger)).containsAsFriend(n.getNode()) || ((LinkableSN)lnode.getProtocol(pidIdle)).containsAsFriend(n.getNode()))
				list.add(n);*/

		//		if (((SNNode)lnode).getRealID() == 1455300416)
		//			System.err.println(list.size() + " " + ((SNNode)rnode).getRealID());

		//System.err.println("SEND SIZE " + list.size());

		return list;
	}

	/**
	 * 
	 * @param lnode
	 * @param rnode
	 * @return the list of messages of lnode where the author is a friend in common with rnode
	 */
	public List<News> getNews(Node lnode, Node rnode, int pid)
	{
		NewsManager rNews = (NewsManager)rnode.getProtocol(pid);
		
		List<News> list = new ArrayList<News>();

		//		System.err.println(((SNNode)lnode).getRealID() + " " + ((SNNode)rnode).getRealID());

		for (News n: news)
			//			if (((LinkableSN)n.getSourceNode().getProtocol(pidIdle)).containsAsFriend(n.getSourceNode(), rnode) ||
			//((LinkableSN)n.getDestNode().getProtocol(pidIdle)).containsAsFriend(n.getSourceNode(), rnode))
			if (isInterestingNews(n, lnode, rnode) && !rNews.contains(n)){
				if (stats)
					System.out.println(CommonState.getIntTime() + " send: " + n.getSourceNode().getID() + " " + n.getDestNode().getID() + " " + n.getEventTime() + " " + rnode.getID() + " indegree " +  ((LinkableSN)n.getSourceNode().getProtocol(pidGossip)).getInDegree() + " " + lnode.getID());
				list.add(n);
			}
		/*if (n.getNode().getID() == lnode.getID() || ((LinkableSN)lnode.getProtocol(pidNetworkManger)).containsAsFriend(n.getNode()) || ((LinkableSN)lnode.getProtocol(pidIdle)).containsAsFriend(n.getNode()))
				list.add(n);*/

		//		if (((SNNode)lnode).getRealID() == 1455300416)
		//			System.err.println(list.size() + " " + ((SNNode)rnode).getRealID());

		//System.err.println("SEND SIZE " + list.size());

		return list;
	}

	/**
	 * 
	 * @param lnode
	 * @return the news of lnode
	 */
	public List<News> getOwnNews(Node lnode)
	{
		List<News> list = new ArrayList<News>();

		for (News n: news)
			if (n.getSourceNode().getID() == lnode.getID())// || n.getDestNode().getID() == lnode.getID())
				list.add(n);

		return list;
	}

	public News getNews(int index)
	{
		return this.news.get(index);
	}

	public boolean merge(List<News> messages)
	{
		boolean addSomething = false;
		for (News n : messages)
			if (!news.contains(n)){
				news.add(n);
				addSomething = true;
			}

		Collections.sort(this.news);
		return addSomething;
	}

	public void processEvent(Node node, int pid, Object event) {}

}
