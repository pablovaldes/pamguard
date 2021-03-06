/*	PAMGUARD - Passive Acoustic Monitoring GUARDianship.
 * To assist in the Detection Classification and Localisation 
 * of marine mammals (cetaceans).
 *  
 * Copyright (C) 2006 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package userDisplay;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JToolBar;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import radardisplay.RadarDisplay;
import radardisplay.RadarParameters;


import Layout.PamInternalFrame;
import PamController.PamController;
import PamController.PamguardVersionInfo;
import PamView.PamPanelTiler;
import PamView.PamTabPanel;
import Spectrogram.SpectrogramDisplay;
import Spectrogram.SpectrogramParameters;

public class UserDisplayTabPanelControl implements PamTabPanel {

	private UserDisplayTabPanel userDisplayTabPanel;

	protected PamPanelTiler panelTiler;

	private UserDisplayControl userDisplayControl;

	UserDisplayTabPanelControl(UserDisplayControl userDisplayControl) {
		userDisplayTabPanel = new UserDisplayTabPanel();
		this.userDisplayControl = userDisplayControl;
		panelTiler = new PamPanelTiler(userDisplayTabPanel);
	}

	void addSpectrogram(SpectrogramParameters spectrogramParameters) {
		// tabPanelControl.AddSpectrogram();
		PamInternalFrame frame;
		SpectrogramDisplay spectrogramDisplay;
		userDisplayTabPanel.add(frame = new PamInternalFrame(
				spectrogramDisplay = new SpectrogramDisplay(userDisplayControl,
						spectrogramParameters), true));
		userDisplayTabPanel.setPosition(frame, 0);
		//spectrogramDisplay.SetParams(spectrogramParameters);
		if (spectrogramParameters.boundingRectangle != null &&
				spectrogramParameters.boundingRectangle.width > 0) {
			frame.setBounds(spectrogramParameters.boundingRectangle);
		}
		frame.addInternalFrameListener(spectrogramDisplay);
		if (PamguardVersionInfo.getReleaseType() == PamguardVersionInfo.ReleaseType.SMRU) {
			frame.addComponentListener(new IntComponentListener(frame));
		}
	}
	void addRadar(RadarParameters radarParameters) {
		// tabPanelControl.AddSpectrogram();
		PamInternalFrame frame;
		RadarDisplay radarDisplay;
		userDisplayTabPanel.add(frame = new PamInternalFrame(
				radarDisplay = new RadarDisplay(userDisplayControl,
						radarParameters), true));
		userDisplayTabPanel.setPosition(frame, 0);
		//spectrogramDisplay.SetParams(spectrogramParameters);
		if (radarParameters.boundingRectangle != null &&
				radarParameters.boundingRectangle.width > 0) {
			frame.setBounds(radarParameters.boundingRectangle);
		}
		frame.addInternalFrameListener(radarDisplay);
		radarDisplay.newSettings();
	}

	/**
	 * Ad a display generated by a different module
	 * @param userDisplayProvider
	 * @param dpParams 
	 */
	public void addUserDisplay(UserDisplayProvider userDisplayProvider, DisplayProviderParameters dpParams) {
		System.out.println("Adding display " + userDisplayProvider.getName());
		JInternalFrame frame = new UserDisplayFrame(userDisplayProvider);
		userDisplayTabPanel.add(frame);
		userDisplayTabPanel.setPosition(frame, 0);
		if (dpParams != null) {
			frame.setLocation(dpParams.location);
			frame.setSize(dpParams.size);
		}
	}

	public Serializable getSettingsReference() {

		ArrayList<SpectrogramParameters> spectrogramsList = new ArrayList<SpectrogramParameters>();
		ArrayList<RadarParameters> radarList = new ArrayList<RadarParameters>();
		ArrayList<DisplayProviderParameters> providerList = new ArrayList<DisplayProviderParameters>();

		UserFramePlots userDisplay;
		UserDisplayFrame userFrame;

		JInternalFrame[] allFrames = userDisplayTabPanel.getAllFrames();
		// reverse order of this loop so frames get reconstructed in correct order.
		for (int i = allFrames.length-1; i >= 0; i--) {
			if (PamInternalFrame.class.isAssignableFrom(allFrames[i].getClass()) == false) {
				userFrame = (UserDisplayFrame) allFrames[i];
				UserDisplayProvider udp = userFrame.getUserDisplayProvider();
				DisplayProviderParameters dp = new DisplayProviderParameters(udp.getComponentClass(), udp.getName());
				dp.size = userFrame.getSize();
				dp.location = userFrame.getLocation();
				providerList.add(dp);
			}
			else {
				userDisplay = (UserFramePlots) ((PamInternalFrame) allFrames[i]).getFramePlots();
				if (userDisplay.getFrameType() == UserFramePlots.FRAME_TYPE_SPECTROGRAM) {

					spectrogramsList
					.add(((SpectrogramDisplay) ((PamInternalFrame) allFrames[i])
							.getFramePlots()).getSpectrogramParameters());
				}
				else if (userDisplay.getFrameType() == UserFramePlots.FRAME_TYPE_RADAR) {
					radarList
					.add(((RadarDisplay) ((PamInternalFrame) allFrames[i])
							.getFramePlots()).getRadarParameters());
				}
			}
		}

		UserDisplayParameters udp = new UserDisplayParameters();
		udp.spectrogramParameters = spectrogramsList;
		udp.radarParameters = radarList;
		udp.displayProviderParameters = providerList;

		return udp;
	}

	public void notifyModelChanged(int changeType) {

		JInternalFrame[] allFrames = userDisplayTabPanel.getAllFrames();
		for (int i = 0; i < allFrames.length; i++) {
			if (PamInternalFrame.class.isAssignableFrom(allFrames[i].getClass())) {
				((UserFramePlots) ((PamInternalFrame) allFrames[i])
						.getFramePlots()).notifyModelChanged(changeType);
			}
		}
	}

	public JMenu createMenu(Frame parentFrame) {
		return null;
	}

	/**
	 * @return Reference to a graphics component that can be added to the view.
	 *         This will typically be a JPanel or a JInternalFrame;
	 */
	public JComponent getPanel() {
		return userDisplayTabPanel;
	}

	public JToolBar getToolBar() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean canPlayViewerSound() {
		SpectrogramDisplay sd = findFirstSpectrogram();
		return (sd != null);
	}

	public void playViewerSound() {
		SpectrogramDisplay sd = findFirstSpectrogram();
		if (sd != null) {
			sd.playViewerSound();
		}
	}

	public SpectrogramDisplay findFirstSpectrogram() {
		JInternalFrame[] allFrames = userDisplayTabPanel.getAllFrames();
		UserFramePlots userDisplay;
		for (int i = 0; i < allFrames.length; i++) {
			userDisplay = (UserFramePlots) ((PamInternalFrame) allFrames[i]).getFramePlots();
			if (userDisplay.getFrameType() == UserFramePlots.FRAME_TYPE_SPECTROGRAM) {
				return (SpectrogramDisplay) userDisplay;
			}
		}
		return null;
	}
	
	class IntComponentListener implements ComponentListener {

		private PamInternalFrame frame;
		public IntComponentListener(PamInternalFrame frame) {
			this.frame = frame;
		}
		@Override
		public void componentHidden(ComponentEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void componentMoved(ComponentEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public void componentResized(ComponentEvent arg0) {
			Dimension d = frame.getSize();
//			String str = String.format("%s resized to %dx%d", frame.getTitle(),
//					d.width, d.height);
//			System.out.println(str);
		}
		@Override
		public void componentShown(ComponentEvent arg0) {
			// TODO Auto-generated method stub
			
		}

	}
}
