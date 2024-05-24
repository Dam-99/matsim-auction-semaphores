/**
 * 
 */
package org.matsim.contrib.smartcity.analisys;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.TravelDistanceStats;
import org.matsim.core.controler.AbstractModule;

/**
 * @author Filippo Muzzini
 *
 */
public class AnalisysModule extends AbstractModule {

	/* (non-Javadoc)
	 * @see org.matsim.core.controler.AbstractModule#install()
	 */
	@Override
	public void install() {
		bind(CalcLegTimesWithoutWalk.class).asEagerSingleton();
		bind(TravelDistance.class).asEagerSingleton();
		bind(CalcSemaphoreCrossingTime.class).asEagerSingleton();

		bind(CalcLegTimes.class).to(CalcLegTimesWithoutWalk.class);
		bind(TravelDistanceStats.class).to(TravelDistance.class);

		addEventHandlerBinding().to(BidAnalyzer.class);
		addControlerListenerBinding().to(BidAnalyzer.class);
		bind(BidAnalyzer.class).asEagerSingleton();

		addEventHandlerBinding().to(SemaphorePredictionAnalyzer.class);
		addControlerListenerBinding().to(SemaphorePredictionAnalyzer.class);
		bind(SemaphorePredictionAnalyzer.class).asEagerSingleton();

		addEventHandlerBinding().to(CalcSemaphoreCrossingTime.class);
		addControlerListenerBinding().to(CalcSemaphoreCrossingTime.class);
	}

}
