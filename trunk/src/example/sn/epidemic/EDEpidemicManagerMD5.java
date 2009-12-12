package example.sn.epidemic;

import example.sn.epidemic.message.EpidemicMessage;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import peersim.extras.am.epidemic.EDEpidemicManager;
import peersim.extras.am.epidemic.EpidemicProtocol;
import peersim.extras.am.epidemic.Message;
import peersim.transport.Transport;

public class EDEpidemicManagerMD5 extends EDEpidemicManager
{

	public EDEpidemicManagerMD5(String n)
	{
		super(n);
	}
	
	public void processEvent(Node lnode, int thisPid, Object event)
	{
		if (event instanceof Integer) {
			activeThread(lnode, (Integer) event, thisPid);
		} else {
			passiveThread(lnode, (Message) event, thisPid);
		}
	}
	
	private void activeThread(Node lnode, Integer pid, int thisPid)
	{
		EDSimulator.add(c.period, pid, lnode, thisPid);
		EpidemicProtocol lpeer = (EpidemicProtocol) lnode.getProtocol(pid);
		Node rnode = lpeer.selectPeer(lnode);
		if (rnode == null)
			return;
		Message request = lpeer.prepareRequest(lnode, rnode);

		if (request != null) {
			
			System.out.println("Request " + request);
			
			request.setPid(pid);
			request.setRequest(true);
			request.setSender(lnode);
			Transport tr = (Transport) lnode.getProtocol(c.tid);
			tr.send(lnode, rnode, request, thisPid);
		}
	}

	private void passiveThread(Node lnode, Message message, int thisPid)
	{
		EpidemicMessage msg = (EpidemicMessage)message;
		int pid = msg.getPid();
		EpidemicProtocol lpeer = (EpidemicProtocol) lnode.getProtocol(pid);
		if (msg.isRequest()) {
			EpidemicMessage reply = (EpidemicMessage)lpeer.prepareResponse(lnode, msg.getSender(), msg);
			if (reply != null) {
				System.out.println("Reply " + reply);
				reply.setPid(pid);
				reply.setRequest(reply.isRequest());
				reply.setSender(lnode);
				Transport tr = (Transport) lnode.getProtocol(c.tid);
				tr.send(lnode, msg.getSender(), reply, thisPid);
			}
		}
		if (!msg.isHash())
			lpeer.merge(lnode, msg.getSender(), msg);
	}
	
}