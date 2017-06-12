package org.ggp.base.player.gamer.statemachine.sample;

import java.util.ArrayList;
import java.util.List;

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

public class FixedDepthSearchMob extends StateMachineGamer {
public int LIMIT = 2;

	@Override
	public StateMachine getInitialStateMachine() {
		//return new CachedStateMachine(new ProverStateMachine());
		return new CachedStateMachine(new mediumRarePropNetStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// TODO Auto-generated method stub

	}

	/*function mobility (role,state)
	 {var actions = findlegals(role,state,game);
	  var feasibles = findactions(role,game);
	  return (actions.length/feasibles.length * 100)}
	  */
	int mobility(Role role, MachineState state)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine theMachine = getStateMachine();
		List<Move> moves = theMachine.getLegalMoves(state, role);
		List<Move> feasibles = theMachine.findActions(role);
		System.out.println((((moves.size())/(feasibles.size())) * 100));
		return (((moves.size())/(feasibles.size())) * 100);
	}

	int proximity(Role role, MachineState state)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine theMachine = getStateMachine();
		return theMachine.findReward(getRole(), state);
	}

	/*
	function focus (role,state)
	 {var actions = findlegals(role,state,game);
	  var feasibles = findactions(role,game);
	  return (100 - actions.length/feasibles.length * 100)}
	  */
	int focus(Role role, MachineState state)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine theMachine = getStateMachine();
		List<Move> moves = theMachine.getLegalMoves(state, role);
		List<Move> feasibles = theMachine.findActions(role);
		return (100 - ((moves.size())/(feasibles.size())) * 100);
	}

	/*
function maxscore (role,state,level)
 {if (findterminalp(state,game)) {return findreward(role,state,game)};
  if (level>=limit) {return 0};
  var actions = findlegals(role,state,game);
  var score = 0;
  for (var i=0; i<actions.length; i++)
      {var result = minscore(role,actions[i],state,level);
       if (result==100) {return 100};
       if (result>score) {score = result}};
  return score} */

	private int maxscore(Role role, MachineState state, int level)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine theMachine = getStateMachine();
		//System.out.println("Maxscore");
		if (theMachine.isTerminal(state)) {
			return theMachine.getGoal(state, getRole());
		}
		if (level >= LIMIT) return (focus(role, state)/5 + proximity(role, state) / 2 + focus(role, state) * 3/10);
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

/*function minscore (role,action,state,level)
 {var opponent = findopponent(role,game)
  var actions = findlegals(opponent,state,game);
  var score = 100;
  for (var i=0; i<actions.length; i++)
      {var move;
       if (role==roles[0]) {
       	move = [action,actions[i]]
       }
       else {
          move = [actions[i],action]
       }
       var newstate = findnext(move,state,game);
       var result = maxscore(role,newstate,level+1);
       if (result==0) {return 0};
       if (result<score) {score = result}};
  return score}
	 */
	public int minscore (Role role, Move action, MachineState state, int level)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		//System.out.println("minscore");
		StateMachine theMachine = getStateMachine();
		List<Role> opponents = theMachine.getRoles();
		int index = opponents.indexOf(role);
		int score = 100;
		List<List<Move>> moves = theMachine.getLegalJointMoves(state);
		for (int i = 0; i < moves.size(); i ++) {
			if (moves.get(i).get(index) == action) {
				List<Move> move = new ArrayList<Move>();
				for (int j = 0; j < opponents.size(); j ++) {
					//if (j == index) {
					//	move.add(action);
					//} else {
						move.add(moves.get(i).get(j));
					//}
				}
/*
			if (role.equals(opponents.get(0))) {
				move.add(action);
				move.addAll(moves.get(i));
			} else {
				move.addAll(moves.get(i));
				move.add(action);
			}*/
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

	/*
	 * function bestmove (role,state)
 {var actions = findlegals(role,state,game);
  var action = actions[0];
  var score = 0;
  for (var i=0; i<actions.length; i++)
      {var result = minscore(role,actions[i],state);
       if (result>score) {score = result; action = actions[i]}};
  return action}
	 */
	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		//System.out.println("asdfasdf");
		StateMachine theMachine = getStateMachine();
		//long start = System.currentTimeMillis();
		//long finishBy = timeout - 1000;

		List<Move> moves = theMachine.getLegalMoves(getCurrentState(), getRole());
		Move action = moves.get(0);
		int score = 0;
		for (int i = 0; i < moves.size(); i ++) {
			int result = minscore(getRole(), moves.get(i), getCurrentState(), 0);
			if (result > score) {
				//System.out.println("score update");
				score = result;
				action = moves.get(i);
			}
		}
		//long stop = System.currentTimeMillis();

		//notifyObservers(new GamerSelectedMoveEvent(moves, action, stop - start));
		return action;
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
		return "FixedDepthSearchMob";
	}

}

