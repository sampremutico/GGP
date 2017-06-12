package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class mediumRarePropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
        	description = sanitizeDistinct(description);
            propNet = OptimizingPropNetFactory.create(description);
            roles = propNet.getRoles();
            ordering = getOrdering();
            System.out.println("PropNet Size: "+propNet.getSize());
            System.out.println("Bases: "+propNet.getBasePropositions().size());
            System.out.println("Props: "+propNet.getPropositions().size());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean markBases(MachineState state){
    	propNet.getInitProposition().setValue(false);
    	Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
    	Set<GdlSentence> currSentences = state.getContents();
    	for (GdlSentence key : props.keySet()){
    		boolean result = currSentences.contains(key);
    		props.get(key).setValue(result);
    	}
    	return true;
    }

    private boolean markActions(List<Move> moveList){
    	propNet.getInitProposition().setValue(false);
    	Map<GdlSentence, Proposition> props = propNet.getInputPropositions(); // IS THIS WHAT ACTIONS IS?
    	List<GdlSentence> currSentences = toDoes(moveList);
    	for (GdlSentence key : props.keySet()){
    		boolean result = currSentences.contains(key);
    		props.get(key).setValue(result);
    	}
    	return true;
    }

    private boolean clearPropNet(){
    	Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
    	propNet.getInitProposition().setValue(false);
    	for (GdlSentence key : props.keySet()){
    		props.get(key).setValue(false);
    	}
    	return true;
    }

    private boolean propMarkNegation (Component p){
 	   return !propMarkP((Proposition) p.getSingleInput()); // look more into single input issue
     }

    private boolean propMarkConjunction(Component p){
 	   for (Component c: p.getInputs()){
 		   if (!propMarkP(c)) return false;
 	   }
 	   return true;
    }

    private boolean propMarkDisjunction(Component p){
 	   for (Component c: p.getInputs()){
 		   if (propMarkP(c)) return true;
 	   }
 	   return false;
    }

    private boolean propMarkP(Component p){
	      if (p instanceof Not) {
	    	  return propMarkNegation(p);
	      } else if (p instanceof And) {
	    	  return propMarkConjunction(p);
	      } else if (p instanceof Or) {
	    	  return propMarkDisjunction(p);
	      } else if (p instanceof Constant) {
	    	  return p.getValue();
	      } else if (p instanceof Transition){
	    	  return propMarkP(p.getSingleInput());
	      } else if (p.equals(propNet.getInitProposition())) {
    		  return p.getValue();
    	  } else if (propNet.getBasePropositions().containsValue(p)){
	    	  return p.getValue();
	      } else if (propNet.getInputPropositions().containsValue(p)){
	    	  return p.getValue();
	      } else
    	  // must be a view proposition in this case
	      if (p instanceof Proposition){return propMarkP((Component) p.getSingleInput());}
      return false;
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
        // Compute whether the MachineState is terminal.
    	markBases(state);
    	return propMarkP(propNet.getTerminalProposition());
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
    	markBases(state);
    	List<Role> roles = propNet.getRoles();
    	Set<Proposition> rewards = new HashSet<Proposition>();
    	for (int i = 0; i < roles.size(); i++){
    		if (role.equals(roles.get(i))) {
    			rewards = propNet.getGoalPropositions().get(role);
    			break;
    		}
    	}
    	if (rewards.size() == 0) {return 0;/*throw new GoalDefinitionException();*/}
    	for (Proposition p : rewards){
    		if (propMarkP(p)) {
    			return getGoalValue(p);  // IS THIS RIGHT?
    		}
    	}
        return 0;  // SHOULD THROW AN ERROR AT SOME POINT?
    }

    private void falsify(){
    	propNet.getTerminalProposition().setValue(false);
    	Map<GdlSentence, Proposition> props = propNet.getInputPropositions();
    	for (GdlSentence key : props.keySet()){
    		props.get(key).setValue(false);
    	}
    }

    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {
    	clearPropNet(); //clear
    	falsify();
    	if (propNet.getInitProposition() != null){ //don't null out
    		propNet.getInitProposition().setValue(true);
    	}
    	// Compute next state
    	Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();
    	Set<GdlSentence> nexts = new HashSet<GdlSentence>();
    	for (GdlSentence key : bases.keySet()){
    		Component currC = bases.get(key).getSingleInput().getSingleInput(); //.source.source here
    		if (propMarkP(currC)){
    			nexts.add(bases.get(key).getName()); //------> should we just add key? look more into this
    		}
    	}
        return new MachineState(nexts);
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role)
            throws MoveDefinitionException {
        Set<Proposition> legals = propNet.getLegalPropositions().get(role);
        ArrayList<Move> result = new ArrayList<Move>();
        for (Proposition legalProp : legals){
        	result.add(getMoveFromProposition(legalProp));
        }
        System.out.println("Found "+result.size()+" legal actions");
        return result;
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {
    	markBases(state);
    	List<Role> roles = propNet.getRoles();
    	Set<Proposition> legals = new HashSet<Proposition>();
    	for (int i = 0; i < roles.size(); i++){
    		if (role.equals(roles.get(i))) {
    			legals = propNet.getLegalPropositions().get(roles.get(i));
    			break;
    		}
    	}
    	List<Move> actions = new ArrayList<Move>();
    	for (Proposition p : legals){
    		if (propMarkP(p)){
    			actions.add(getMoveFromProposition(p));
    		}
    	}
    	System.out.println("Found "+actions.size()+" legal moves");
        return actions;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {
    	markActions(moves);
    	markBases(state);
    	propNet.getInitProposition().setValue(false);
    	Map< GdlSentence, Proposition> bases = propNet.getBasePropositions();
    	Set<GdlSentence> nexts = new HashSet<GdlSentence>();
    	for (GdlSentence key : bases.keySet()){
    		Component currC = bases.get(key).getSingleInput().getSingleInput();
    		if (propMarkP(currC)){
    			nexts.add(key);
    		}
    	}
        return (new MachineState(nexts));
    }

    /**
     * This should compute the topological ordering of propositions.
     * Each component is either a proposition, logical gate, or transition.
     * Logical gates and transitions only have propositions as inputs.
     *
     * The base propositions and input propositions should always be exempt
     * from this ordering.
     *
     * The base propositions values are set from the MachineState that
     * operations are performed on and the input propositions are set from
     * the Moves that operations are performed on as well (if any).
     *
     * @return The order in which the truth values of propositions need to be set.
     */
    public List<Proposition> getOrdering()
    {
        // List to contain the topological ordering.
        List<Proposition> order = new LinkedList<Proposition>();

        // All of the components in the PropNet
        List<Component> components = new ArrayList<Component>(propNet.getComponents());

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

        // TODO: Compute the topological ordering.

        return order;
    }

    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */

    /**
     * The Input propositions are indexed by (does ?player ?action).
     *
     * This translates a list of Moves (backed by a sentence that is simply ?action)
     * into GdlSentences that can be used to get Propositions from inputPropositions.
     * and accordingly set their values etc.  This is a naive implementation when coupled with
     * setting input values, feel free to change this for a more efficient implementation.
     *
     * @param moves
     * @return
     */
    private List<GdlSentence> toDoes(List<Move> moves)
    {
        List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
        Map<Role, Integer> roleIndices = getRoleIndices();

        for (int i = 0; i < roles.size(); i++)
        {
            int index = roleIndices.get(roles.get(i));
            doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
        }
        return doeses;
    }

    /**
     * Takes in a Legal Proposition and returns the appropriate corresponding Move
     * @param p
     * @return a PropNetMove
     */
    public static Move getMoveFromProposition(Proposition p)
    {
        return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
    private int getGoalValue(Proposition goalProposition)
    {
        GdlRelation relation = (GdlRelation) goalProposition.getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        return Integer.parseInt(constant.toString());
    }

    /**
     * A Naive implementation that computes a PropNetMachineState
     * from the true BasePropositions.  This is correct but slower than more advanced implementations
     * You need not use this method!
     * @return PropNetMachineState
     */
    public MachineState getStateFromBase()
    {
        Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values())
        {
            p.setValue(p.getSingleInput().getValue());
            if (p.getValue())
            {
                contents.add(p.getName());
            }

        }
        return new MachineState(contents);
    }

    // code from Piazza
    private void sanitizeDistinctHelper(Gdl gdl, List<Gdl> in, List<Gdl> out) {
        if (!(gdl instanceof GdlRule)) {
            out.add(gdl);
            return;
        }
        GdlRule rule = (GdlRule) gdl;
        for (GdlLiteral lit : rule.getBody()) {
            if (lit instanceof GdlDistinct) {
                GdlDistinct d = (GdlDistinct) lit;
                GdlTerm a = d.getArg1();
                GdlTerm b = d.getArg2();
                if (!(a instanceof GdlFunction) && !(b instanceof GdlFunction)) continue;
                if (!(a instanceof GdlFunction && b instanceof GdlFunction)) return;
                GdlSentence af = ((GdlFunction) a).toSentence();
                GdlSentence bf = ((GdlFunction) b).toSentence();
                if (!af.getName().equals(bf.getName())) return;
                if (af.arity() != bf.arity()) return;
                for (int i = 0; i < af.arity(); i++) {
                    List<GdlLiteral> ruleBody = new ArrayList<>();
                    for (GdlLiteral newLit : rule.getBody()) {
                        if (newLit != lit) ruleBody.add(newLit);
                        else ruleBody.add(GdlPool.getDistinct(af.get(i), bf.get(i)));
                    }
                    GdlRule newRule = GdlPool.getRule(rule.getHead(), ruleBody);
                    System.out.println("new rule: " + newRule);
                    in.add(newRule);
                }
                return;
            }
        }
        for (GdlLiteral lit : rule.getBody()) {
            if (lit instanceof GdlDistinct) {
                System.out.println("distinct rule added: " + rule);
                break;
            }
        }
        out.add(rule);
    }

    private List<Gdl> sanitizeDistinct(List<Gdl> description) {
        List<Gdl> out = new ArrayList<>();
        for (int i = 0; i < description.size(); i++) {
            sanitizeDistinctHelper(description.get(i), description, out);
        }
        return out;
    }
}