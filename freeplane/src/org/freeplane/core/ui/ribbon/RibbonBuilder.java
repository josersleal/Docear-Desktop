package org.freeplane.core.ui.ribbon;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.IndexedTree;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.ui.ribbon.special.FontStyleContributorFactory;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.ModeController;
import org.pushingpixels.flamingo.api.ribbon.JRibbon;
import org.pushingpixels.flamingo.api.ribbon.RibbonApplicationMenu;


public class RibbonBuilder {
	private final HashMap<String, IRibbonContributorFactory> contributorFactories = new HashMap<String, IRibbonContributorFactory>();
	
	private final IndexedTree structure;
	private final RootContributor rootContributor;
	private final RibbonStructureReader reader;
	private final JRibbon ribbon;
	
	public RibbonBuilder(ModeController mode, JRibbon ribbon) {
		final RibbonApplicationMenu applicationMenu = new RibbonApplicationMenu();
		structure = new IndexedTree(applicationMenu);
		this.rootContributor = new RootContributor(ribbon);
		this.ribbon = ribbon;
		reader = new RibbonStructureReader(this);
		registerContributorFactory("ribbon_task", new RibbonTaskContributorFactory());
		registerContributorFactory("ribbon_band", new RibbonBandContributorFactory());
		registerContributorFactory("ribbon_action", new RibbonActionContributorFactory());
		registerContributorFactory("font_style", new FontStyleContributorFactory());
	}
	
	public void add(IRibbonContributor contributor, RibbonPath path, int position) {
		if(contributor == null || path == null) {
			throw new IllegalArgumentException("NULL");
		}
		synchronized (structure) {
			RibbonPath elementPath = new RibbonPath(path);
			elementPath.setName(contributor.getKey());
			if("/ribbon".equals(path.getKey())) {				
				structure.addElement(structure, contributor, elementPath.getKey(), position);
			}
			else {
				structure.addElement(path.getKey(), contributor, elementPath.getKey(), position);
			}
		}
	}
	
	public void registerContributorFactory(String key, IRibbonContributorFactory factory) {
		synchronized (contributorFactories) {
			this.contributorFactories.put(key, factory);
		}

	}
	
	public IRibbonContributorFactory getContributorFactory(String key) {
		return this.contributorFactories.get(key);
	}
	
	public void buildRibbon() {
		
		synchronized (structure) {
			rootContributor.contribute(structure, null);			
		}
		Window f = SwingUtilities.getWindowAncestor(ribbon);
		Dimension rv = f.getSize();
		f.applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
		Rectangle r = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		f.setPreferredSize(new Dimension(r.width, r.height / 2));
		f.setMinimumSize(new Dimension(640,240));
		f.pack();
		f.getSize(rv);
	}
	
	public void updateRibbon(String xmlResource) {
		final URL xmlSource = ResourceController.getResourceController().getResource(xmlResource);
		if (xmlSource != null) {
			final boolean isUserDefined = xmlSource.getProtocol().equalsIgnoreCase("file");
			try{
			reader.loadStructure(xmlSource);
			}
			catch (RuntimeException e){
				if(isUserDefined){
					LogUtils.warn(e);
					String myMessage = TextUtils.format("ribbon_error", xmlSource.getPath(), e.getMessage());
					UITools.backOtherWindows();
					JOptionPane.showMessageDialog(UITools.getFrame(), myMessage, "Freeplane", JOptionPane.ERROR_MESSAGE);
					System.exit(-1);
				}
				throw e;
			}
		}
	}

	public boolean containsKey(String key) {
		synchronized (structure) {
			return structure.contains(key);
		}		
	}

	public static class RibbonPath {
		public static RibbonPath emptyPath() {
			final RibbonPath menuPath = new RibbonPath(null);
			return menuPath;
		}

		private final RibbonPath parent;
		private String key = "";
		
		public RibbonPath(final RibbonPath parent) {
			this.parent = parent;
		}

		public void setName(final String name) {
			key = name;
		}
		
		public RibbonPath getParent() {
			return parent;
		}
		
		public String getKey() {
			return ((parent != null) ? parent.getKey() + "/" : "") + key;
		}

		@Override
		public String toString() {
			return getKey();
		}
	}

}
