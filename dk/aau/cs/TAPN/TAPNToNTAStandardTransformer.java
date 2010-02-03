package dk.aau.cs.TAPN;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.aau.cs.TA.Edge;
import dk.aau.cs.TA.Location;
import dk.aau.cs.TA.StandardUPPAALQuery;
import dk.aau.cs.TA.TimedAutomaton;
import dk.aau.cs.TA.UPPAALQuery;
import dk.aau.cs.petrinet.Arc;
import dk.aau.cs.petrinet.TAPNArc;
import dk.aau.cs.petrinet.TAPNPlace;
import dk.aau.cs.petrinet.TAPNQuery;
import dk.aau.cs.petrinet.TAPNTransition;
import dk.aau.cs.petrinet.TAPNTransportArc;
import dk.aau.cs.petrinet.TimedArcPetriNet;
import dk.aau.cs.petrinet.Token;

public class TAPNToNTAStandardTransformer 
extends TAPNToNTATransformer{

	private static final String TANAME = "Token";
	private static final String PLOCK = "P_lock";
	private int taCount = 0;

	public TAPNToNTAStandardTransformer(int extraNumberOfTokens){
		super(extraNumberOfTokens);
	}

	@Override
	protected List<TimedAutomaton> createAutomata(TimedArcPetriNet model) {
		ArrayList<TimedAutomaton> tas = new ArrayList<TimedAutomaton>();

		List<Token> tokens = model.getTokens();
		for(Token token : tokens){
			clearLocationMappings();
			
			TimedAutomaton ta = createTimedAutomata(model);
			ta.setInitLocation(getLocationByName(token.getPlace().getName()));

			tas.add(ta);
		}
		return tas;
	}

	private TimedAutomaton createTimedAutomata(TimedArcPetriNet model) {
		TimedAutomaton ta = new TimedAutomaton();

		createLocations(model, ta);
		createTransitions(model, ta);
		ta.setDeclarations("clock x;");
		ta.setName(TANAME + taCount);
		taCount++;
		
		return ta;
	}


	private void createTransitions(TimedArcPetriNet model, TimedAutomaton ta) {
		for(TAPNTransition transition : model.getTransitions()){
			char symbol = '!';
			Arc usedPostSetArc = null;

			for(Arc presetArc : transition.getPreset()){ // at most two
				for(Arc postsetArc : transition.getPostset()){
					if(presetArc instanceof TAPNTransportArc){
						if(postsetArc instanceof TAPNTransportArc && postsetArc != usedPostSetArc){
							Edge e = createEdge(transition, (TAPNArc)presetArc, postsetArc, symbol);
							ta.addTransition(e);
							usedPostSetArc = postsetArc;
							symbol = '?';
							break;
						}
					}else{
						if(!(postsetArc instanceof TAPNTransportArc) && postsetArc != usedPostSetArc){
							Edge e = createEdge(transition, (TAPNArc)presetArc, postsetArc, symbol);
							ta.addTransition(e);
							usedPostSetArc = postsetArc;
							symbol = '?'; // Makes next edge a ? edge
							break;
						}
					}
				}
			}
		}
	}


	private Edge createEdge(TAPNTransition transition, TAPNArc sourceArc, Arc destinationArc, char symbol) {
		Location source = getLocationByName(sourceArc.getSource().getName());
		Location destination = getLocationByName(destinationArc.getTarget().getName());

		String guard = createTransitionGuard(sourceArc.getGuard(), transition.isFromOriginalNet());
		String sync = createSyncExpression(transition, symbol);
		String update = createUpdateExpression(sourceArc);
		if(source.getName().equals(PLOCK)){
			if(update.length()>0){
				update = "lock = 1, " + update;
			}else{
				update = "lock = 1";
			}
			
		}else if(destination.getName().equals(PLOCK)){
			if(update.length()>0){
				update = "lock = 0, " + update;
			}else{
				update = "lock = 0";
			}
		}
		Edge e = new Edge(source, destination, guard, sync, update);
		return e;
	}
	

	private void createLocations(TimedArcPetriNet model, TimedAutomaton ta) {
		for(TAPNPlace p : model.getPlaces()){
			Location l = new Location(p.getName(), convertInvariant(p.getInvariant()));
			l.setUrgent(p.isUrgent());

			ta.addLocation(l);
			addLocationMapping(p.getName(), l);
		}
	}


	
	public UPPAALQuery transformQuery(TAPNQuery tapnQuery) throws Exception {
		String query = tapnQuery.toString();

		Pattern pattern = Pattern.compile(QUERY_PATTERN);
		Matcher matcher = pattern.matcher(query);

		StringBuilder builder = new StringBuilder("(");
		StringBuilder lock = new StringBuilder("(");
		for(int i = 0; i < tapnQuery.getTotalTokens(); i++){
			if(i > 0){
				builder.append(" + ");
				lock.append(" + ");
			}
			builder.append(TANAME);
			builder.append(i);
			builder.append(".$1");
			
			lock.append(TANAME);
			lock.append(i);
			lock.append(".");
			lock.append(PLOCK);
		}
		builder.append(") $2 $3");
		lock.append(") == 1");
		String transformedQuery = matcher.replaceAll(builder.toString());
	
		transformedQuery = transformedQuery.substring(0, transformedQuery.length()-2) + " && " + lock.toString() + "))";
		return new StandardUPPAALQuery(transformedQuery);
	}
}