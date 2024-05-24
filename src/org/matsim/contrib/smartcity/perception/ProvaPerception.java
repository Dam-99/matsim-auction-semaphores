package org.matsim.contrib.smartcity.perception;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.smartcity.perception.wrapper.ActivePerceptionWrapper;
import org.matsim.contrib.smartcity.perception.wrapper.LinkChangedListener;
import org.matsim.contrib.smartcity.perception.wrapper.LinkTrafficStatus;

/**
 * Simple class to test the wrapper.
 * Write to file all change in the roads netwrok
 * 
 * @author Filippo Muzzini
 * 
 *
 */
public class ProvaPerception implements LinkChangedListener {
	
	private final String file = "prove"+File.separator+"output_prova"+File.separator+"file_prova";
	private ActivePerceptionWrapper wrapper;
	private PrintWriter stream;
	
	public ProvaPerception(ActivePerceptionWrapper wrapper) {
		//create output file
		createFile();
		
		//bind the wrapper
		this.wrapper = wrapper;
		this.wrapper.addLinkChangedListener(this);
	}

	private void createFile() {
		try {
			File f = new File(file);
			f.getParentFile().mkdirs();
			f.createNewFile();
			this.stream = new PrintWriter(f);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void publishLinkChanged(Id<Link> idLink, LinkTrafficStatus status, double time) {
		int linkStatus = wrapper.getTotalVehicleOnLink(idLink);
		String str = "Link: "+idLink+" n: "+linkStatus;
		this.stream.println(str);
		this.stream.flush();
	}
	
}
