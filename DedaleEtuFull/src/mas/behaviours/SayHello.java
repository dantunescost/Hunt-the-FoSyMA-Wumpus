package mas.behaviours;

import java.io.IOException;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;


public class SayHello extends TickerBehaviour{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2058134622078521998L;

	/**
	 * An agent tries to contact its friend and to give him its current position
	 * @param myagent the agent who posses the behaviour
	 *  
	 */
	public SayHello (final Agent myagent) {
		super(myagent, 3000);
		//super(myagent);
	}

	@Override
	public void onTick() {
		String myPosition=((mas.abstractAgent)this.myAgent).getCurrentPosition();
		
		ACLMessage msg=new ACLMessage(7);
		msg.setSender(this.myAgent.getAID());

		if (myPosition!=""){
			System.out.println("Agent "+this.myAgent.getLocalName()+ " is trying to reach its friends");
			try {
				msg.setContentObject(((mas.agents.DummyExploAgent)this.myAgent).getGraphSerial());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!myAgent.getLocalName().equals("Explo1")){
				msg.addReceiver(new AID("Explo1",AID.ISLOCALNAME));
			}else{
				msg.addReceiver(new AID("Explo2",AID.ISLOCALNAME));
			}

			((mas.abstractAgent)this.myAgent).sendMessage(msg);

		}

	}

}