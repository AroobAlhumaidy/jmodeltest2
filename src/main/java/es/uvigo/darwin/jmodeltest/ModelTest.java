/** 
 * ModelTest.java
 *
 * Description:		Main class for selecting models of nucleotide substitition
 * @author			David Posada, University of Vigo, Spain  
 *					dposada@uvigo.es | darwin.uvigo.es
 * @version			0.0 (May 2006)


 0.1.1 previous version (0.1) was reading Rf as Re

 0.1.2 (July 08) should not not print fT or Rf in the PAUP* block

 0.1.3 (August 08) made GUI componentes resizable

 0.1.4 (March 09) fixed some attempt to access frames when running on the command line. The models CI was not
 well constructed when building a model averaged tree.

 0.1.5 (May 09): present all consensus trees from phylip in a consistent fashion (we remove now previous consense file for any 
	execution of model averaging) and make explicit that they are unrooted. 
	- report ML tree for best fit model if suited. 
	- updated command lines for the latest phmyl version (PhyML v3.0 (179M)).

 0.1.6 (Feb 10): uses PhyML v3.0  (PhyML v3.0_176:178M ) (http://www.atgc-montpellier.fr/phyml/download.php)
	- fixed some attempt to access frames when running on the command line
	 
0.1.7 (Feb 10): uses PhyML v3.0 downloaded from http://code.google.com/p/phyml/
	- checks for 32/64 bits

TODO: check paths for user trees....

 */

package es.uvigo.darwin.jmodeltest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Vector;

import mpi.MPI;
import pal.alignment.Alignment;
import pal.tree.Tree;
import pal.tree.TreeParseException;
import es.uvigo.darwin.jmodeltest.exe.RunConsense;
import es.uvigo.darwin.jmodeltest.exe.RunPhyml;
import es.uvigo.darwin.jmodeltest.exe.RunPhymlHibrid;
import es.uvigo.darwin.jmodeltest.exe.RunPhymlMPJ;
import es.uvigo.darwin.jmodeltest.exe.RunPhymlThread;
import es.uvigo.darwin.jmodeltest.io.TextOutputStream;
import es.uvigo.darwin.jmodeltest.model.Model;
import es.uvigo.darwin.jmodeltest.observer.ConsoleProgressObserver;
import es.uvigo.darwin.jmodeltest.selection.AIC;
import es.uvigo.darwin.jmodeltest.selection.AICc;
import es.uvigo.darwin.jmodeltest.selection.BIC;
import es.uvigo.darwin.jmodeltest.selection.DT;
import es.uvigo.darwin.jmodeltest.selection.HLRT;
import es.uvigo.darwin.jmodeltest.tree.TreeUtilities;
import es.uvigo.darwin.jmodeltest.utilities.Simulation;
import es.uvigo.darwin.prottest.util.fileio.AlignmentReader;

public class ModelTest {

	private ApplicationOptions options = ApplicationOptions.getInstance();

	/** The MPJ rank of the process. It is only useful if MPJ is running. */
	public static int MPJ_ME;
	/** The MPJ size of the communicator. It is only useful if MPJ is running. */
	public static int MPJ_SIZE;
	/** The MPJ running state. */
	public static boolean MPJ_RUN;

	// application constant definitions
	public static final int PRECISION = 4;
	public static final double INFINITY = 9999;
	public static final int MAX_NUM_MODELS = 88;
	public static final int MAX_NAME = 60;
	public static final String CURRENT_VERSION = "2.0";
	public static final String programName = ("jModeltest");
	public static final String URL = "http://darwin.uvigo.es/software/jmodeltest.html";
	public static final String PDF = "/doc/jModelTest." + CURRENT_VERSION
			+ ".pdf";

	private static TextOutputStream MAIN_CONSOLE;
	private static TextOutputStream CURRENT_OUT_STREAM;

	public static String[] arguments;

	private static boolean AICwasCalculated = false;
	private static boolean AICcwasCalculated = false;
	private static boolean BICwasCalculated = false;
	private static boolean DTwasCalculated = false;

	public static Vector<String> testingOrder; // order of the hLRTs
	public static String averagedTreeString; // model-averaged phylogeny in
												// Newick format

	private static AIC myAIC;
	private static AICc myAICc;
	private static BIC myBIC;
	private static DT myDT;
	private static HLRT myHLRT;

	public static Model[] model;
	private static Model minAIC, minAICc, minBIC, minDT, minHLRT, minDLRT;

	// We can work under a GUI or in the command line
	public static boolean buildGUI = true;

	// constructor with GUI
	public ModelTest() {
		XManager.getInstance();
	}

	// constructor without GUI
	public ModelTest(String[] arg) {
		try {
			ParseArguments();
			if (options.doingSimulations) {
				Simulation sim = new Simulation(options);
				sim.run();
			} else
				runCommandLine();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finalize(0);
	}

	/****************************
	 * main ************************************ * Starts the application * * *
	 ***********************************************************************/

	public static void main(String[] args) {
		// initializing MPJ environment (if available)
		try {
			arguments = MPI.Init(args);
			MPJ_ME = MPI.COMM_WORLD.Rank();
			MPJ_SIZE = MPI.COMM_WORLD.Size();
			MPJ_RUN = true;
		} catch (Exception e) {
			MPJ_ME = 0;
			MPJ_SIZE = 1;
			MPJ_RUN = false;
			arguments = args;
		} catch (ExceptionInInitializerError e) {
			e.printStackTrace();
			System.exit(-1);
			MPJ_ME = 0;
			MPJ_SIZE = 1;
			MPJ_RUN = false;
			arguments = args;
		}

		if (arguments.length < 1) {
			buildGUI = true;
			new ModelTest();
		} else {
			buildGUI = false;
			new ModelTest(arguments);
		}

		ApplicationOptions.getInstance().getLogFile().delete();
	}

	/****************************
	 * runCommandLine ************************** * Organizes all the tasks that
	 * the program needs to carry out * * *
	 ***********************************************************************/
	public void runCommandLine() {
		// open mainConsole
		MAIN_CONSOLE = new TextOutputStream(System.out);

		if (MPJ_ME == 0) {
			// print header information
			printHeader(MAIN_CONSOLE);

			// print citation information
			printCitation(MAIN_CONSOLE);

			// print notice information
			printNotice(MAIN_CONSOLE);

			// check expiration date
			// CheckExpiration (mainConsole);

			// print the command line
			MAIN_CONSOLE.println(" ");
			MAIN_CONSOLE.print("Arguments =");
			for (int i = 0; i < arguments.length; i++)
				MAIN_CONSOLE.print(" " + arguments[i]);

			try {
				checkInputFiles();
			} catch (InvalidArgumentException.InvalidInputFileException e) {
				MAIN_CONSOLE.println(e.getMessage());
				finalize(-1);
			}

			// calculate number of models
			if (options.getSubstTypeCode() == 0)
				options.numModels = 3;
			else if (options.getSubstTypeCode() == 1)
				options.numModels = 5;
			else if (options.getSubstTypeCode() == 2)
				options.numModels = 7;
			else
				options.numModels = 11;

			if (options.doF)
				options.numModels *= 2;

			if (options.doI && options.doG)
				options.numModels *= 4;
			else if (options.doI || options.doG)
				options.numModels *= 2;

			// build set of models
			options.setCandidateModels();

		}

		// calculate likelihoods with phyml in the command line
		RunPhyml runPhyml;
		if (MPJ_RUN) {
			if (options.threadScheduling && options.getNumberOfThreads() > 0) {
				runPhyml = new RunPhymlHibrid(MPJ_ME, MPJ_SIZE, new ConsoleProgressObserver(options),
						options, model, options.getNumberOfThreads());
			} else {
				runPhyml = new RunPhymlMPJ(new ConsoleProgressObserver(options),
						options, model);
			}
		} else {
			runPhyml = new RunPhymlThread(new ConsoleProgressObserver(options),
					options, model);
		}
		runPhyml.execute();

		if (MPJ_ME == 0) {

			// do AIC if selected
			if (options.doAIC) {
				myAIC = new AIC(options.writePAUPblock, options.doImportances,
						options.doModelAveraging, options.confidenceInterval);
				myAIC.compute();
				minAIC = myAIC.getMinModel();
				AICwasCalculated = true;
				myAIC.print(MAIN_CONSOLE);
				if (options.doAveragedPhylogeny) {
					new RunConsense(myAIC, options.consensusType,
							options.confidenceInterval);
				}
			}

			// do AICc if selected
			if (options.doAICc) {
				myAICc = new AICc(options.writePAUPblock,
						options.doImportances, options.doModelAveraging,
						options.confidenceInterval);
				myAICc.compute();
				minAICc = myAICc.getMinModel();
				AICcwasCalculated = true;
				myAICc.print(MAIN_CONSOLE);
				if (options.doAveragedPhylogeny) {
					new RunConsense(myAICc, options.consensusType,
							options.confidenceInterval);
				}
			}

			// do BIC if selected
			if (options.doBIC) {
				myBIC = new BIC(options.writePAUPblock, options.doImportances,
						options.doModelAveraging, options.confidenceInterval);
				myBIC.compute();
				minBIC = myBIC.getMinModel();
				BICwasCalculated = true;
				myBIC.print(MAIN_CONSOLE);
				if (options.doAveragedPhylogeny) {
					new RunConsense(myBIC, options.consensusType,
							options.confidenceInterval);
				}
			}

			// do DT if selected
			if (options.doDT) {
				myDT = new DT(options.writePAUPblock, options.doImportances,
						options.doModelAveraging, options.confidenceInterval);
				myDT.compute();
				minDT = myDT.getMinModel();
				DTwasCalculated = true;
				myDT.print(MAIN_CONSOLE);
				if (options.doAveragedPhylogeny) {
					new RunConsense(myDT, options.consensusType,
							options.confidenceInterval);
				}
			}

			// do hLRT if selected
			if (options.doHLRT) {
				myHLRT = new HLRT();
				myHLRT.compute(!options.backwardHLRTSelection,
						options.confidenceLevelHLRT, options.writePAUPblock);
			}

			// do dLRT if selected
			if (options.doDLRT) {
				myHLRT = new HLRT();
				myHLRT.computeDynamical(!options.backwardHLRTSelection,
						options.confidenceLevelHLRT, options.writePAUPblock);
			}
			MAIN_CONSOLE.println(" ");
			MAIN_CONSOLE.println("Program is done.");
			MAIN_CONSOLE.println(" ");

		} // end root

	} // end of runCommandLine

	/****************************
	 * ParseArguments **************************** * Parses the command line for
	 * jModeltest * *
	 ************************************************************************/

	public void ParseArguments() {
		int i, j;
		String arg = "";
		String error = "\nCOMMAND LINE ERROR: ";
		try {
			i = 0;
			while (i < arguments.length) {
				if (!arguments[i].startsWith("-")) {
					System.err
							.println(error
									+ "Arguments must start with \"-\". The ofending argument was: "
									+ arguments[i] + ".");
					PrintUsage();
					System.exit(1);
				}

				arg = arguments[i++];

				if (arg.equals("-d")) {
					if (i < arguments.length) {
						options.setInputFile(new File(arguments[i++]));
					} else {
						System.err.println(error
								+ "-d option requires an input filename.");
						PrintUsage();
					}
				}

				else if (arg.equals("-s")) {
					if (i < arguments.length) {
						String type = arguments[i++];
						try {
							int number = Integer.parseInt(type);
							switch (number) {
							case 3:
								options.setSubstTypeCode(0);
								break;
							case 5:
								options.setSubstTypeCode(1);
								break;
							case 7:
								options.setSubstTypeCode(2);
								break;
							case 11:
								options.setSubstTypeCode(3);
								break;
							default:
								System.err
										.println(error
												+ "-s substitution types have to be 3,5,7,11 only.");
								PrintUsage();
							}
						} catch (NumberFormatException e) {
							System.err
									.println(error
											+ "-s option requires a number for the substitution types: 3,5,7,11.");
							PrintUsage();
						}
					} else {
						System.err
								.println(error
										+ "-s option requires a number for the substitution types: 3,5,7,11.");
						PrintUsage();
					}
				}

				else if (arg.equals("-f")) {
					options.doF = true;
				}

				else if (arg.equals("-i")) {
					options.doI = true;
				}

				else if (arg.equals("-g")) {
					if (i < arguments.length) {
						try {
							options.doG = true;
							String type = arguments[i++];
							options.numGammaCat = Integer.parseInt(type);
						} catch (NumberFormatException e) {
							System.err
									.println(error
											+ "-g option requires a number of gamma categories.");
							PrintUsage();
						}
					} else {
						System.err
								.println(error
										+ "-g option requires a number of gamma categories.");
						PrintUsage();
					}
				}

				else if (arg.equals("-t")) {
					if (i < arguments.length) {
						String type = arguments[i++];

						if (type.equalsIgnoreCase("fixed")) {
							options.fixedTopology = true;
							options.optimizeMLTopology = false;
							options.userTopologyExists = false;
						} else if (type.equalsIgnoreCase("BIONJ")) {
							options.fixedTopology = false;
							options.optimizeMLTopology = false;
							options.userTopologyExists = false;
						} else if (type.equalsIgnoreCase("ML")) {
							options.fixedTopology = false;
							options.optimizeMLTopology = true;
							options.userTopologyExists = false;
						} else {
							System.err
									.println(error
											+ "-t option requires a type of base tree for likelihod calculations: "
											+ "\"fixed\", \"BIONJ\" or \"ML\" only");
							PrintUsage();
						}
					} else {
						System.err
								.println(error
										+ "-t option requires a type of base tree for likelihod calculations: "
										+ "fixed, BIONJ or ML");
						PrintUsage();
					}
				}

				else if (arg.equals("-u")) {
					if (i < arguments.length) {
						options.setInputTreeFile(new File(arguments[i++]));
						options.fixedTopology = false;
						options.optimizeMLTopology = false;
						options.userTopologyExists = true;
					} else {
						System.err
								.println(error
										+ "-u option requires an file name for the tree file");
						PrintUsage();
					}
				}

				else if (arg.equals("-S")) {
					if (i < arguments.length) {
						String type = arguments[i++];

						if (type.equalsIgnoreCase("NNI")) {
							options.treeSearchOperations = ApplicationOptions.TreeSearch.NNI;
						} else if (type.equalsIgnoreCase("SPR")) {
							options.treeSearchOperations = ApplicationOptions.TreeSearch.SPR;
						} else if (type.equalsIgnoreCase("BEST")) {
							options.treeSearchOperations = ApplicationOptions.TreeSearch.BEST;
						} else {
							System.err
									.println(error
											+ "-S option requires a type of tree topology search operation: "
											+ "\"NNI\", \"SPR\" or \"BEST\" only");
							PrintUsage();
						}
					} else {
						System.err
						.println(error
								+ "-S option requires a type of tree topology search operation: "
								+ "\"NNI\", \"SPR\", \"BEST\"");
						PrintUsage();
					}
				}
				
				else if (arg.equals("-AIC")) {
					options.doAIC = true;
				}

				else if (arg.equals("-AICc")) {
					options.doAICc = true;
				}

				else if (arg.equals("-BIC")) {
					options.doBIC = true;
				}

				else if (arg.equals("-DT")) {
					options.doDT = true;
				}

				else if (arg.equals("-p")) {
					options.doImportances = true;
				}

				else if (arg.equals("-v")) {
					options.doImportances = true;
					options.doModelAveraging = true;
				}

				else if (arg.equals("-w")) {
					options.writePAUPblock = true;
				}

				else if (arg.equals("-c")) {
					if (i < arguments.length) {
						try {
							String type = arguments[i++];
							options.confidenceInterval = Double
									.parseDouble(type);
						} catch (NumberFormatException e) {
							System.err
									.println(error
											+ "-c option requires a number (0-1) for the model selection confidence interval.");
							PrintUsage();
						}
					} else {
						System.err
								.println(error
										+ "-c option requires a number (0-1) for the model selection confidence interval.");
						PrintUsage();
					}
				}

				else if (arg.equals("-hLRT")) {
					options.doHLRT = true;
				}

				else if (arg.equals("-o")) {
					if (i < arguments.length) {
						String type = arguments[i++];
						char[] array = type.toCharArray();
						Arrays.sort(array);

						String validString = "";

						if (type.length() == 5) {
							validString = "ftvgp";
						} else if (type.length() == 6) {
							validString = "ftvwgp";
						} else if (type.length() == 7) {
							validString = "ftvwxgp";
						} else {
							System.err
									.println(error
											+ "-o option requires a 5, 6 or 7 specific letter string with the order of tests (ftvgp/ftvwgp/ftvwxgp)");
							PrintUsage();
						}

						char[] valid = validString.toCharArray();
						if (!Arrays.equals(array, valid)) {
							System.err
									.println(error
											+ "-o option requires a 5, 6 or 7 specific letter string with the order of tests (ftvgp/ftvwgp/ftvwxgp)");
							PrintUsage();
						} else {
							testingOrder = new Vector<String>();
							for (j = 0; j < type.length(); j++) {
								if (type.charAt(j) == 'f')
									testingOrder.addElement("freq");
								else if (type.charAt(j) == 't')
									testingOrder.addElement("titv");
								else if (type.charAt(j) == 'v') {
									if (options.getSubstTypeCode() == 0)
										testingOrder.addElement("2ti4tv");
									else if (options.getSubstTypeCode() >= 1)
										testingOrder.addElement("2ti");
								} else if (type.charAt(j) == 'w') {
									if (options.getSubstTypeCode() >= 1)
										testingOrder.addElement("2tv");
								} else if (type.charAt(j) == 'x') {
									if (options.getSubstTypeCode() > 1)
										testingOrder.addElement("4tv");
								} else if (type.charAt(j) == 'g')
									testingOrder.addElement("gamma");
								else if (type.charAt(j) == 'p')
									testingOrder.addElement("pinv");
							}
						}
					}
				}

				else if (arg.equals("-dLRT")) {
					options.doDLRT = true;
				}

				else if (arg.equals("-r")) {
					options.backwardHLRTSelection = true;
				}

				else if (arg.equals("-h")) {
					if (i < arguments.length) {
						try {
							String type = arguments[i++];
							options.confidenceLevelHLRT = Double
									.parseDouble(type);
						} catch (NumberFormatException e) {
							System.err
									.println(error
											+ "-hoption requires a number (0-1) for the hLRT confidence interval.");
							PrintUsage();
						}
					} else {
						System.err
								.println(error
										+ "-hoption requires a number (0-1) for the hLRT confidence interval.");
						PrintUsage();
					}
				}

				else if (arg.equals("-a")) {
					options.doAveragedPhylogeny = true;
				}

				else if (arg.equals("-z")) {
					options.consensusType = "strict";
				}

				else if (arg.equals("-sims")) {
					if (i < arguments.length) {
						options.simulationsName = arguments[i++];
						options.doingSimulations = true;
					} else {
						System.err
								.println(error
										+ "-sims option requires a name for the simulations files");
						PrintUsage();
					}

				} else if (arg.equals("-tr")) {
					if (i < arguments.length) {
						try {
							String type = arguments[i++];
							options.setNumberOfThreads(Integer.parseInt(type));
							options.threadScheduling = true;
						} catch (NumberFormatException e) {
							System.err
									.println(error
											+ "-tr option requires the number of processors to compute.");
							PrintUsage();
						}
					}
					
					else {
						System.err
								.println(error
										+ "-tr option requires the number of processors to compute.");
						PrintUsage();
					}

				} else {
					System.err.println(error + "the argument \" " + arg
							+ "\" is unknown. Check its syntax.");
					PrintUsage();
				}

			} // while
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/****************************
	 * PrintUsage **************************** * Prints command line usage * *
	 ************************************************************************/
	static public void PrintUsage() {
		String usage = "\njModelTest command usage"
				+ "\n -d: input data file (e.g., -d data.phy)"
				+ "\n -s: number of substitution schemes (e.g., -s 11) (it has to be 3,5,7,11; default is 3)"
				+ "\n -f: include models with unequals base frecuencies (e.g., -f) (default is false)"
				+ "\n -i: include models with a proportion invariable sites (e.g., -i) (default is false)"
				+ "\n -g: include models with rate variation among sites and number of categories (e.g., -g 8) (default is false & 4 categories)"
				+ "\n -t: base tree for likelihood calculations (fixed (BIONJ-JC), BIONJ, ML) (e.g., -t BIONJ) (default is ML)"
				+ "\n -u: user tree for likelihood calculations  (e.g., -u data.tre)"
				+ "\n -S: tree topology search operation option (NNI (fast), SPR (a bit slower), BEST (best of NNI and SPR)) (default is NNI)"
				+ "\n -n: sample size (-n235) (default is  the number of sites)"
				+ "\n -AIC: calculate the Akaike Information Criterion (e.g., -AIC) (default is false)"
				+ "\n -AICc: calculate the corrected Akaike Information Criterion (e.g., -AICc) (default is false)"
				+ "\n -BIC: calculate the Bayesian Information Criterion (e.g., -BIC) (default is false)"
				+ "\n -DT: calculate the decision theory criterion (e.g., -DT) (default is false)"
				+ "\n -p: calculate parameter importances (e.g., -p) (default is false)"
				+ "\n -v: do model averaging and parameter importances (e.g., -v) (default is false)"
				+ "\n -c: confidence interval (e.g., -c90) (default is 100)"
				+ "\n -w: write PAUP block (e.g., -w) (default is false)"
				+ "\n -dLRT: do dynamical likelihood ratio tests (e.g., -dLRT)(default is false)"
				+ "\n -hLRT: do hierarchical likelihood ratio tests (default is false)"
				+ "\n -o: hypothesis order for the hLRTs (e.g., -hLRT gpftv) (default is ftvgp/ftvwgp/ftvwxgp)"
				+ "\n        f=freq, t=titvi, v=2ti4tv(subst=3)/2ti(subst>3), w=2tv, x=4tv, g=gamma, p=pinv"
				+ "\n -r: backward selection for the hLRT (e.g., -r) (default is forward)"
				+ "\n -h: confidence level for the hLRTs (e.g., -a0.002) (default is 0.01)"
				+ "\n -a: estimate model-averaged phylogeny for each active criterion (e.g., -a) (default is false)"
				+ "\n -z: strict consensus type for model-averaged phylogeny (e.g., -z) (default is majority rule)"
				+ "\n -tr: number of threads to execute (default is 1)"
				+ "\n\n Command line: java -jar jModeltest.jar [arguments]";
		System.err.println(usage);
		System.err.println(" ");
		System.exit(1);

	}

	/****************************
	 * printHeader ****************************** * Prints header information at
	 * start * * *
	 ***********************************************************************/

	static public void printHeader(TextOutputStream stream) {
		// we can set styles using the editor pane
		// I am using doc to stream ....
		stream.print("----------------------------- ");
		stream.print("jModeltest " + CURRENT_VERSION);
		stream.println(" -----------------------------");
		stream.print("(c) 2008-onwards David Posada, ");
		stream.println("Department of Biochemistry, Genetics and Immunology");
		stream.println("University of Vigo, 36310 Vigo, Spain. e-mail: dposada@uvigo.es");
		stream.println("--------------------------------------------------------------------------");

		java.util.Date current_time = new java.util.Date();
		stream.print(current_time.toString());
		stream.println("  (" + System.getProperty("os.name") + " "
				+ System.getProperty("os.version") + ", arch: "
				+ System.getProperty("os.arch") + ", bits: "
				+ System.getProperty("sun.arch.data.model") + ")");
		stream.println(" ");
	}

	/****************************
	 * printNotice ****************************** * Prints notice information at
	 * start up * * *
	 ***********************************************************************/

	static public void printNotice(TextOutputStream stream) {
		// stream.println("\n******************************* NOTICE ************************************");
		stream.println("Notice: This program may contain errors. Please inspect results carefully.");
		stream.println(" ");
		// stream.println("***************************************************************************\n");

	}

	/****************************
	 * printCitation ****************************** * Prints citation
	 * information at start up * * *
	 ***********************************************************************/

	static public void printCitation(TextOutputStream stream) {
		// stream.println("\n******************************* CITATION *********************************");
		stream.println("Citation: Posada D. 2008. jModelTest: Phylogenetic Model Averaging.");
		stream.println("          Molecular Biology and Evolution 25: 1253-1256.");
		stream.println(" ");
		// stream.println("***************************************************************************\n");

	}

	public static void CheckExpiration(TextOutputStream stream) {
		java.util.Calendar now = java.util.Calendar.getInstance();

		if ((now.get(Calendar.MONTH) != Calendar.JUNE && now
				.get(Calendar.MONTH) != Calendar.DECEMBER)
				|| now.get(Calendar.YEAR) != 2007) {
			stream.println("Program has expired! \n    Bye...");
			System.exit(0);
		}
	}

	/****************************
	 * writePaupBlockk *************************** * Prints a block of PAUP
	 * commands for the best model * *
	 ************************************************************************/
	static public void WritePaupBlock(TextOutputStream stream,
			String criterion, Model model) {
		try {
			stream.println("\n--\nPAUP* Commands Block:");
			stream.println(" If you want to load the selected model and associated estimates in PAUP*,");
			stream.println(" attach the next block of commands after the data in your PAUP file:");
			stream.print("\n[!\nLikelihood settings from best-fit model (");
			stream.printf("%s", model.getName());
			stream.print(") selected by ");
			stream.printf("%s", criterion);
			stream.print("\nwith ");
			stream.printf("%s", programName);
			stream.print(" ");
			stream.printf("%s", CURRENT_VERSION);
			stream.print(" on ");
			java.util.Date current_time = new java.util.Date();
			stream.print(current_time.toString());
			stream.println("]");

			stream.print("\nBEGIN PAUP;");
			stream.print("\nLset");

			/* base frequencies */
			stream.print(" base=");
			if (model.ispF()) {
				stream.print("(");
				stream.printf("%.4f ", model.getfA());
				stream.printf("%.4f ", model.getfC());
				stream.printf("%.4f ", model.getfG());
				/* stream.printf("%.4f",model.fT); */
				stream.print(")");
			} else
				stream.print("equal");

			/* substitution rates */
			if (!model.ispT() && !model.ispR())
				stream.print(" nst=1");
			else if (model.ispT()) {
				stream.print(" nst=2 tratio=");
				stream.printf("%.4f", model.getTitv());
			} else if (model.ispR()) {
				stream.print(" nst=6  rmat=(");
				stream.printf("%.4f ", model.getRa());
				stream.printf("%.4f ", model.getRb());
				stream.printf("%.4f ", model.getRc());
				stream.printf("%.4f ", model.getRd());
				stream.printf("%.4f ", model.getRe());
				/* stream.print("1.0000)"); */
			}

			/* site rate variation */
			stream.print(" rates=");
			if (model.ispG()) {
				stream.print("gamma shape=");
				stream.printf("%.4f", model.getShape());
				stream.print(" ncat=");
				stream.printf("%d", model.getNumGammaCat());
			} else
				stream.print("equal");

			/* invariable sites */
			stream.print(" pinvar=");
			if (model.ispI())
				stream.printf("%.4f", model.getPinv());
			else
				stream.print("0");

			stream.print(";\nEND;");
			stream.print("\n--");
		}

		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void checkInputFiles() {
		// open data file
		File inputFile = options.getInputFile();
		MAIN_CONSOLE.print("\n\nReading data file \"" + inputFile.getName()
				+ "\"...");

		if (inputFile.exists()) {

			try {

//				File outputFile = File
//						.createTempFile("jmodeltest", "input.aln");
				ModelTestService.readAlignment(inputFile, options.getAlignmentFile());

				Alignment alignment = AlignmentReader.readAlignment(
						new PrintWriter(System.err),
						options.getAlignmentFile().getAbsolutePath(), true); // file
				options.numTaxa = alignment.getSequenceCount();
				options.numSites = alignment.getSiteCount();
				options.numBranches = 2 * options.numTaxa - 3;

				MAIN_CONSOLE.println(" OK.");
				MAIN_CONSOLE.println("  number of sequences: "
						+ options.numTaxa);
				MAIN_CONSOLE.println("  number of sites: " + options.numSites);
				options.sampleSize = options.numSites;
			} catch (Exception e)// file cannot be read correctly
			{
				System.err.println("\nThe specified file \""
						+ inputFile.getAbsolutePath()
						+ "\" cannot be read as an alignment");
				MAIN_CONSOLE.println(" failed.\n");
				throw new InvalidArgumentException.InvalidAlignmentFileException(
						inputFile);
			}
		} else // file does not exist
		{
			System.err.println("\nThe specified file \""
					+ inputFile.getAbsolutePath() + "\" cannot be found");
			MAIN_CONSOLE.println(" failed.\n");
			throw new InvalidArgumentException.UnexistentAlignmentFileException(
					inputFile);
		}

		// open tree file if necessary
		if (options.userTopologyExists) {
			File treefile = options.getInputTreeFile();
			MAIN_CONSOLE.print("Reading tree file \"" + treefile.getName()
					+ "\"...");

			// read the tree in
			Tree tree = null;
			try {
				tree = TreeUtilities.readTree(treefile.getAbsolutePath());
			} catch (IOException e) {
				System.err.println("\nThe specified tree file \""
						+ treefile.getName() + "\" cannot be found");
				MAIN_CONSOLE.println(" failed.\n");
				throw new InvalidArgumentException.UnexistentTreeFileException(
						treefile.getAbsolutePath());
			} catch (TreeParseException e) {
				System.err.println("\nCannot parse tree file \""
						+ treefile.getName() + "\"");
				MAIN_CONSOLE.println(" failed.\n");
				throw new InvalidArgumentException.InvalidTreeFileException(
						treefile.getAbsolutePath());
			}
			if (tree != null) {
				options.setUserTree(TreeUtilities.toNewick(tree, true, false,
						false));
				TextOutputStream out = new TextOutputStream(options
						.getTreeFile().getAbsolutePath());
				out.print(options.getUserTree());
				out.close();
				MAIN_CONSOLE.println(" OK.");
			} else // tree is not valid
			{
				System.err.println("\nUnexpected error parsing \""
						+ treefile.getName() + "\"");
				MAIN_CONSOLE.println(" failed.\n");
				throw new InvalidArgumentException.InvalidTreeFileException(
						treefile.getAbsolutePath());
			}

		}
	}

	public static TextOutputStream setMainConsole(TextOutputStream mainConsole) {
		ModelTest.MAIN_CONSOLE = mainConsole;
		return mainConsole;
	}

	public static TextOutputStream getMainConsole() {
		return MAIN_CONSOLE;
	}

	public static TextOutputStream getCurrentOutStream() {
		return CURRENT_OUT_STREAM;
	}

	public static void setCurrentOutStream(TextOutputStream currentOutStream) {
		CURRENT_OUT_STREAM = currentOutStream;
	}

	public static AIC getMyAIC() {
		if (!AICwasCalculated)
			throw new WeakStateException.UninitializedCriterionException("AIC");
		return myAIC;
	}

	public static void setMyAIC(AIC myAIC) {
		ModelTest.myAIC = myAIC;
		ModelTest.minAIC = myAIC != null ? myAIC.getMinModel() : null;
		AICwasCalculated = (myAIC != null);
	}

	public static boolean testAIC() {
		return AICwasCalculated;
	}

	public static AICc getMyAICc() {
		if (!AICcwasCalculated)
			throw new WeakStateException.UninitializedCriterionException("AICc");
		return myAICc;
	}

	public static void setMyAICc(AICc myAICc) {
		ModelTest.myAICc = myAICc;
		ModelTest.minAICc = myAICc != null ? myAICc.getMinModel() : null;
		AICcwasCalculated = (myAICc != null);
	}

	public static boolean testAICc() {
		return AICcwasCalculated;
	}

	public static BIC getMyBIC() {
		if (!BICwasCalculated)
			throw new WeakStateException.UninitializedCriterionException("BIC");
		return myBIC;
	}

	public static void setMyBIC(BIC myBIC) {
		ModelTest.myBIC = myBIC;
		ModelTest.minBIC = myBIC != null ? myBIC.getMinModel() : null;
		BICwasCalculated = (myBIC != null);
	}

	public static boolean testBIC() {
		return BICwasCalculated;
	}

	public static DT getMyDT() {
		if (!DTwasCalculated)
			throw new WeakStateException.UninitializedCriterionException("DT");
		return myDT;
	}

	public static void setMyDT(DT myDT) {
		ModelTest.myDT = myDT;
		ModelTest.minDT = myDT != null ? myDT.getMinModel() : null;
		DTwasCalculated = (myDT != null);
	}

	public static boolean testDT() {
		return DTwasCalculated;
	}

	/**
	 * Finalizes the MPJ runtime environment. When an error occurs, it aborts
	 * the execution of every other processes.
	 * 
	 * @param status
	 *            the finalization status
	 */
	public static void finalize(int status) {

		if (status != 0) {
			if (MPJ_RUN) {
				MPI.COMM_WORLD.Abort(status);
			}
		}

		if (MPJ_RUN) {
			MPI.Finalize();
		}

		System.exit(status);

	}

	/**
	 * @param minDLRT
	 *            the minDLRT to set
	 */
	public static void setMinDLRT(Model minDLRT) {
		ModelTest.minDLRT = minDLRT;
	}

	/**
	 * @return the minDLRT
	 */
	public static Model getMinDLRT() {
		return minDLRT;
	}

	/**
	 * @param minHLRT
	 *            the minHLRT to set
	 */
	public static void setMinHLRT(Model minHLRT) {
		ModelTest.minHLRT = minHLRT;
	}

	/**
	 * @return the minHLRT
	 */
	public static Model getMinHLRT() {
		return minHLRT;
	}

	/**
	 * @return the minDT
	 */
	public static Model getMinDT() {
		return minDT;
	}

	/**
	 * @return the minBIC
	 */
	public static Model getMinBIC() {
		return minBIC;
	}

	/**
	 * @return the minAICc
	 */
	public static Model getMinAICc() {
		return minAICc;
	}

	/**
	 * @return the minAIC
	 */
	public static Model getMinAIC() {
		return minAIC;
	}
} // class ModelTest
