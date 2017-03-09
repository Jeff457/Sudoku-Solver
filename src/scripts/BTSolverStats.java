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
		for(ValueSelectionHeuristic valueSelect : ValueSelectionHeuristic.values())
		{
			for(VariableSelectionHeuristic variableSelect : VariableSelectionHeuristic.values())
			{
				for(ConsistencyCheck consistencyCheck : ConsistencyCheck.values())
				{
					for(BTSolver.HeuristicCheck heuristicCheck : BTSolver.HeuristicCheck.values())
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

						System.out.println("Completed combination "+i+"/72.");
						i++;
						if(i == 11)
							break outer;
					}
				}
			}
		}

		try{
			FileWriter fw = new FileWriter(results);

			for(String str : output)
				fw.write(str+System.lineSeparator());
			/*long totalRunTime = 0;
			long totalAssignments = 0;
			long totalBackTracks = 0;
			int totalSuccessful = 0;
			int totalPuzzles = 0;
			
			for(runStats rs : statistics)
			{
				if(rs.isSolution())
				{
					totalRunTime += rs.getRuntime();
					totalAssignments += rs.getNumAssignments();
					totalBackTracks += rs.getNumBacktracks();
					totalSuccessful++;
				}
				totalPuzzles++;
			}
			
			System.out.print("Solution found for " + totalSuccessful + "/" + totalPuzzles + "puzzles" + sep);
			System.out.print("average runTime: " + (totalRunTime/totalSuccessful) + sep);
			System.out.print("average number of assignments per puzzle: " + (totalAssignments/totalSuccessful) + sep);
			System.out.print("average number of backtracks per puzzle: " + (totalBackTracks/totalSuccessful) + sep);
			fw.write("Consistency Check: " + cc + sep);
			fw.write("ValueSelectionHeuristic: " + valsh + sep);
			fw.write("VariableSelectionHeuristic: " + varsh + sep);
			fw.write("Solution found for " + totalSuccessful + "/" + totalPuzzles + "puzzles" + sep);
			fw.write("average runTime: " + (totalRunTime/totalSuccessful) + sep);
			fw.write("average number of assignments per puzzle: " + (totalAssignments/totalSuccessful) + sep);
			fw.write("average number of backtracks per puzzle: " + (totalBackTracks/totalSuccessful) + sep);*/

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

		StringBuilder builder = new StringBuilder();
		builder
				.append(String.format("%16s",consistencyCheck.name()))//16
				.append(" | ")
				.append(String.format("%23s",valueSelectionHeuristic.name()))//16
				.append(" | ")
				.append(String.format("%22s",variableSelectionHeuristic.name()))//16
				.append(" | ")
				.append(String.format("%13s", heuristicCheck.name()))//16
				.append(" | ");

		builder
				.append(String.format("%15s",(int)(totalRunTime/totalSuccessful)))
				.append(" | ")
				.append(String.format("%10s",(int)(totalAssignments/totalSuccessful)))
				.append(" | ")
				.append(String.format("%10s",(int)(totalBackTracks/totalSuccessful)))
				.append(" | ");

		builder
				.append(String.format("%4s%%",(int)(easyPercent)))
				.append(" | ")
				.append(String.format("%4s%%",(int)(medPercent)))
				.append(" | ")
				.append(String.format("%4s%%",(int)(hardPercent)))
				.append(" | ");


		return builder.toString();
	}
}
