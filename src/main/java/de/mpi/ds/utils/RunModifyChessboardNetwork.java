/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package de.mpi.ds.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

/**
 * @author tthunig
 */
public class RunModifyChessboardNetwork {

	// capacity at the links that all agents have to use
	private static final long CAP_FIRST_LAST = 10; // [veh/h]
	// capacity at all other links
	private static final long CAP_MAIN = 10; // [veh/h]

	// link length for all other links
	private static final long LINK_LENGTH = 1000; // [m]

	public static void main(String[] args) throws IOException {

		// create an empty network
		Network net = NetworkUtils.readNetwork("/home/helge/Programs/matsim/matsim_ownBuild/scenarios/bimodal_try/network_template.xml");
		NetworkFactory fac = net.getFactory();

		int i = 0;
		for (Link l : net.getLinks().values()) {
		    System.out.println(l.getId());
			if ((i >= 10 && i <= 45) || (i >= 100 && i <= 135)) {
				setLinkAttributes(l, "car,train");
			} else {
				setLinkAttributes(l, "car");
			}
			i++;
		}


		// create output folder if necessary
		Path outputFolder = Files.createDirectories(Paths.get("output"));

		// write network
		new NetworkWriter(net).write(outputFolder.resolve("network.xml").toString());
	}

	
	private static void setLinkAttributes(Link link, String modes) {
		HashSet<String> hash_Set = new HashSet<String>();
		hash_Set.add(modes);
		link.setAllowedModes(hash_Set);
	}

}
