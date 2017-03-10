package scripts;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import sudoku.SudokuBoardReader;
import sudoku.SudokuFile;
import cspSolver.BTSolver;
import cspSolver.BTSolver.ConsistencyCheck;
import cspSolver.BTSolver.ValueSelectionHeuristic;
import cspSolver.BTSolver.VariableSelectionHeuristic;

public class BTSolverStats {
	public static List<SudokuFile> getPuzzlesFromFolder(File folder) {
	    List<SudokuFile> puzzles = new ArrayList<SudokuFile>();
		for (File fileEntry : folder.listFiles()) {
    		SudokuFile sfPE = SudokuBoardReader.readFile(fileEntry.getPath());
    		if(sfPE.getN() != 0)
    		{
    			puzzles.add(sfPE);
    		}
	    }
		return puzzles;
	}
	
	public static runStats testSolver(BTSolver solver, ConsistencyCheck consistencyCheck, ValueSelectionHeuristic valueSelect, VariableSelectionHeuristic varSelect, BTSolver.HeuristicCheck heurCheck)
	{
		solver.setConsistencyChecks(consistencyCheck);
		solver.setValueSelectionHeuristic(valueSelect);
		solver.setVariableSelectionHeuristic(varSelect);
		solver.setHeuristicCheck(heurCheck);
		
		Thread t1 = new Thread(solver);
		try
		{
			t1.start();
			t1.join(60000);
			if(t1.isAlive())
			{
				t1.interrupt();
			}
		}catch(InterruptedException e)
		{
		}
		long runtime;
		int numAssignments;
		int numBacktracks;
		boolean isSolution;
		runStats rs;
		runtime = solver.getTimeTaken();
		numAssignments = solver.getNumAssignments();
		numBacktracks = solver.getNumBacktracks();
		isSolution = solver.hasSolution();
		rs = new runStats(runtime, numAssignments, numBacktracks, isSolution);
		return rs;
	}
	
	public static void main(String[] args)
	{
		long startTime = System.currentTimeMillis();

		File results = new File("BTSolverResultsTable.txt");
		File folder = new File("ExampleSudokuFiles/");

		List<SudokuFile> puzzles = getPuzzlesFromFolder(folder);
		List<runStats> easyStats = new ArrayList<runStats>();
		List<runStats> mediumStats = new ArrayList<runStats>();
		List<runStats> hardStats = new ArrayList<runStats>();

		List<SudokuFile> easyPuzzles = puzzles.subList(0,5);
		List<SudokuFile> mediumPuzzles = puzzles.subList(55,60);
		List<SudokuFile> hardPuzzles = puzzles.subList(50,55);

		ArrayList<String> output = new ArrayList<>();

		int i = 1;
		outer:
		for(ConsistencyCheck consistencyCheck : ConsistencyCheck.values())
		{
			for(BTSolver.HeuristicCheck heuristicCheck : BTSolver.HeuristicCheck.values())
			{
				for(VariableSelectionHeuristic variableSelect : VariableSelectionHeuristic.values())
				{
					for(ValueSelectionHeuristic valueSelect : ValueSelectionHeuristic.values())
					{
						System.out.println("Starting next combination.");
						easyStats.clear();
						mediumStats.clear();
						hardStats.clear();

						for(SudokuFile puzzle : easyPuzzles)
							easyStats.add(testSolver(new BTSolver(puzzle),consistencyCheck,valueSelect,variableSelect,heuristicCheck));
						for(SudokuFile puzzle : mediumPuzzles)
							mediumStats.add(testSolver(new BTSolver(puzzle),consistencyCheck,valueSelect,variableSelect,heuristicCheck));
						for(SudokuFile puzzle : hardPuzzles)
							hardStats.add(testSolver(new BTSolver(puzzle),consistencyCheck,valueSelect,variableSelect,heuristicCheck));

						output.add(statsToString(consistencyCheck,valueSelect,variableSelect,heuristicCheck,easyStats,mediumStats,hardStats));
						writeStrings(results,output);

						System.out.println("Completed combination "+i+"/72.");
						i++;
					}
				}
			}
		}

		long elapsedTime = System.currentTimeMillis()-startTime;
		System.out.println("Total elapsed time is: "+elapsedTime+" milliseconds.");
	}

	private static void writeStrings(File file, List<String> strings)
	{
		try{
			if(file.exists())
				file.delete();

			FileWriter fw = new FileWriter(file);

			for(String str : strings)
				fw.write(str+System.lineSeparator());

			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String statsToString(ConsistencyCheck consistencyCheck,
										ValueSelectionHeuristic valueSelectionHeuristic,
										VariableSelectionHeuristic variableSelectionHeuristic,
										BTSolver.HeuristicCheck heuristicCheck,
										List<runStats> easy,
										List<runStats> medium,
										List<runStats> hard)
	{
		int easyPercent = 0;
		for(runStats s : easy)
			if(s.isSolution())
				easyPercent++;
		easyPercent = (int)(((double)easyPercent/(double)easy.size())*100);

		int medPercent = 0;
		for(runStats s : medium)
			if(s.isSolution())
				medPercent++;
		medPercent = (int)(((double)medPercent/(double)medium.size())*100);

		int hardPercent = 0;
		for(runStats s : hard)
			if(s.isSolution())
				hardPercent++;
		hardPercent = (int)(((double)hardPercent/(double)hard.size())*100);

		List<runStats> all = new ArrayList<>(easy);
		all.addAll(medium);
		all.addAll(hard);

		long totalRunTime = 0;
		long totalAssignments = 0;
		long totalBackTracks = 0;
		int totalSuccessful = 0;

		for(runStats rs : all)
		{
			if(rs.isSolution())
			{
				totalRunTime += rs.getRuntime();
				totalAssignments += rs.getNumAssignments();
				totalBackTracks += rs.getNumBacktracks();
				totalSuccessful++;
			}
		}

		boolean completedNone = false;
		if(totalSuccessful == 0)
		{
			totalSuccessful = 1;
			completedNone = true;
		}

		StringBuilder builder = new StringBuilder();
		builder
				.append(centerPad(consistencyCheck.name(),16)) //Consistency check type
				.append(" | ")
				.append(centerPad(heuristicCheck.name(),13)) //Heuristic type
				.append(" | ")
				.append(centerPad(variableSelectionHeuristic.name(),22)) //Variable select type
				.append(" | ")
				.append(centerPad(valueSelectionHeuristic.name(),23)) //Value select type
				.append(" | ");

		builder
				.append(centerPad(String.valueOf((int)(totalRunTime/totalSuccessful)),15)) //Average run time
				.append(" | ")
				.append(centerPad(String.valueOf((int)(totalAssignments/totalSuccessful)),10)) //Average assignments
				.append(" | ")
				.append(centerPad(String.valueOf((int)(totalBackTracks/totalSuccessful)),10)) //Average backtracks
				.append(" | ");

		builder
				.append(centerPad(String.valueOf((int)easyPercent),4)) //Percent of easy problems finished
				.append(" | ")
				.append(centerPad(String.valueOf((int)medPercent),4)) //Percent of medium problems finished
				.append(" | ")
				.append(centerPad(String.valueOf((int)hardPercent),4))//Percent of hard problems finished
				.append(" | ");

		if(completedNone)
			builder.append(centerPad("*None solved*","*None solved*".length()+2)).append(" | ");

		return builder.toString();
	}

	private static String centerPad(String string, int size)
	{
		int padSize = size - string.length();
		int padStart = string.length() + padSize / 2;

		string = String.format("%" + padStart + "s", string);
		string = String.format("%-" + size  + "s", string);
		return string;
	}
}
