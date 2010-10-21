package dk.aau.cs.verification.UPPAAL;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import dk.aau.cs.TA.trace.Participant;
import dk.aau.cs.TA.trace.TAFiringAction;
import dk.aau.cs.TA.trace.TimeDelayFiringAction;
import dk.aau.cs.TA.trace.ConcreteTransitionFiring;
import dk.aau.cs.TA.trace.UppaalTrace;
import dk.aau.cs.petrinet.TAPNPlace;
import dk.aau.cs.petrinet.TAPNTransition;
import dk.aau.cs.petrinet.TimedArcPetriNet;
import dk.aau.cs.petrinet.Token;
import dk.aau.cs.petrinet.trace.TAPNFiringAction;
import dk.aau.cs.petrinet.trace.TAPNTrace;
import dk.aau.cs.petrinet.trace.TAPNTrace;
import dk.aau.cs.translations.TranslationNamingScheme;
import dk.aau.cs.translations.TranslationNamingScheme.TransitionTranslation;
import dk.aau.cs.translations.TranslationNamingScheme.TransitionTranslation.SequenceInfo;

public class VerifytaTraceInterpreter {
	private final TimedArcPetriNet tapn;
	private final TranslationNamingScheme namingScheme;

	public VerifytaTraceInterpreter(TimedArcPetriNet tapn, TranslationNamingScheme namingScheme){
		this.tapn = tapn;
		this.namingScheme = namingScheme;
	}

	protected TimedArcPetriNet tapn(){
		return tapn;
	}

	protected TranslationNamingScheme namingScheme(){
		return namingScheme;
	}

	public TAPNTrace interpretTrace(UppaalTrace trace){
		return trace.isConcreteTrace() ? interpretTimedTrace(trace) : interpretUntimedTrace(trace);
	}

	private TAPNTrace interpretUntimedTrace(UppaalTrace trace) {
		// TODO Auto-generated method stub
		return null;
	}

	private TAPNTrace interpretTimedTrace(UppaalTrace trace) {
		TAPNTrace result = new TAPNTrace();

		Iterator<TAFiringAction> iterator = trace.iterator();
		TAFiringAction action = null;

		while(iterator.hasNext()){
			List<ConcreteTransitionFiring> firingSequence = new ArrayList<ConcreteTransitionFiring>();
			List<String> firingSequenceNames = new ArrayList<String>();

			while(iterator.hasNext() && 
					(action = iterator.next()) instanceof ConcreteTransitionFiring){
				firingSequence.add((ConcreteTransitionFiring)action);
				firingSequenceNames.add(((ConcreteTransitionFiring)action).channel());
			}

			TransitionTranslation[] transitions = namingScheme.interpretTransitionSequence(firingSequenceNames);

			for(TransitionTranslation transitionTranslation : transitions){
				TAPNFiringAction firingAction = interpretTransitionFiring(firingSequence, transitionTranslation);
				result.addFiringAction(firingAction);
			}


			if(action != null && action instanceof TimeDelayFiringAction){
				BigDecimal delay = ((TimeDelayFiringAction)action).getDelay();
				TAPNFiringAction delayAction = new dk.aau.cs.petrinet.trace.TimeDelayFiringAction(delay);
				result.addFiringAction(delayAction);
			}
		}

		return result;
	}

	protected TAPNFiringAction interpretTransitionFiring
	(
			List<ConcreteTransitionFiring> firingSequence,
			TransitionTranslation transitionTranslation
	) {
		TAPNTransition transition = tapn.getTransitionsByName(transitionTranslation.originalTransitionName());
		List<Token> tokens = null;
		if(transitionTranslation.sequenceInfo().equals(SequenceInfo.WHOLE)){
			tokens = parseConsumedTokens(
					firingSequence.subList(transitionTranslation.startsAt(), transitionTranslation.endsAt()+1)
			);
		}else if(transitionTranslation.sequenceInfo().equals(SequenceInfo.END)){
			ConcreteTransitionFiring start = firingSequence.get(transitionTranslation.startsAt());
			ConcreteTransitionFiring end = firingSequence.get(transitionTranslation.endsAt());
			tokens = parseConsumedTokens(start, end);
		}

		return new dk.aau.cs.petrinet.trace.TransitionFiringAction(transition, tokens);
	}

	private List<Token> parseConsumedTokens(List<ConcreteTransitionFiring> actions) {
		ArrayList<Token> tokens = new ArrayList<Token>();

		for(int i = 0; i < actions.size(); i++){
			ConcreteTransitionFiring action = actions.get(i);

			for(Participant participant : action.participants()){
				String automata = participant.automata();
				String sourceLocation = participant.location();

				if(!namingScheme.isIgnoredAutomata(automata) && !namingScheme.isIgnoredPlace(sourceLocation)){
					TAPNPlace place = tapn.getPlaceByName(sourceLocation);
					BigDecimal clockValue = participant.clockOrVariableValue(namingScheme().tokenClockName()).lower();
					Token token = new Token(place, clockValue); 
					tokens.add(token);
				}
			}
		}

		return tokens;
	}

		private List<Token> parseConsumedTokens(ConcreteTransitionFiring start, ConcreteTransitionFiring end) {
			ArrayList<Token> tokens = new ArrayList<Token>();
	
			for(Participant participant : end.participants()){
				String automata = participant.automata();
				String sourceLocation = start.sourceState().locationFor(automata);
				
				if(!namingScheme.isIgnoredAutomata(automata) && !namingScheme.isIgnoredPlace(sourceLocation)){
					TAPNPlace place = tapn.getPlaceByName(sourceLocation);
					BigDecimal clockValue = start.sourceState().getLocalClockOrVariable(automata, namingScheme().tokenClockName()).lower();
					Token token = new Token(place, clockValue); 
					tokens.add(token);
				}
			}
	
			return tokens;
		}
}
