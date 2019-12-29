package pipe.gui;

import java.util.ArrayList;
import com.sun.jna.Platform;
import net.tapaal.TAPAAL;
import pipe.dataLayer.DataLayer;
import pipe.dataLayer.NetType;
import pipe.gui.canvas.DrawingSurfaceImpl;
import pipe.gui.handler.SpecialMacHandler;
import dk.aau.cs.gui.TabContent;


public class CreateGui {

	private static GuiFrame appGui;
	private static GuiFrameController appGuiController;

	private static ArrayList<TabContent> tabs = new ArrayList<TabContent>();
	
	public static final String imgPath = "resources/Images/";

	public static void init() {
		appGui = new GuiFrame(TAPAAL.getProgramName());
        appGuiController = new GuiFrameController(appGui);

		if (Platform.isMac()){
			try {
				SpecialMacHandler.postprocess();
			} catch (NoClassDefFoundError e) {
				//Failed loading special mac handler, ignore and run program without MacOS integration
			}
		}

		appGui.setVisible(true);
		appGuiController.checkForUpdate(false);
	}

	@Deprecated
	public static DataLayer getModel() {
		return getModel(appGui.getSelectedTabIndex());
	}

	@Deprecated
	public static DataLayer getModel(int index) {
		if (index < 0) {
			return null;
		}

		TabContent tab = (tabs.get(index));
		return tab.getModel();
	}

	@Deprecated
	public static DrawingSurfaceImpl getDrawingSurface() {
		return getDrawingSurface(appGui.getSelectedTabIndex());
	}

	@Deprecated
	public static DrawingSurfaceImpl getDrawingSurface(int index) {

		if (index < 0) {
			return null;
		}

		TabContent tab = (tabs.get(index));

		return tab.drawingSurface();
	}

	@Deprecated
	public static void addTab (TabContent tab ) {
		tabs.add(tab);
	}

	@Deprecated
	public static void removeTab(int index) {
		tabs.remove(index);
	}

	@Deprecated
	public static void removeTab(TabContent tab) {
		tabs.remove(tab);
	}

	@Deprecated
	public static TabContent getTab(int index) {
		if (index < 0) {
			return null;
		}
		return tabs.get(index);
	}

	@Deprecated
	public static TabContent getCurrentTab() {
		return getTab(appGui.getSelectedTabIndex());
	}

	/**
	 * @deprecated Use method getAnimator in GuiFrame
	 */
	@Deprecated
	public static Animator getAnimator() {
		if (getCurrentTab() == null) {
			return null;
		}
		return getCurrentTab().getAnimator();
	}
	
	//XXX Two Methodes to access same data (created after auto encapsulate)
	@Deprecated
	public static GuiFrame getApp() { // returns a reference to the application
		return getAppGui();
	}
	@Deprecated
	public static GuiFrame getAppGui() {
		return appGui;
	}
}
