import java.util.ArrayList;
import java.util.List;
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
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class MonteCarloGamer extends StateMachineGamer {

	public int LEVELS = 2;
	public int PROBES_PER_STATE = 4;

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
		//return new CachedStateMachine(new mediumRarePropNetStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine theMachine = getStateMachine();

		//taken from HeuristicSearchGamer
		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move action = moves.get(0);
		int score = 0;
		for (int i = 0; i < moves.size(); i ++) {
			int result = minscore(getRole(), moves.get(i), getCurrentState(), 0);
			if (result > score) {
				score = result;
				action = moves.get(i);
			}
		}
		return action;
	}

	//adapted from HeuristicSearchGamer
	private int maxscore(Role role, MachineState state, int level)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine theMachine = getStateMachine();
		//System.out.println("Maxscore");
		if (theMachine.isTerminal(state)) {
			return theMachine.getGoal(state, getRole());
		}
		if (level >= LEVELS) return montecarlo(role, state, PROBES_PER_STATE);
		int score = 0;
		List<Move> moves = theMachine.getLegalMoves(state, role);
		for (int i = 0; i < moves.size(); i ++) {
			int result = minscore(role, moves.get(i), state, level);
			if (result == 100) return result;
			if (result > score) {
				score = result;
			}
		}
		return score;

	}

	/*
	 function montecarlo (role,state,count)
	 {var total = 0;
	  for (var i=0; i<count; i++)
	      {total = total + depthcharge(role,state)};
	  return total/count}
	 */

	private int montecarlo(Role role, MachineState state, int count) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		int total = 0;
		for (int i = 0; i < count; i++){
			total = total + depthcharge(role, state);
		}
		return total/count;
	}

	/*
	 function depthcharge (role,state)
	 {if (findterminalp(state,game)) {return findreward(role,state,game)};
	  var move = seq();
	  for (var i=0; i<roles.length; i++)
	      {var options = findlegals(roles[i],state,game);
	       move[i] = randomelement(options)};
	  var newstate = simulate(move,state);
	  return depthcharge(role,newstate)}
	 */

	private int depthcharge(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		StateMachine theMachine = getStateMachine();
		if (theMachine.isTerminal(state)) {
			return theMachine.getGoal(state, role);
		}
		List<Move> move = new ArrayList<Move>();
		List<Role> roles = theMachine.getRoles();
		for(int i = 0; i < roles.size(); i++){
			List<Move> options = theMachine.getLegalMoves(state, roles.get(i));
			Move randomMove = options.get(new Random().nextInt(options.size()));
			move.add(i, randomMove);
		}
		MachineState newstate = theMachine.getNextState(state, move);
		return depthcharge(role, newstate);
	}

	//taken from HeuristicSearchGamer
	public int minscore (Role role, Move action, MachineState state, int level)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		StateMachine theMachine = getStateMachine();
		List<Role> opponents = theMachine.getRoles();
		int index = opponents.indexOf(role);
		int score = 100;
		List<List<Move>> moves = theMachine.getLegalJointMoves(state);
		for (int i = 0; i < moves.size(); i ++) {
			if (moves.get(i).get(index) == action) {
				List<Move> move = new ArrayList<Move>();
				for (int j = 0; j < opponents.size(); j ++) {
					move.add(moves.get(i).get(j));
				}
				MachineState nextState = theMachine.getNextState(state, move);
				int result = maxscore(role, nextState, level + 1);
				if (result == 0) return 0;
				if (result < score) {
					score = result;
				}
			}
		}
		return score;

	}

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
		return "MonteCarloGamer";
	}

}
