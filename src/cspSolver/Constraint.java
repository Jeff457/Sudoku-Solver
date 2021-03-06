package cspSolver;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Constraint represents a NotEquals constraint on a set of variables. 
 * Used to ensure none of the variables contained in the constraint have the same assignment. 
 */
public class Constraint{ 
	
	//===============================================================================
	// Properties 
	//===============================================================================

	public List<Variable> vars;

	//===============================================================================
	// Constructors
	//===============================================================================

	public Constraint()
	{
		vars = new ArrayList<Variable>();
	}
	
	public Constraint(List<Variable> vars)
	{
		this.vars = vars;
	}
	
	//===============================================================================
	// Modifiers
	//===============================================================================

	public void addVariable(Variable v)
	{
		vars.add(v);
	}
		
	//===============================================================================
	// Accessors
	//===============================================================================
	
	public int size()
	{
		return vars.size();
	}
	
	/**
	 * 
	 * @param v a Variable
	 * @return true if v is in the constraint, false otherwise
	 */
	public boolean contains(Variable v)
	{
		return vars.contains(v) ? true: false;
	}
	
	/**
	 * Returns whether or not the a variable in the constraint has been modified.
	 * 
	 * @return
	 */
	public boolean isModified()
	{
		for (Variable var : vars)
		{
			if (var.isModified())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Used for local search
	 * @return
	 */
	public int getConflicts()
	{
		int numConflicts = 0;
		for(Variable var : vars)
		{
			for (Variable otherVar : vars)
			{
				if(var.equals(otherVar))
				{
					continue;
				}
				else if(var.getAssignment().equals(otherVar.getAssignment()))
				{
					numConflicts++;
				}
			}
		}
		
		return numConflicts;
	}
	
	//===============================================================================
	// Modifiers
	//===============================================================================
	
	/**
	 * Attempts to propagate the notequal constraint through the variables in 
	 * the constraint. If it fails to do so, the propagation stops midway
	 * and does not reset the changes to the domains of the variables made
	 * so far. 
	 * @return true if the constraint is consistent and propagation succeeds, 
	 * false otherwise
	 */
	public boolean propagateConstraint()
	{
		//compares assignments and determines if the assigment breaks the constraints
		for(Variable var : vars)
		{
			if(!var.isAssigned())
				continue;
			Integer varAssignment = var.getAssignment();
			for (Variable otherVar : vars)
			{
				if(var.equals(otherVar))
				{
					continue;
				}
				if (otherVar.size() == 1 && otherVar.getAssignment() == varAssignment)
				{
					return false;
				}
				otherVar.removeValueFromDomain(varAssignment);
			}
		}
		return true;
	}

	/**
	 * TODO: Implement naked pairs
	 * @return
	 */
	public boolean propagateNakedPairs()
	{
		List<Variable> pairs = new ArrayList<>();
		List<Variable> nakedPairs = new ArrayList<>(2);

		for (Variable v : vars)  // get all Variables with only 2 values left in its Domain
		{
			if (v.isAssigned())
				continue;
			if (v.getDomain().size() == 2)
				pairs.add(v);
		}

		outer:
		for (int i = 0; i < pairs.size() - 1; i++)  // for every Variable with 2 values left in its Domain
		{
			for (int j = i+1; j < pairs.size(); j++)
			{
				if (pairs.get(i).getDomain().isSubsetOf(pairs.get(j).getDomain()))  // check if another Variable has those same 2 values left in Domain
				{
					nakedPairs.add(pairs.get(i));
					nakedPairs.add(pairs.get(j));
					break outer;

				}
			}
		}

		if (nakedPairs.size() > 0)  // if we found any naked pairs
		{
			Domain domain = nakedPairs.get(0).getDomain();  // both variables have the same domain, so only need to grab 1
			for (Variable v : vars)
			{
				if (!v.isAssigned() && !nakedPairs.contains(v))
					v.removeValuesFromDomain(domain);
			}
			return true;
		}

		return false;
	}

	/**
	 * TODO: Implement naked triples
	 */
	public boolean propagateNakedTriples()
	{
		ArrayList<Variable> threes = new ArrayList<>();
		ArrayList<Variable> twos = new ArrayList<>();

		for(Variable v : vars) {
			if(v.isAssigned())
				continue;
			if (v.getDomain().size() == 3)
				threes.add(v);
			else if(v.getDomain().size() == 2)
				twos.add(v);
		}

		ArrayList<Variable> targets = null;
		outer:
		for(int i = 0; i < threes.size(); i++)
		{
			targets = new ArrayList<>(3);
			targets.add(threes.get(i));
			for(int j = 0; j < threes.size(); j++)
			{
				if(j == i)
					continue;

				if(threes.get(j).getDomain().isSubsetOf(threes.get(i).getDomain()))
					targets.add(threes.get(j));

				if(targets.size() == 3) //found naked triple
					break outer;
			}

			for(int j = 0; j < twos.size(); j++)
			{
				if(twos.get(j).getDomain().isSubsetOf(threes.get(i).getDomain()))
					targets.add(twos.get(j));

				if(targets.size() == 3) //found naked triple
					break outer;
			}
		}

		if(targets != null && targets.size() == 3) //Then we know this is a naked triple
		{
			Domain d = targets.get(0).getDomain();
			for(Variable v : vars)
			{
				if(!targets.contains(v))
					v.removeValuesFromDomain(d);
			}
			return true;
		}

		return false;
	}

	/**
	 * Used for local search. Same as propagate constraint. 
	 * @return true if constraint is consistent, false otherwise. 
	 */
	public boolean isConsistent()
	{
		return propagateConstraint();
	}

	//===============================================================================
	// String representation
	//===============================================================================
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder("{");
		String delim = "";
		for(Variable v : vars)
		{
			sb.append(delim).append(v.getName());
			delim = ",";
		}
		sb.append("}");
		return sb.toString();
	}

	
}
