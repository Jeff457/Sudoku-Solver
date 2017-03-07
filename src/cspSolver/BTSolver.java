package cspSolver;
import java.util.*;

import sudoku.Converter;
import sudoku.SudokuFile;
/**
 * Backtracking solver. 
 *
 */
public class BTSolver implements Runnable{

	//===============================================================================
	// Properties
	//===============================================================================

	private ConstraintNetwork network;
	private static Trail trail = Trail.getTrail();
	private boolean hasSolution = false;
	private SudokuFile sudokuGrid;

	private int numAssignments;
	private int numBacktracks;
	private long startTime;
	private long endTime;
	
	public enum VariableSelectionHeuristic 		{ None, MinimumRemainingValue, Degree };
	public enum ValueSelectionHeuristic 		{ None, LeastConstrainingValue };
	public enum ConsistencyCheck				{ None, ForwardChecking, ArcConsistency };
	public enum HeuristicCheck					{ None, NakedPairs, NakedTriples, Both}
	
	private VariableSelectionHeuristic varHeuristics;
	private ValueSelectionHeuristic valHeuristics;
	private ConsistencyCheck cChecks;
	private HeuristicCheck heurCheck;
	//===============================================================================
	// Constructors
	//===============================================================================

	public BTSolver(SudokuFile sf)
	{
		this.network = Converter.SudokuFileToConstraintNetwork(sf);
		this.sudokuGrid = sf;
		numAssignments = 0;
		numBacktracks = 0;
	}

	//===============================================================================
	// Modifiers
	//===============================================================================
	
	public void setVariableSelectionHeuristic(VariableSelectionHeuristic vsh)
	{
		this.varHeuristics = vsh;
	}
	
	public void setValueSelectionHeuristic(ValueSelectionHeuristic vsh)
	{
		this.valHeuristics = vsh;
	}
	
	public void setConsistencyChecks(ConsistencyCheck cc)
	{
		this.cChecks = cc;
	}

	public void setHeuristicCheck(HeuristicCheck check)
	{
		this.heurCheck = check;
	}
	//===============================================================================
	// Accessors
	//===============================================================================

	/** 
	 * @return true if a solution has been found, false otherwise. 
	 */
	public boolean hasSolution()
	{
		return hasSolution;
	}

	/**
	 * @return solution if a solution has been found, otherwise returns the unsolved puzzle.
	 */
	public SudokuFile getSolution()
	{
		return sudokuGrid;
	}

	public void printSolverStats()
	{
		System.out.println("Time taken:" + (endTime-startTime) + " ms");
		System.out.println("Number of assignments: " + numAssignments);
		System.out.println("Number of backtracks: " + numBacktracks);
	}

	/**
	 * 
	 * @return time required for the solver to attain in seconds
	 */
	public long getTimeTaken()
	{
		return endTime-startTime;
	}

	public int getNumAssignments()
	{
		return numAssignments;
	}

	public int getNumBacktracks()
	{
		return numBacktracks;
	}

	public ConstraintNetwork getNetwork()
	{
		return network;
	}

	//===============================================================================
	// Helper Methods
	//===============================================================================

	/**
	 * Checks whether the changes from the last time this method was called are consistent. 
	 * @return true if consistent, false otherwise
	 */
	private boolean checkConsistency()
	{
		boolean isConsistent = false;
		switch(cChecks)
		{
		case None: 				isConsistent = assignmentsCheck();
		break;
		case ForwardChecking: 	isConsistent = forwardChecking();
		break;
		case ArcConsistency: 	isConsistent = arcConsistency();
		break;
		default: 				isConsistent = assignmentsCheck();
		break;
		}
		return isConsistent;
	}

	private boolean checkHeuristic()
	{
		if(heurCheck == HeuristicCheck.NakedPairs)
			return nakedPairs();
		if(heurCheck == HeuristicCheck.NakedTriples)
			return nakedTriples();
		if(heurCheck == HeuristicCheck.Both)
			return nakedPairs() && nakedTriples();
		return true;
	}
	
	/**
	 * default consistency check. Ensures no two variables are assigned to the same value.
	 * @return true if consistent, false otherwise. 
	 */
	private boolean assignmentsCheck()
	{
		for(Variable v : network.getVariables())
		{
			if(v.isAssigned())
			{
				for(Variable vOther : network.getNeighborsOfVariable(v))
				{
					if (v.getAssignment() == vOther.getAssignment())
					{
						return false;
					}
				}
			}
		}
		return true;
	}

	
	/**
	 * TODO: Implement forward checking. 
	 */
	private boolean forwardChecking()
	{
		return false;
	}
	
	/**
	 * TODO: Implement Maintaining Arc Consistency.
	 */
	private boolean arcConsistency()
	{
		return false;
	}

	//TODO---Implement Naked Pairs
	private boolean nakedPairs()
	{
		return false;
	}

	//TODO---Implement Naked Triples
	private boolean nakedTriples()
	{
		boolean success = false;
		for(Constraint constraint : network.getConstraints())
			if(constraint.propagateNakedTriples())
				success = true;
		return success;
	}

	/**
	 * Selects the next variable to check.
	 * @return next variable to check. null if there are no more variables to check. 
	 */
	private Variable selectNextVariable()
	{
		Variable next = null;
		switch(varHeuristics)
		{
		case None: 					next = getfirstUnassignedVariable();
		break;
		case MinimumRemainingValue: next = getMRV();
		break;
		case Degree:				next = getDegree();
		break;
		default:					next = getfirstUnassignedVariable();
		break;
		}
		return next;
	}
	
	/**
	 * default next variable selection heuristic. Selects the first unassigned variable. 
	 * @return first unassigned variable. null if no variables are unassigned. 
	 */
	private Variable getfirstUnassignedVariable()
	{
		for(Variable v : network.getVariables())
		{
			if(!v.isAssigned())
			{
				return v;
			}
		}
		return null;
	}

	/**
	 * TODO: Implement MRV heuristic
	 * @return variable with minimum remaining values that isn't assigned, null if all variables are assigned. 
	 */
	private Variable getMRV()
	{
		Variable min = null;
		for(Variable v : network.getVariables())
		{
			if(!v.isAssigned())
			{
				if(min == null || v.getDomain().getValues().size() < min.getDomain().getValues().size())
					min = v;
			}
		}
		return min;
	}
	
	/**
	 * TODO: Implement Degree heuristic
	 * @return variable constrained by the most unassigned variables, null if all variables are assigned.
	 */
	private Variable getDegree()
	{
		//TODO---Update this to use the getNeighbors() method instead of finding them by hand
		int constraints = 0;
		Variable mostConstrained = null;

		//We are selecting a variable so go through all the variables
		for(Variable v : network.getVariables())
		{
			//We are only looking for unassigned variables
			if(!v.isAssigned())
			{
				//Store all the unassigned variables that are constrained with Variable v
				HashSet<Variable> unassignedVars = new HashSet<>();
				List<Constraint> involvedConstraints = network.getConstraintsContainingVariable(v);

				for(Constraint c : involvedConstraints)
				{
					for(Variable v2 : c.vars)
						if(!v2.isAssigned() && !v2.equals(v))
							unassignedVars.add(v2);
				}

				//Now the Set of vars contains all the DISTINCT unassigned constrained variables
				if(unassignedVars.size() > constraints)
				{
					constraints = unassignedVars.size();
					mostConstrained = v;
				}
			}
		}
		return mostConstrained;
	}
	
	/**
	 * Value Selection Heuristics. Orders the values in the domain of the variable 
	 * passed as a parameter and returns them as a list.
	 * @return List of values in the domain of a variable in a specified order. 
	 */
	public List<Integer> getNextValues(Variable v)
	{
		List<Integer> orderedValues;
		switch(valHeuristics)
		{
		case None: 						orderedValues = getValuesInOrder(v);
		break;
		case LeastConstrainingValue: 	orderedValues = getValuesLCVOrder(v);
		break;
		default:						orderedValues = getValuesInOrder(v);
		break;
		}
		return orderedValues;
	}
	
	/**
	 * Default value ordering. 
	 * @param v Variable whose values need to be ordered
	 * @return values ordered by lowest to highest. 
	 */
	public List<Integer> getValuesInOrder(Variable v)
	{
		List<Integer> values = v.getDomain().getValues();
		
		Comparator<Integer> valueComparator = new Comparator<Integer>(){

			@Override
			public int compare(Integer i1, Integer i2) {
				return i1.compareTo(i2);
			}
		};
		Collections.sort(values, valueComparator);
		return values;
	}
	
	/**
	 * TODO: LCV heuristic
	 */
	public List<Integer> getValuesLCVOrder(Variable v)
	{
		ArrayList<Integer> domain = new ArrayList<>(v.getDomain().getValues());
		final HashMap<Integer,Integer> cache = new HashMap<>();
		Collections.sort(domain, new Comparator<Integer>()
		{
			@Override
			public int compare(Integer val1, Integer val2)
			{
				int val1Conflicts = Integer.MAX_VALUE;
				int val2Conflicts = Integer.MAX_VALUE;

				if(cache.containsKey(val1))
					val1Conflicts = cache.get(val1);
				else
				{

					cache.put(val1,val1Conflicts);
				}

				if(cache.containsKey(val2))
					val2Conflicts = cache.get(val2);
				else
				{
					cache.put(val2,val2Conflicts);
				}
				return 0;
			}
		});
		return domain;
	}
	/**
	 * Called when solver finds a solution
	 */
	private void success()
	{
		hasSolution = true;
		sudokuGrid = Converter.ConstraintNetworkToSudokuFile(network, sudokuGrid.getN(), sudokuGrid.getP(), sudokuGrid.getQ());
	}

	//===============================================================================
	// Solver
	//===============================================================================

	/**
	 * Method to start the solver
	 */
	public void solve()
	{
		startTime = System.currentTimeMillis();
		try {

			//TODO---determine if this is allowed
			//Trim down starting variable domains using constraint propegation
			for(Constraint c : network.getConstraints())
				c.propagateConstraint();
/*
			SudokuFile f = Converter.ConstraintNetworkToSudokuFile(network,sudokuGrid.getN(),sudokuGrid.getP(),sudokuGrid.getQ());
			System.out.println();
			System.out.println(f);

			System.out.println();
			System.out.println(network.isConsistent());
*/
			solve(0);
		}catch (VariableSelectionException e)
		{
			System.out.println("error with variable selection heuristic.");
		}
		endTime = System.currentTimeMillis();
		Trail.clearTrail();
	}

	/**
	 * Solver
	 * @param level How deep the solver is in its recursion. 
	 * @throws VariableSelectionException 
	 */

	private void solve(int level) throws VariableSelectionException
	{
		if(!Thread.currentThread().isInterrupted())

		{//Check if assignment is completed
			if(hasSolution)
			{
				return;
			}

			//Select unassigned variable
			Variable v = selectNextVariable();		

			//check if the assignment is complete
			if(v == null)
			{
				for(Variable var : network.getVariables())
				{
					if(!var.isAssigned())
					{
						throw new VariableSelectionException("Something happened with the variable selection heuristic");
					}
				}
				success();
				return;
			}

			//loop through the values of the variable being checked LCV

			
			for(Integer i : getNextValues(v))
			{
				trail.placeBreadCrumb();

				//check a value
				v.updateDomain(new Domain(i));
				numAssignments++;

				//move to the next assignment
				if(checkConsistency() && checkHeuristic())
				{		
					solve(level + 1);
				}

				//if this assignment failed at any stage, backtrack
				if(!hasSolution)
				{
					trail.undo();
					numBacktracks++;
				}
				
				else
				{
					return;
				}
			}	
		}	
	}

	@Override
	public void run() {
		solve();
	}
}
