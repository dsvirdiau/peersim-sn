package example.sn.epidemic.control;

import java.util.HashSet;
import java.util.Set;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import example.sn.NewsManager;
import example.sn.epidemic.message.NewsComment;
import example.sn.epidemic.message.NewsFriendship;
import example.sn.epidemic.message.NewsStatusChange;
import example.sn.linkable.LinkableSN;
import example.sn.node.SNNode;

public class AddNews implements Control
{
	private static final String PAR_PROT_NEWS_MANAGER = "protocol.newsManager";
	private static final String PAR_PROT_IDLE = "protocol.idle";
	private static final String PAR_PROT_NEWSCAST = "protocol.newscast";
	private static final String PAR_FRIENDSHIP = "friendshipNo";
	private static final String PAR_STATUS_CHANGE = "statusChangeNo";
	private static final String PAR_COMMENT = "commentNo";
	//private static final String PAR_ROOT = "root";

	private final int pidNewsManager;
	private final int pidIdle;
	private final int pidNewscast;
	private final int friendshipNo;
	private final int statusChangeNo;
	private final int commentNo;
	private static long root;

	public AddNews(String n)
	{
		this.pidNewsManager = Configuration.getPid(n + "." + PAR_PROT_NEWS_MANAGER);
		this.pidIdle = Configuration.getPid(n + "." + PAR_PROT_IDLE);
		this.pidNewscast = Configuration.getPid(n + "." + PAR_PROT_NEWSCAST);
		this.friendshipNo = Configuration.getInt(n + "." + PAR_FRIENDSHIP);
		this.statusChangeNo = Configuration.getInt(n + "." + PAR_STATUS_CHANGE);
		this.commentNo = Configuration.getInt(n + "." + PAR_COMMENT);
		//this.root = Configuration.getLong(n + "." + PAR_ROOT);
	}
	
	private int indexOf(long nodeRealID)
	{
		for (int i = 0; i < Network.size(); i++)
			if (((SNNode)Network.get(i)).getRealID() == nodeRealID)
				return i;
		
		return -1;
	}


	public boolean execute()
	{		
		final int size = Network.size();
		Set<Integer> s = new HashSet<Integer>();

		int i;
		NewsManager newsManager;
		while (s.size() < statusChangeNo){
			//i = CommonState.r.nextInt(size);
			i = indexOf(root);
			if (!s.contains(i) && Network.get(i).isUp()){
				newsManager = (NewsManager)Network.get(i).getProtocol(pidNewsManager);
				newsManager.addNews(new NewsStatusChange(Network.get(i)), Network.get(i));
				s.add(i);
			}
		}
		
		s = new HashSet<Integer>();
		while (s.size() < commentNo){
			//i = CommonState.r.nextInt(size);
			i = indexOf(root);
			if (!s.contains(i) && Network.get(i).isUp()){
				newsManager = (NewsManager)Network.get(i).getProtocol(pidNewsManager);
				newsManager.addNews(new NewsComment(Network.get(i),
						((LinkableSN)Network.get(i).getProtocol(pidIdle)).getFriendPeer(Network.get(i), Network.get(i))), Network.get(i));
				s.add(i);
			}
		}

		s = new HashSet<Integer>();
		LinkableSN idle;
		LinkableSN newscast;
		while (s.size() < friendshipNo){
			i = CommonState.r.nextInt(size);
			if (!s.contains(i)){
				idle = (LinkableSN)Network.get(i).getProtocol(pidIdle);
				newscast = (LinkableSN)Network.get(i).getProtocol(pidNewscast);

				Node n = Network.get(CommonState.r.nextInt(size));
				if (!idle.contains(n) && !newscast.contains(n)){				
					newsManager = (NewsManager)Network.get(i).getProtocol(pidNewsManager);
					newsManager.addNews(new NewsFriendship(Network.get(i), n), Network.get(i));
					s.add(i);
				}
			}
		}

		return false;
	}
	
	public static long getRoot() {
		return root;
	}

	public static void setRoot(long root) {
		AddNews.root = root;
	}

}