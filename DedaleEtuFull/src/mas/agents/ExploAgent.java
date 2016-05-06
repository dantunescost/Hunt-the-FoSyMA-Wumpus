package mas.agents;


import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;














import env.Environment;
import graph.GraphStreamSerial;
import graph.Graphe;
import graph.Pair;
import mas.abstractAgent;
import mas.behaviours.*;


public class ExploAgent extends abstractAgent{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1784844593772918359L;
	
	
	private static final int NO_PRIO = 0;
	private static final int UNBLOCK_PRIO = 1;
	private static final int TREASURE_PRIO = 2;
	private static final int UNBLOCK_TREASURE_PRIO = 3;
	

	private GraphStreamSerial graph = new GraphStreamSerial(this.getLocalName());
	private Graphe graphe = new Graphe();
	private Random rng = new Random();
	private long period = 100;

	private int echecs = 0;
	private ArrayList<String> path = new ArrayList<String>();
	
	private FSMBehaviour fsm = new FSMBehaviour(this);
	private ParallelBehaviour pb = new ParallelBehaviour(this, ParallelBehaviour.WHEN_ANY);
	private CoopWalk cw = new CoopWalk(this);
	
	private int treasurePicked;
	private int priorityLevel;

	protected String styleSheet =
	        "node.known {" +
	        "	fill-color: red, cyan;" +
	        "	fill-mode: dyn-plain;" +
	        "}" +
	        "node.explored {" +
	        "	fill-color: blue, cyan;" +
	        "	fill-mode: dyn-plain;" +
	        "}" +
	        "node.treasure {" +
	        "	fill-color: yellow, cyan;" +
	        "	fill-mode: dyn-plain;" +
	        "}";

	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup(){

		super.setup();

		//get the parameters given into the object[]. In the current case, the environment where the agent will evolve
		final Object[] args = getArguments();
		if(args[0]!=null){

			deployAgent((Environment) args[0]);

		}else{
			System.err.println("Malfunction during parameter's loading of agent"+ this.getClass().getName());
			System.exit(-1);
		}
		
		this.priorityLevel = NO_PRIO;
		

		//Add the behaviours
		addBehaviour(new SendGraph(this));
		pb.addSubBehaviour(cw);
		fsm.registerFirstState(pb, "Exploration");
		addBehaviour(fsm);
		
		
		
		
		// Initialize graphStream
		graph.addAttribute("ui.stylesheet", styleSheet);
		graph.addAttribute("ui.quality");
	    graph.addAttribute("ui.antialias");
		graph.display();
		
		// Enregistrement sur le DF
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID()); /* getAID est l'AID de l'agent qui veut s'enregistrer*/
		ServiceDescription sd  = new ServiceDescription();
		sd.setType( "explorer" ); /* il faut donner des noms aux services qu'on propose (ici explorer)*/
		sd.setName(getLocalName() );
		dfd.addServices(sd);
		        
		try {  
		      DFService.register(this, dfd );  
		}
		catch (FIPAException fe) { fe.printStackTrace(); }
		

		System.out.println("the agent "+this.getLocalName()+ " is started");

	}

	/**
	 * This method is automatically called after doDelete()
	 */
	protected void takeDown(){

	}
	
	public Collection<String> getListeExplores() {
		return graphe.getExplores();
	}
	
	public Collection<String> getListeConnus() {
		return graphe.getConnus();
	}
	
	/*public Serializable getListeExploresSerial() {
		return (Serializable) graph.getExplores();
	}*/
	
	public GraphStreamSerial getGraphStream(){
		return this.graph;
	}
	
	public int getRandom(int max)
	{
		return rng.nextInt(max);
	}
	
	public Serializable getGraphSerial()
	{
		return (Serializable) graphe;
	}
	
	public Graphe getGraph()
	{
		return graphe;
	}

	public int getEchecs() {
		return echecs;
	}

	public void resetEchecs() {
		this.echecs = 0;
	}
	
	public void incEchecs() {
		this.echecs++;
	}
	
	public long getPeriod() {
		return period;
	}
	
	public int getTreasurePicked() {
		return treasurePicked;
	}

	public ArrayList<String> getPath() {
		return path;
	}

	public void setPath(ArrayList<String> path) {
		this.path = path;
	}
	
	public void restartExplo() {
		jade.util.leap.Collection c = this.pb.getTerminatedChildren();
		if(!c.isEmpty())
		{
			pb.removeSubBehaviour((Behaviour) c.toArray()[0]);
			cw = new CoopWalk(this);
			pb.addSubBehaviour(cw);
		}
	}
	
	public void restartTreasuring(Pair<String,Integer> tresor, int prof, String parent){
		System.out.println("Start TREASURING for agent " + this.getLocalName());
		fsm.deregisterState("Treasure");
		fsm.registerState(new TreasureBehaviour(this,tresor,prof, parent), "Treasure");
		fsm.registerTransition("Exploration", "Treasure", 0);
		fsm.registerTransition("Treasure", "Exploration", 0);
		
	}
	
	public void viderBoiteReception(){
		ACLMessage msg1, msg2, msg3, msg4;
		final MessageTemplate msgTemplate1 = MessageTemplate.MatchLanguage("treasure");
		final MessageTemplate msgTemplate2 = MessageTemplate.MatchLanguage("elu");
		final MessageTemplate msgTemplate3 = MessageTemplate.MatchLanguage("ack");
		final MessageTemplate msgTemplate4 = MessageTemplate.MatchLanguage("solution");
		do{
			msg1 = this.receive(msgTemplate1);
			msg2 = this.receive(msgTemplate2);
			msg3 = this.receive(msgTemplate3);
			msg4 = this.receive(msgTemplate4);
			
		}while(msg1 != null || msg2 != null || msg3 != null || msg4 != null);
	}
	
	public int betterPickUp()
	{
		int pu = this.pick();
		this.treasurePicked += pu;
		this.priorityLevel = NO_PRIO;
		return pu;
	}
	
	
	public void setPriorityToNone()
	{
		this.priorityLevel = NO_PRIO;
	}
	
	public void setPriorityToTreasure()
	{
		this.priorityLevel = TREASURE_PRIO;
	}
	
	public void setPriorityToUnblock()
	{
		this.priorityLevel = UNBLOCK_PRIO;
	}
	
	public void setPriorityToUnblockTreasure()
	{
		this.priorityLevel = UNBLOCK_TREASURE_PRIO;
	}
}
