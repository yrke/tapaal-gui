package dk.aau.cs.verification.VerifyTAPN;

import dk.aau.cs.Messenger;
import dk.aau.cs.TCTL.TCTLAFNode;
import dk.aau.cs.TCTL.TCTLAGNode;
import dk.aau.cs.TCTL.TCTLEFNode;
import dk.aau.cs.TCTL.TCTLEGNode;
import dk.aau.cs.gui.TabContent;
import dk.aau.cs.io.LoadedModel;
import dk.aau.cs.io.TapnXmlLoader;
import dk.aau.cs.model.CPN.ColorType;
import dk.aau.cs.model.tapn.*;
import dk.aau.cs.model.tapn.simulation.TimedArcPetriNetTrace;
import dk.aau.cs.util.*;
import dk.aau.cs.verification.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import net.tapaal.Preferences;
import net.tapaal.TAPAAL;
import pipe.dataLayer.DataLayer;
import pipe.dataLayer.TAPNQuery.TraceOption;
import pipe.dataLayer.TAPNQuery.WorkflowMode;
import pipe.gui.*;
import pipe.gui.widgets.InclusionPlaces;
import pipe.gui.widgets.InclusionPlaces.InclusionPlacesOption;

import javax.swing.*;

public class VerifyDTAPN implements ModelChecker{

	private static final String NEED_TO_LOCATE_VERIFYDTAPN_MSG = "TAPAAL needs to know the location of the file verifydtapn.\n\n"
			+ "Verifydtapn is a part of the TAPAAL distribution and it is\n"
			+ "normally located in the directory lib.";

    private static String VERIFYDTAPN_VERSION_PATTERN = "^VerifyDTAPN (\\d+\\.\\d+\\.\\d+)$";

    protected static String verifydtapnpath = "";

	protected final FileFinder fileFinder;
	protected final Messenger messenger;

	protected ProcessRunner runner;

	public VerifyDTAPN(FileFinder fileFinder, Messenger messenger) {
		this.fileFinder = fileFinder;
		this.messenger = messenger;
	}

	public boolean supportsStats() {
		return true;
	}

    public String[] getStatsExplanations(){
        String[] explanations = new String[3];
        explanations[0] = "The number of found markings (each time a successor is calculated, this number is incremented)";
        explanations[1] = "The number of markings taken out of the waiting list during the search.";
        explanations[2] = "The number of markings found in the passed/waiting list at the end of verification.";
        return explanations;
    }

	public String getPath() {
		return verifydtapnpath;
	}


    public String getVersion() {
        return EngineHelperFunctions.getVersion(new String[]{verifydtapnpath, "-v"}, VERIFYDTAPN_VERSION_PATTERN);
    }
    public String getVersion(String path) {
        return EngineHelperFunctions.getVersion(new String[]{path, "-v"}, VERIFYDTAPN_VERSION_PATTERN);
    }

    public boolean isCorrectVersion() {
        return isCorrectVersion(getPath());
    }
    public boolean isCorrectVersion(String path) {

        if ((path == null || path.isBlank() || !(new File(path).exists()))) {
            return false;
        }

        File file = new File(path);
        if (!file.canExecute()) {
            messenger.displayErrorMessage("The engine verifydtapn is not executable.\n"
                + "The verifydtapn path will be reset. Please try again, "
                + "to manually set the verifydtapn path.", "Verifydtapn Error");
            return false;
        }

        String version = getVersion(path);

        if (version != null) {
            return EngineHelperFunctions.versionIsEqualOrGreater(version, Pipe.VERIFYDTAPN_MIN_REV);
        } else {
            return false;
        }
    }

	private void resetVerifytapn() {
		verifydtapnpath = null;
		Preferences.getInstance().setVerifydtapnLocation(null);
	}


	public void kill() {
		if (runner != null) {
			runner.kill();
		}
	}

	public void setPath(String path) throws IllegalArgumentException {

        if (isCorrectVersion(path)) {
            verifydtapnpath = path;
            Preferences.getInstance().setVerifydtapnLocation(path);
        } else {
            messenger.displayErrorMessage(
                "The specified version of the file verifydtapn is too old, or not recognized as verifypn", "Verifydtapn Error"
            );
        }
	}

	public boolean setup() {
		if (isNotSetup()) {
			messenger.displayInfoMessage(NEED_TO_LOCATE_VERIFYDTAPN_MSG, "Locate verifydtapn");

			try {
				File file = fileFinder.ShowFileBrowserDialog("Verifydtapn", "", System.getProperty("user.home"));
				if (file != null) {
					if (file.getName().matches("^verifydtapn.*(?:\\.exe)?$")) {
						setPath(file.getAbsolutePath());
					} else {
						messenger.displayErrorMessage("The selected executable does not seem to be verifydtapn.");
					}
				}

			} catch (Exception e) {
				messenger.displayErrorMessage("There were errors performing the requested action:\n" + e.getMessage(), "Error");
			}

		}

		return !isNotSetup();
	}

	private boolean isNotSetup() {
		return verifydtapnpath == null || verifydtapnpath.equals("") || !(new File(verifydtapnpath).exists());
	}

	public static boolean trySetup() {

		String verifydtapn;

		//If env is set, it overwrites the value
		verifydtapn = System.getenv("verifydtapn");
		if (verifydtapn != null && !verifydtapn.isEmpty()) {
			if (new File(verifydtapn).exists()) {
				verifydtapnpath = verifydtapn;
				VerifyDTAPN v = new VerifyDTAPN(new FileFinder(), new MessengerImpl());
				if (v.isCorrectVersion()) {
					return true;
				} else {
					verifydtapnpath = null;
				}
			}
		}

		//If pref is set
		verifydtapn = Preferences.getInstance().getVerifydtapnLocation();
		if (verifydtapn != null && !verifydtapn.isEmpty()) {
			verifydtapnpath = verifydtapn;
			VerifyDTAPN v = new VerifyDTAPN(new FileFinder(), new MessengerImpl());
			if (v.isCorrectVersion()) {
				return true;
			} else {
				verifydtapnpath = null;
			}
		}

		//Search the installdir for verifytapn
		File installdir = TAPAAL.getInstallDir();

		String[] paths = {"/bin/verifydtapn", "/bin/verifydtapn64", "/bin/verifydtapn.exe", "/bin/verifydtapn64.exe"};
		for (String s : paths) {
			File verifydtapnfile = new File(installdir + s);

			if (verifydtapnfile.exists()) {

				verifydtapnpath = verifydtapnfile.getAbsolutePath();
				VerifyDTAPN v = new VerifyDTAPN(new FileFinder(), new MessengerImpl());
				if (v.isCorrectVersion()) {
					return true;
				} else {
					verifydtapnpath = null;
				}

			}
		}
		return false;

	}

	public VerificationResult<TimedArcPetriNetTrace> verify(VerificationOptions options, Tuple<TimedArcPetriNet, NameMapping> model, TAPNQuery query, DataLayer guiModel, pipe.dataLayer.TAPNQuery dataLayerQuery) throws Exception {
		if (!supportsModel(model.value1(), options)) {
			throw new UnsupportedModelException("Verifydtapn does not support the given model.");
		}

		if (!supportsQuery(model.value1(), query, options)) {
			throw new UnsupportedQueryException("Verifydtapn does not support the given query-option combination. ");
		}
		//if(!supportsQuery(model.value1(), query, options))
		//throw new UnsupportedQueryException("Verifydtapn does not support the given query.");

		//if(((VerifyTAPNOptions)options).discreteInclusion() && !isQueryUpwardClosed(query))
		//throw new UnsupportedQueryException("Discrete inclusion check only supports upward closed queries.");

		if (((VerifyTAPNOptions) options).discreteInclusion()) mapDiscreteInclusionPlacesToNewNames(options, model);
		if (CreateGui.getCurrentTab().getLens().isGame() && !CreateGui.getCurrentTab().getLens().isTimed()) {
		    addGhostPlace(model.value1());
        }

		VerifyTAPNExporter exporter = new VerifyTAPNExporter();

		ExportedVerifyTAPNModel exportedModel = exporter.export(model.value1(), query, CreateGui.getCurrentTab().getLens(),model.value2(), guiModel, dataLayerQuery);

		if (exportedModel == null) {
			messenger.displayErrorMessage("There was an error exporting the model");
		}

		return verify(options, model, exportedModel, query, dataLayerQuery);
	}

    //An extra place is added before verifying the query so the timed engine is able to mimic the untimed game semantics.
	protected void addGhostPlace(TimedArcPetriNet net) {
	    TimedPlace place = new LocalTimedPlace("ghost", new TimeInvariant(true, new IntBound(0)), ColorType.COLORTYPE_DOT);
	    net.add(place);
        place.addToken(new TimedToken(place, new BigDecimal(0), ColorType.COLORTYPE_DOT.getFirstColor()));
    }

	protected void mapDiscreteInclusionPlacesToNewNames(VerificationOptions options, Tuple<TimedArcPetriNet, NameMapping> model) {
		VerifyTAPNOptions verificationOptions = (VerifyTAPNOptions) options;

		if (verificationOptions.inclusionPlaces().inclusionOption() == InclusionPlacesOption.AllPlaces)
			return;

		List<TimedPlace> inclusionPlaces = new ArrayList<TimedPlace>();
		for (TimedPlace p : verificationOptions.inclusionPlaces().inclusionPlaces()) {
			if (p instanceof LocalTimedPlace) {
				LocalTimedPlace local = (LocalTimedPlace) p;
				if (local.model().isActive()) {
					inclusionPlaces.add(model.value1().getPlaceByName(model.value2().map(local.model().name(), local.name())));
				}
			} else { // shared place
				inclusionPlaces.add(model.value1().getPlaceByName(model.value2().map("", p.name())));
			}
		}

		((VerifyTAPNOptions) options).setInclusionPlaces(new InclusionPlaces(InclusionPlacesOption.UserSpecified, inclusionPlaces));
	}

	protected VerificationResult<TimedArcPetriNetTrace> verify(VerificationOptions options, Tuple<TimedArcPetriNet, NameMapping> model, ExportedVerifyTAPNModel exportedModel, TAPNQuery query, pipe.dataLayer.TAPNQuery dataLayerQuery) {
		((VerifyTAPNOptions) options).setTokensInModel(model.value1().marking().size()); // TODO: get rid of me

        runner = new ProcessRunner(verifydtapnpath, createArgumentString(exportedModel.modelFile(), exportedModel.queryFile(), options));
		runner.run();

		if (runner.error()) {
			return null;
		} else {
			String errorOutput = readOutput(runner.errorOutput());
			String standardOutput = readOutput(runner.standardOutput());

			Tuple<QueryResult, Stats> queryResult = parseQueryResult(standardOutput, model.value1().marking().size() + query.getExtraTokens(), query.getExtraTokens(), query, model.value1());

			if (queryResult == null || queryResult.value1() == null) {
				return new VerificationResult<TimedArcPetriNetTrace>(errorOutput + System.getProperty("line.separator") + standardOutput, runner.getRunningTime());
			} else {

                TimedArcPetriNetTrace tapnTrace = null;

                if(options.traceOption() != TraceOption.NONE && model.value1().isColored() && queryResult != null && queryResult.value1() != null && queryResult.value1().isQuerySatisfied()) {
                    TapnXmlLoader tapnLoader = new TapnXmlLoader();
                    File fileOut = new File(options.unfoldedModelPath());
                    File queriesOut = new File(options.unfoldedQueriesPath());
                    TabContent newTab;
                    LoadedModel loadedModel = null;
                    try {
                        loadedModel = tapnLoader.load(fileOut);
                        TAPNComposer newComposer = new TAPNComposer(new MessengerImpl(), true);
                        model = newComposer.transformModel(loadedModel.network());

                        if (queryResult != null && queryResult.value1() != null) {
                            tapnTrace = parseTrace(!errorOutput.contains("Trace:") ? errorOutput : (errorOutput.split("Trace:")[1]), options, model, exportedModel, query, queryResult.value1());
                        }

                        if(tapnTrace == null){
                            String message = "No trace could be generated.\n\n";
                            message += "Model checker output:\n" + standardOutput;
                            messenger.displayWrappedErrorMessage(message,"No trace found");
                        } else {
                            int dialogResult = JOptionPane.showConfirmDialog(null, "There is a trace that will be displayed in a new tab on the unfolded net/query.", "Open trace", JOptionPane.OK_CANCEL_OPTION);
                            if (dialogResult == JOptionPane.OK_OPTION) {
                                newTab = new TabContent(loadedModel.network(), loadedModel.templates(), loadedModel.queries(), new TabContent.TAPNLens(CreateGui.getCurrentTab().getLens().isTimed(), CreateGui.getCurrentTab().getLens().isGame(), false));

                                //The query being verified should be the only query
                                for (pipe.dataLayer.TAPNQuery loadedQuery : UnfoldNet.getQueries(queriesOut, loadedModel.network())) {
                                    newTab.setInitialName(loadedQuery.getName() + " - unfolded");
                                    loadedQuery.copyOptions(dataLayerQuery);
                                    newTab.addQuery(loadedQuery);
                                }

                                CreateGui.openNewTabFromStream(newTab);
                            } else {
                                options.setTraceOption(TraceOption.NONE);
                            }
                        }
                    } catch (FormatException e) {
                        e.printStackTrace();
                        return null;
                    } catch (ThreadDeath d) {
                        d.printStackTrace();
                        return null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                if (tapnTrace == null) {
                    tapnTrace = parseTrace(!errorOutput.contains("Trace:") ? errorOutput : (errorOutput.split("Trace:")[1]), options, model, exportedModel, query, queryResult.value1());
                }

				// Parse covered trace
				TimedArcPetriNetTrace secondaryTrace = null;
				if (queryResult.value2().getCoveredMarking() != null) {
					secondaryTrace = parseTrace((errorOutput.split("Trace:")[2]), options, model, exportedModel, query, queryResult.value1());
				}

				return new VerificationResult<TimedArcPetriNetTrace>(queryResult.value1(), tapnTrace, secondaryTrace, runner.getRunningTime(), queryResult.value2(), false, standardOutput, model);
			}
		}
	}

	private TimedArcPetriNetTrace parseTrace(String output, VerificationOptions options, Tuple<TimedArcPetriNet, NameMapping> model, ExportedVerifyTAPNModel exportedModel, TAPNQuery query, QueryResult queryResult) {
		if (((VerifyTAPNOptions) options).trace() == TraceOption.NONE) return null;

		VerifyTAPNTraceParser traceParser = new VerifyTAPNTraceParser(model.value1());
		TimedArcPetriNetTrace trace = traceParser.parseTrace(new BufferedReader(new StringReader(output)));

		if (trace == null) {
			if (((VerifyTAPNOptions) options).trace() != TraceOption.NONE) {
				if ((query.getProperty() instanceof TCTLEFNode && !queryResult.isQuerySatisfied()) ||
						(query.getProperty() instanceof TCTLAGNode && queryResult.isQuerySatisfied()) ||
						(query.getProperty() instanceof TCTLEGNode && !queryResult.isQuerySatisfied()) ||
						(query.getProperty() instanceof TCTLAFNode && queryResult.isQuerySatisfied())) {
					return null;
				} else {
					messenger.displayErrorMessage("Verifydtapn cannot generate the requested trace for the model. Try another trace option.");
				}
			}
		}
		return trace;
	}

	private String createArgumentString(String modelFile, String queryFile, VerificationOptions options) {
		StringBuilder buffer = new StringBuilder(options.toString());
		buffer.append(' ');
		buffer.append(modelFile);
		VerifyDTAPNOptions opts = (VerifyDTAPNOptions) options;
		if (opts.getWorkflowMode() == WorkflowMode.NOT_WORKFLOW || opts.getWorkflowMode() == null) {
			buffer.append(' ');
			buffer.append(queryFile);
		}
		return buffer.toString();
	}

	private String readOutput(BufferedReader reader) {
		try {
			if (!reader.ready())
				return "";
		} catch (IOException e1) {
			return "";
		}
		StringBuilder buffer = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
				buffer.append(System.getProperty("line.separator"));
			}
		} catch (IOException e) {
		}

		return buffer.toString();
	}

	private Tuple<QueryResult, Stats> parseQueryResult(String output, int totalTokens, int extraTokens, TAPNQuery query, TimedArcPetriNet model) {
		VerifyDTAPNOutputParser outputParser = new VerifyDTAPNOutputParser(totalTokens, extraTokens, query);
		return outputParser.parseOutput(output);
	}


	public boolean supportsModel(TimedArcPetriNet model, VerificationOptions options) {
		if (model.hasUrgentTransitions() && ((VerifyDTAPNOptions) options).timeDarts()) {
			return false;
		}

		return model.isNonStrict();
	}

	public boolean supportsQuery(TimedArcPetriNet model, TAPNQuery query, VerificationOptions options) {
		// if liveness, has deadlock proposition and uses timedarts, it is not supported
		if ((query.getProperty() instanceof TCTLEGNode || query.getProperty() instanceof TCTLAFNode)
				&& query.hasDeadlock()
				&& ((VerifyDTAPNOptions) options).timeDarts()) {
			return false;
		}

		return true;
	}

	public static void reset() {
		//Clear value
		verifydtapnpath = "";
		Preferences.getInstance().setVerifydtapnLocation(null);
		//Set the detault
		trySetup();
	}

	@Override
	public String toString() {
		return "verifydtapn";
	}

	public boolean useDiscreteSemantics() {
		return true;
	}
}