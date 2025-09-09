package netLogoLink;


// partly based on the work of Uri Wilensky's Mathematica link:
//(C) 2007 Uri Wilensky. This code may be freely copied, distributed,
//altered, or otherwise used by anyone for any legal purpose.


import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.workspace.AbstractWorkspace;

import scala.Option;
import scala.collection.Seq;
import scala.collection.JavaConverters;

import org.nlogo.core.Breed;
import org.nlogo.core.CompilerException;
import org.nlogo.core.Program;
import org.nlogo.api.Agent;
import org.nlogo.api.AgentException;
import org.nlogo.api.AgentSet;
import org.nlogo.api.LogoException;
import org.nlogo.api.Turtle;
import org.nlogo.api.World;
import org.nlogo.app.App;
import org.nlogo.agent.Patch;

import java.awt.EventQueue;
import java.awt.Frame;

import javax.swing.JOptionPane;

import java.lang.Thread;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class NetLogoLink {
	private org.nlogo.workspace.Controllable workspace = null;
	private java.io.IOException caughtEx = null;
	private boolean isGUIworkspace;
	private static boolean blockExit = true;

	public NetLogoLink(Boolean isGUImode, Boolean is3d)
	{
		/**
		 * Instantiates a link to netlogo
		 * 
		 * @param isGuiMode	boolean indicated whether netlogo should be run
		 * 		  			with gui or in headless mode
		 * @param is3d      boolean indicated whether to run netlogo in 2d or 
		 * 				   	in 3d mode.
		 * 
		 */
		
		try
		{
			System.setProperty("org.nlogo.is3d", is3d.toString());
			isGUIworkspace = isGUImode.booleanValue();
			if( isGUIworkspace ) {
				App.main( new String[] { } ) ;
				workspace = App.app();
				org.nlogo.api.Exceptions.setHandler
					( new org.nlogo.api.Exceptions.Handler() {
							public void handle( Throwable t ) {
								throw new RuntimeException(t.getMessage());
							} } );
			}
			else
				workspace = HeadlessWorkspace.newInstance() ;
		}
		catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Error in Constructor NLink:"+ex, "Error", JOptionPane.OK_CANCEL_OPTION);			
		}
	}	
	
	public void killWorkspace()
	{
		/**
		 * 	it is not possible to close NetLogo by its own closing method, 
		 *  because it is based on System.exit(0) which will result in a 
		 *  termination of the JVM, jpype and finally python. Therefore, we 
		 *  only dispose the thread, we can find. This is the identical to
		 *  how it is done in RNetLogo.
		 */
		
		try
		{
			NetLogoLink.blockExit = false;

			if (isGUIworkspace) {
				for (int i=0; i<((App)workspace).frame().getFrames().length; i++) {
					((App)workspace).frame();
					java.awt.Frame frame = Frame.getFrames()[i];
					
					frame.dispose();
				}
				Thread.currentThread().interrupt();
			}
			else {
				((HeadlessWorkspace)workspace).dispose();
			}
		}
		catch (Exception ex) {
			JOptionPane.showMessageDialog(null, "Error in killing workspace:"+ex, "Error", JOptionPane.OK_CANCEL_OPTION);
		}
		workspace = null;
		System.gc();
	}


	public void loadModel(final String path)
		throws java.io.IOException, LogoException, InterruptedException, CompilerException, Exception
	{
		/**
		 * load a model
		 * 
		 * @param path	a string with the absolute path of the model
		 * @throws IOException, LogoException, CompilerException, InterruptedException
		 * 
		 */
			caughtEx = null;
			if ( isGUIworkspace ) {
				try {
					EventQueue.invokeAndWait ( 
						new Runnable() {
							public void run() {
								try {
									/* netlogo 6.1*/
									App.app().open(path, true);
								} catch( java.io.IOException ex) {
									{caughtEx = ex; }
								}
							} } );
				}
				catch( java.lang.reflect.InvocationTargetException ex ) {
					JOptionPane.showMessageDialog(null, "Error in loading model:"+ex, "Error", JOptionPane.OK_CANCEL_OPTION);
					throw new RuntimeException(ex.getMessage());
				}
				if( caughtEx != null ) {
					throw caughtEx;
				}
			}
			else {
				try {
					if (workspace != null)
						((HeadlessWorkspace)workspace).dispose();
					workspace = HeadlessWorkspace.newInstance() ;
					workspace.open(path, true);
				}
				catch( java.io.IOException ex) {
					JOptionPane.showMessageDialog(null, "Error in loading model:"+ex, "Error", JOptionPane.OK_CANCEL_OPTION);

					if (workspace != null)
						((HeadlessWorkspace)workspace).dispose();
					workspace = HeadlessWorkspace.newInstance() ;
					throw ex;
				}
			}
	}

	public void command(final String s)
		throws Exception, LogoException, CompilerException
	{
		/**
		 * execute the supplied command in netlogo. This method
		 * is a wrapper around netlogo's command.
		 * 
		 * @param s	a valid netlogo command
		 * @throws LogoException, CompilerException
		 * 
		 */
		
		workspace.command(s);
	}

	/* returns the value of a reporter.  if it is a LogoList, it will be
	recursively converted to an array of Objects */
	public Object report(String s)
		throws Exception, LogoException, CompilerException
	{
		/** 
		 * Every reporter (commands which return a value) that can be called 
		 * in the NetLogo Command Center can be called with this method. 
		 * Like command, it is essentially a wrapper around netlogo's report
		 * 
		 * @param s	a valid netlogo reporter
		 * 
		 */		
		
		NLResult result = new NLResult();
		result.setResultValue(workspace.report(s));
		return result;
	}

    public void sourceFromString(final String source, final Boolean addProcedure)
	throws java.io.IOException, LogoException, CompilerException, InterruptedException
	{
		caughtEx = null;
		if ( isGUIworkspace ) {
			try {
				EventQueue.invokeAndWait (
					new Runnable() {
						public void run() {
							try
							{
								if (addProcedure)
								{
									App.app().setProcedures(App.app().getProcedures()+"\n"+source);
								}
								else
								{
									App.app().setProcedures(source);
								}

								App.app().compile();
							}
							catch( Exception ex)
							{
								//System.out.println("Error: "+ex);
							}
						}
					}
				);
			}
			catch( java.lang.reflect.InvocationTargetException ex ) {
				JOptionPane.showMessageDialog(null, "Error in model from source:"+ex, "Error", JOptionPane.OK_CANCEL_OPTION);
				throw new RuntimeException(ex.getMessage());
			}
			if( caughtEx != null ) {
				throw caughtEx;
			}
		}
	}

	public void doCommandWhile(final String s, final String cond, Integer maxMinutes) throws LogoException, CompilerException
	{
		if (maxMinutes > 0) {
			long startTime = System.currentTimeMillis();
			while (((Boolean)workspace.report(cond)).booleanValue())
			{
				workspace.command(s);
				// max. time exceeded
				if ((System.currentTimeMillis() - startTime) / 60000 >= maxMinutes) {
					//break;
					throw new RuntimeException("Maximum time for NLDoCommandWhile reached. Process stopped.");
				}
			}
		}
		else {
			while (((Boolean)workspace.report(cond)).booleanValue())
			{
				workspace.command(s);
			}
		}
	}

	@SuppressWarnings("unused")
	public Object[] doReportWhile(final String s, final String var, final String condition, Integer maxMinutes)
		throws LogoException, CompilerException, Exception
	{
		java.util.ArrayList<Object> varList = new java.util.ArrayList<Object>();
		if (maxMinutes > 0) {
			long startTime = System.currentTimeMillis();
			for(int i=0; ((Boolean)workspace.report(condition)).booleanValue(); i++) {
				workspace.command(s);
				varList.add(report(var));
				// max. time exceeded
				if ((System.currentTimeMillis() - startTime) / 60000 >= maxMinutes) {
					//break;
					throw new RuntimeException("Maximum time for NLDoReportWhile reached. Process stopped.");
				}
			}
		}
		else {
			for(int i=0; ((Boolean)workspace.report(condition)).booleanValue(); i++) {
				workspace.command(s);
				varList.add(report(var));
			}
		}
		Object[] objArray = varList.toArray();
		return objArray;
	}

	/*
	source from string to add procedures to netlogo
	commandWhile
	reportWhile
	*/
	

	/**
	 * Returns a map of variable names to their inferred types for a given breed of turtles.
	 * The result only includes turtles-own and breed-own variables.
	 */
	public Map<String, String> getBreedVariableTypes(String breedName) {
		Map<String, String> varNameToType = new LinkedHashMap<>();

		AgentSet breedSet = getBreedAgentSet(breedName);
		if (breedSet == null || breedSet.count() == 0) {
			System.out.println("No turtles found for breed: " + breedName);
			return varNameToType;
		}

		// Use the first turtle in the agent set as the type reference
		Turtle sampleTurtle = (Turtle) breedSet.agents().iterator().next();

		World world = getWorld();
		Program program = world.program();

		// turtles-own variables
		Seq<String> turtlesOwn = program.turtlesOwn();
		List<String> turtlesOwnList = JavaConverters.seqAsJavaList(turtlesOwn);

		// breed-own variables
		List<String> breedOwn = new ArrayList<>();
		Option<Breed> breedOpt = program.breeds().get(breedName.toUpperCase());
		if (breedOpt.isDefined()) {
			Breed breed = breedOpt.get();
			breedOwn = JavaConverters.seqAsJavaList(breed.owns());
		}

		int numTurtlesOwn = turtlesOwn.size();

		// turtles-own vars
		for (int i = 0; i < numTurtlesOwn; i++) {
			Object val = sampleTurtle.getVariable(i);
			String typeName = (val == null) ? "null" : val.getClass().getSimpleName();
			varNameToType.put(turtlesOwnList.get(i), typeName);
		}

		// breed-own vars
		for (int j = 0; j < breedOwn.size(); j++) {
			Object val = sampleTurtle.getVariable(numTurtlesOwn + j);
			String typeName = (val == null) ? "null" : val.getClass().getSimpleName();
			varNameToType.put(breedOwn.get(j), typeName);
		}

		return varNameToType;
	}

	private AgentSet getBreedAgentSet(String breedName) {
		World world = getWorld();
		return world.getBreed(breedName.toUpperCase());
	}


	/**
	 * For a given breed and a list of variable names, return a map:
	 *    varName -> List of values (one per turtle in breed, in iteration order).
	 *
	 * Example result:
	 *   { "energy": [23.5, 17.0, 9.2],
	 *     "is-fleeing": [false, true, false] }
	 */
	public Map<String, List<Object>> getBreedVariableCollections(String breedName,
																List<String> variableNames) {

		Map<String, List<Object>> outMap = new LinkedHashMap<>();

		// Early exit if breed is missing
		AgentSet breedSet = getBreedAgentSet(breedName);
		if (breedSet == null || breedSet.count() == 0) {
			System.out.println("No turtles found for breed: " + breedName);
			return outMap;
		}

		// Build variable-name  index map  (re-use helpers)
		World world  = getWorld();
		Program prog = world.program();

		List<String> turtlesOwn = JavaConverters.seqAsJavaList(prog.turtlesOwn());
		Map<String, Integer> varIndex = new HashMap<>();
		for (int i = 0; i < turtlesOwn.size(); i++)
			varIndex.put(turtlesOwn.get(i).toUpperCase(), i);

		Option<Breed> bOpt = prog.breeds().get(breedName.toUpperCase());
		if (bOpt.isDefined()) {
			List<String> breedOwn = JavaConverters.seqAsJavaList(bOpt.get().owns());
			int offset = turtlesOwn.size();
			for (int j = 0; j < breedOwn.size(); j++)
				varIndex.put(breedOwn.get(j).toUpperCase(), offset + j);
		}

		// Initialise output lists
		for (String v : variableNames) outMap.put(v, new ArrayList<>());

		// Collect values
		for (Agent a : breedSet.agents()) {
			Turtle t = (Turtle) a;
			for (String v : variableNames) {
				Integer idx = varIndex.get(v.toUpperCase());
				outMap.get(v).add(idx != null ? t.getVariable(idx) : null);
			}
		}
		return outMap;
	}

	public void setBreedVariableByWho(String breedName, String varName, int[] whoList, Object[] values) throws LogoException, AgentException {
		if (whoList.length != values.length) {
			throw new IllegalArgumentException("whoList and values must have the same length");
		}
		String upperVarName = varName.toUpperCase();

		AgentSet breedSet = getBreedAgentSet(breedName);
		if (breedSet == null || breedSet.count() == 0) {
			System.out.println("No turtles found for breed: " + breedName);
			return;
		}
	
		Turtle sample = (Turtle) breedSet.agents().iterator().next();
		int varIndex = getVariableIndex(sample, breedSet, upperVarName);
		if (varIndex == -1) {
			System.out.println("Variable not found: " + upperVarName);
			return;
		}
	
		// Build map from who to Turtle
		Map<Long, Turtle> whoToTurtle = new HashMap<>();
		for (Agent a : breedSet.agents()) {
			Turtle t = (Turtle) a;
			whoToTurtle.put(t.id(), t);
		}
	
		for (int i = 0; i < whoList.length; i++) {
			Turtle t = whoToTurtle.get((long)whoList[i]);
			if (t != null) {

				Object finalValue = values[i];

				// If it's a Long or Integer, coerce to Double
				if (values[i] instanceof Number && !(values[i] instanceof Double)) {
					finalValue = ((Number) values[i]).doubleValue();
				}

				t.setVariable(varIndex, finalValue);
			}
		}
	}

	/**
	 * Sets a variable (by name) for all turtles of a given breed to a single value.
	 * @throws AgentException 
	 * @throws LogoException 
	 */
	public void setBreedVariable(String breedName, String varName, Object value) throws LogoException, AgentException {
		AgentSet breedSet = getBreedAgentSet(breedName);
		if (breedSet == null || breedSet.count() == 0) {
			System.out.println("No turtles found for breed: " + breedName);
			return;
		}

		String upperVarName = varName.toUpperCase();
		Turtle sample = (Turtle) breedSet.agents().iterator().next();
		int varIndex = getVariableIndex(sample, breedSet, upperVarName);
		if (varIndex == -1) {
			System.out.println("Variable not found: " + upperVarName);
			return;
		}

		for (Agent agent : breedSet.agents()) {
			Turtle t = (Turtle) agent;

			Object finalValue = value;

			// If it's a Long or Integer, coerce to Double
			if (value instanceof Number && !(value instanceof Double)) {
				finalValue = ((Number) value).doubleValue();
			}

			t.setVariable(varIndex, finalValue);
		}
	}


	private int getVariableIndex(Turtle turtle, AgentSet breedSet, String varName) {		
		World world = getWorld();

		List<String> turtlesOwn = JavaConverters.seqAsJavaList(world.program().turtlesOwn());
	
		int index = turtlesOwn.indexOf(varName);
		if (index != -1) {
			return index;
		}
	
		Option<Breed> breedOpt = world.program().breeds().get(breedSet.printName());
		if (breedOpt.isDefined()) {
			Breed breed = breedOpt.get();
			List<String> breedOwn = JavaConverters.seqAsJavaList(breed.owns());
			int breedIndex = breedOwn.indexOf(varName);
			if (breedIndex != -1) {
				return turtlesOwn.size() + breedIndex;
			}
		}
	
		return -1;  // not found
	}

	public List<Object> getPatchVariableValues(String varName) {
		World world = getWorld();
		Program program = world.program();

		// Find index of patch variable
		List<String> patchesOwn = JavaConverters.seqAsJavaList(program.patchesOwn());
		int varIndex = patchesOwn.indexOf(varName);
		if (varIndex == -1) {
			System.out.println("Patch variable not found: " + varName);
			return new ArrayList<>();
		}

		List<Object> values = new ArrayList<>();
		for (Agent a : world.patches().agents()) {
			Patch p = (Patch) a;
			values.add(p.getPatchVariable(varIndex));
		}
		return values;
	}

	public void setPatchVariableByWho(String varName, int[] whoList, Object[] values) 
	{
		World world = getWorld();
		Program program = world.program();

		// Find variable index
		List<String> patchesOwn = JavaConverters.seqAsJavaList(program.patchesOwn());
		int varIndex = patchesOwn.indexOf(varName.toUpperCase());

		if (varIndex == -1) {
			System.out.println("Patch variable not found: " + varName.toUpperCase());
			return;
		}

		// Ensure length match
		if (whoList.length != values.length) {
			throw new IllegalArgumentException("'Who' and 'values' lists must be same length");
		}

		// Map to who values 
		Map<Long, Patch> whoToPatch = new HashMap<>();
		for (Agent a : world.patches().agents()) {
			Patch p = (Patch) a;
			whoToPatch.put(p.id(), p);
		}

		// Actual assignment
		for (int i = 0; i < whoList.length; i++) {
			Patch p = whoToPatch.get((long) whoList[i]);
			if (p != null) {
				Object val = values[i];
				// Netlogo uses doubles and we need to handle this abstraction
				if (val instanceof Number && !(val instanceof Double)) {
					val = ((Number) val).doubleValue();
				}
				try {
					p.setPatchVariable(varIndex, val);
				} catch (AgentException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private World getWorld() {
		if (workspace instanceof HeadlessWorkspace) {
			return ((HeadlessWorkspace) workspace).world();
		}
	
		if (workspace instanceof App) {
			return ((App) workspace).workspace().world();
		}
	
		throw new IllegalStateException("Unsupported workspace type: " + workspace.getClass().getName());
	}



}

 