
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.mediumRarePropNetStateMachine;

public class MonteCarloTreeSearch extends StateMachineGamer {

	class node {

		int num_visits;
		double utility;
		MachineState state;
		ArrayList<node> children;
		node parent;
		List<Move> move;
		//used for multiplayer
		Map<Move, Double> myMoveToUtility = new HashMap<Move, Double>();

		node(int num_visits, int utility, MachineState state, node parent, List<Move> list) {
			this.num_visits = num_visits;
			this.utility = 0;
			this.state = state;
			this.children = new ArrayList<node>();
			this.parent = parent;
			this.move = list;
		}

	}

	boolean isSingleplayer;

	@Override
	public StateMachine getInitialStateMachine() {
		//return new CachedStateMachine(new ProverStateMachine());
		return new CachedStateMachine(new mediumRarePropNetStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		StateMachine theMachine = getStateMachine();
		List<Role> roles = theMachine.getRoles();
		isSingleplayer = (roles.size() == 1);

	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine theMachine = getStateMachine();
		long start = System.currentTimeMillis();
		long finishBy = timeout - 1500;
		//////System.out.println(Long.toString(timeout));
		// List<Move> moves = theMachine.getLegalMoves(getCurrentState(),
		// getRole());
		node currnode = new node(0, 0, getCurrentState(), null, null);
		// for (int i = 0; i < 100; i ++) {
		// ArrayList<node> visited = new ArrayList<node>();
		int numMoves = 1;
		while (System.currentTimeMillis() <= timeout - 1500) {
			numMoves++;
			////System.out.println("top of while");
			node nextnode = select(currnode);
			// if (visited.contains(nextnode)) break;
			// visited.add(nextnode);
			expand(nextnode);
			// ////System.out.println(nextnode.num_visits);
			int result = simulate(getRole(), nextnode.state, timeout);
			if (result == -1)
				break;
			backpropagate(nextnode, result);
		}
		System.out.println("Num Moves: "+numMoves);
		////System.out.println("PRINT");
		// if (currnode.children.size()==0){
		// return null;
		// }
		if(isSingleplayer){
			node result = currnode.children.get(0);
			for (int i = 0; i < currnode.children.size(); i++) {
				if (result.utility < currnode.children.get(i).utility) {
					result = currnode.children.get(i);
				}
			}
			return result.move.get(0);
		} else {
			double maxScore = -1;
			Move maxMove = currnode.children.get(0).move.get(0);
			for (Map.Entry<Move, Double> curEntry : currnode.myMoveToUtility.entrySet()){
				if(curEntry.getValue() > maxScore){
					maxScore = curEntry.getValue();
					maxMove = curEntry.getKey();
				}
			}
			return maxMove;

		}
		//return result.move.get(0);
	}

	public node select(node curr_node) {
		////System.out.println("top of select");
		if (curr_node.num_visits == 0)
			return curr_node;
		for (int i = 0; i < curr_node.children.size(); i++) {
			if (curr_node.children.get(i).num_visits == 0) {
				return curr_node.children.get(i);
			}
		}
		double score = 0;
		node result = curr_node;
		for (int i = 0; i < curr_node.children.size(); i++) {
			double newscore = selectfn(curr_node.children.get(i));
			if (newscore > score) {
				score = newscore;
				result = curr_node.children.get(i);
			}
		}
		return select(result);
	}

	double selectfn(node curr_node) {
		return (curr_node.utility / curr_node.num_visits
				+ Math.sqrt(2 * Math.log(curr_node.parent.num_visits) / curr_node.num_visits));
	}

	/*
	 * function select (node) {if (node.visits==0) {return node}; for (var i=0;
	 * i<node.children.length; i++) {if (node.children[i].visits==0) {return
	 * node.children[i]}}; score = 0; result = node; for (var i=0;
	 * i<node.children.length; i++) {var newscore = selectfn(node.children[i]);
	 * if (newscore>score) {score = newscore; result=node.children[i]}}; return
	 * select(result)}
	 */

	boolean expand(node currNode) throws MoveDefinitionException, TransitionDefinitionException {
		// ////System.out.println("top of expand");
		StateMachine theMachine = getStateMachine();
		List<Move> actions = theMachine.getLegalMoves(currNode.state, getRole());
		List<List<Move>> moves = theMachine.getLegalJointMoves(currNode.state);
		for (int i = 0; i < moves.size(); i++) {
			////System.out.println(actions.size());
			/*
			 * List<Move> m = new ArrayList<Move>(); m.add(actions.get(i));
			 */
			MachineState newstate = theMachine.getNextState(currNode.state, moves.get(i));
			node newnode = new node(0, 0, newstate, currNode, moves.get(i));
			currNode.children.add(newnode);
		}
		return true;
	}

	/*
	 * function expand (node) {var actions = findlegals(role,node.state,game);
	 * for (var i=0; i<actions.length; i++) {var newstate =
	 * simulate(seq(actions[i]),state); var newnode =
	 * makenode(newstate,0,0,node,seq());
	 * node.children[node.children.length]=newnod}; return true}
	 */

	// simulate a.k.a depthCharge taken from MonteCarloGamer
	private int simulate(Role role, MachineState state, long timeout)
			throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		StateMachine theMachine = getStateMachine();

		while (!theMachine.isTerminal(state)) {
			if (System.currentTimeMillis() > timeout - 5000)
				return -1;
			// ////System.out.println("top of simulate");
			List<Move> move = new ArrayList<Move>();
			List<Role> roles = theMachine.getRoles();
			Random rand = new Random();
			for (int i = 0; i < roles.size(); i++) {
				List<Move> options = theMachine.getLegalMoves(state, roles.get(i));
				Move randomMove = options.get(rand.nextInt(options.size()));
				move.add(i, randomMove);
			}
			state = theMachine.getNextState(state, move);
		}

		return theMachine.getGoal(state, role);
		/*
		 *
		 * if (theMachine.isTerminal(state)) { return theMachine.getGoal(state,
		 * role); } List<Move> move = new ArrayList<Move>(); List<Role> roles =
		 * theMachine.getRoles(); Random rand = new Random(); for(int i = 0; i <
		 * roles.size(); i++){ List<Move> options =
		 * theMachine.getLegalMoves(state, roles.get(i)); Move randomMove =
		 * options.get(rand.nextInt(options.size())); move.add(i, randomMove); }
		 * MachineState newstate = theMachine.getNextState(state, move); return
		 * simulate(role, newstate);
		 */
	}

	boolean backpropagate(node currnode, double score) {
		StateMachine theMachine = getStateMachine();
		////System.out.println("top of prop");
		currnode.num_visits++;
		if(isSingleplayer){
			currnode.utility += score;
		} else {
			//if this node has a parent, update the parent to be worst case of us making this move
			//like a type of minmaxing
			if(currnode.parent != null){
				int roleIndex = theMachine.getRoleIndices().get(getRole());
				Move move = currnode.move.get(roleIndex);

				//default it to sentinel -1 value if this move doesn't yet exist
				double prevMoveScore = currnode.parent.myMoveToUtility.getOrDefault(move, -1.0);
				if (prevMoveScore == -1 || score < prevMoveScore){
					currnode.parent.myMoveToUtility.put(move, score);
				}
			}
			//utility is the max of all the moves
			double maxUtility = -1;
			for(Double moveUtility : currnode.myMoveToUtility.values()){
				if(moveUtility > maxUtility){
					maxUtility = moveUtility;
				}
			}
			currnode.utility += maxUtility;

		}

		if (currnode.parent != null) {
			backpropagate(currnode.parent, score);
		}
		return true;
	}
	/*
	 * function backpropagate (node,score) {node.visits = node.visits+1;
	 * node.utility = node.utility+score; if (node.parent)
	 * {backpropagate(node.parent,score)}; return true}
	 */

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		return "MediumRareMonteCarloTreeSearch";
	}

}
