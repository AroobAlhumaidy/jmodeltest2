/** 
 * FrameResults.java
 *
 * Title:			Modeltest
 * Description:		Select models of nucleotide substitition
 * @author			
 * @version			
 */

package es.uvigo.darwin.jmodeltest.gui;

import java.awt.Component;
import java.awt.Insets;
import java.awt.Point;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.TableColumn;

import es.uvigo.darwin.jmodeltest.XManager;
import es.uvigo.darwin.jmodeltest.utilities.MyTableCellRenderer;
import es.uvigo.darwin.jmodeltest.utilities.MyTableModel;
import es.uvigo.darwin.jmodeltest.utilities.TableSorter;

public class FrameResults extends JModelTestFrame {

	public static final int TAB_AIC = 1;
	public static final int TAB_AICc = 2;
	public static final int TAB_BIC = 3;
	public static final int TAB_DT = 4;
	
	private static final long serialVersionUID = 7368405541555631433L;

	private javax.swing.JPanel panelInfo = new javax.swing.JPanel();
	private javax.swing.JLabel labelInfo = new javax.swing.JLabel();
	private javax.swing.JLabel labelDate = new javax.swing.JLabel();
	private javax.swing.JTabbedPane tabbedPane = new javax.swing.JTabbedPane();
	private javax.swing.JPanel panelModels = new javax.swing.JPanel();
	private javax.swing.JScrollPane scrollPaneModels = new javax.swing.JScrollPane();
	private javax.swing.JTable tableModels = new javax.swing.JTable();
	private javax.swing.JPanel panelAIC = new javax.swing.JPanel();
	private javax.swing.JScrollPane scrollPaneAIC = new javax.swing.JScrollPane();
	private javax.swing.JTable tableAIC = new javax.swing.JTable();
	private javax.swing.JPanel panelAICc = new javax.swing.JPanel();
	private javax.swing.JScrollPane scrollPaneAICc = new javax.swing.JScrollPane();
	private javax.swing.JTable tableAICc = new javax.swing.JTable();
	private javax.swing.JPanel panelBIC = new javax.swing.JPanel();
	private javax.swing.JScrollPane scrollPaneBIC = new javax.swing.JScrollPane();
	private javax.swing.JTable tableBIC = new javax.swing.JTable();
	private javax.swing.JPanel panelDT = new javax.swing.JPanel();
	private javax.swing.JScrollPane scrollPaneDT = new javax.swing.JScrollPane();
	private  javax.swing.JTable tableDT = new javax.swing.JTable();

	private  MyTableModel modelModels = new MyTableModel("Model", options.numModels);
	TableSorter sorterModels = new TableSorter(modelModels);
	JTable tempTableModels = new JTable(sorterModels);

	private MyTableModel modelAIC = new MyTableModel("AIC", options.numModels);
	TableSorter sorterAIC = new TableSorter(modelAIC);
	JTable tempTableAIC = new JTable(sorterAIC);
	MyTableCellRenderer AICRenderer = new MyTableCellRenderer(tempTableAIC,"AIC"); 

	private MyTableModel modelAICc = new MyTableModel("AICc", options.numModels);
	TableSorter sorterAICc = new TableSorter(modelAICc);
	JTable tempTableAICc = new JTable(sorterAICc);
	MyTableCellRenderer AICcRenderer = new MyTableCellRenderer(tempTableAICc,"AICc"); 

	private MyTableModel modelBIC = new MyTableModel("BIC", options.numModels);
	TableSorter sorterBIC = new TableSorter(modelBIC);
	JTable tempTableBIC = new JTable(sorterBIC);
	MyTableCellRenderer BICRenderer = new MyTableCellRenderer(tempTableBIC,"BIC"); 
	
	private MyTableModel modelDT = new MyTableModel("DT", options.numModels);
	TableSorter sorterDT = new TableSorter(modelDT);
	JTable tempTableDT = new JTable(sorterDT);
	MyTableCellRenderer DTRenderer = new MyTableCellRenderer(tempTableDT,"DT"); 

	public void initComponents() throws Exception {

	   	tableModels = tempTableModels;
	    tableAIC = tempTableAIC;
	   	tableAICc = tempTableAICc;
		tableBIC = tempTableBIC;
		tableDT = tempTableDT;
				
		// set format for all columns
		for (int i = 0; i < 8; i++) 
			{ 
			TableColumn AICtableColumn = tableAIC.getColumnModel().getColumn(i); 
			AICtableColumn.setCellRenderer((javax.swing.table.TableCellRenderer) AICRenderer); 

			TableColumn AICctableColumn = tableAICc.getColumnModel().getColumn(i); 
			AICctableColumn.setCellRenderer((javax.swing.table.TableCellRenderer) AICcRenderer); 

			TableColumn BICtableColumn = tableBIC.getColumnModel().getColumn(i); 
			BICtableColumn.setCellRenderer((javax.swing.table.TableCellRenderer) BICRenderer); 

			TableColumn DTtableColumn = tableDT.getColumnModel().getColumn(i); 
			DTtableColumn.setCellRenderer((javax.swing.table.TableCellRenderer) DTRenderer); 
			}

 		panelInfo.setSize(new java.awt.Dimension(600, 30));
 		panelInfo.setLocation(new java.awt.Point(0, 400));
 		panelInfo.setVisible(true);
 		panelInfo.setLayout(null);
 		labelInfo.setSize(new java.awt.Dimension(580, 20));
 		labelInfo.setLocation(new java.awt.Point(40, 0));
 		labelInfo.setVisible(true);
 		labelInfo.setText("Decimal numbers are rounded. Click on column headers to sort data in ascending or descending order (+Shift)");
 		labelInfo.setForeground(java.awt.Color.gray);
 		labelInfo.setHorizontalTextPosition(javax.swing.JLabel.CENTER);
 		labelInfo.setFont(XManager.FONT_LABEL_BIG);
 		labelDate.setSize(new java.awt.Dimension(80, 20));
 		labelDate.setLocation(new java.awt.Point(40, 10));
 		labelDate.setVisible(true);
 		labelDate.setText("Date");
 		labelDate.setForeground(java.awt.Color.gray);
 		labelDate.setFont(XManager.FONT_LABEL_BIG);
 		tabbedPane.setSize(new java.awt.Dimension(600, 400));
 		tabbedPane.setLocation(new java.awt.Point(0, 0));
 		tabbedPane.setVisible(true);
 		tabbedPane.setAutoscrolls(true);
 		panelModels.setVisible(true);
 		panelModels.setLayout(null);
		panelModels.setFont(XManager.FONT_CONSOLE);

 		scrollPaneModels.setSize(new java.awt.Dimension(570, 320));
 		scrollPaneModels.setLocation(new java.awt.Point(12, 14));
 		scrollPaneModels.setVisible(true);
 		scrollPaneModels.setAutoscrolls(true);
 		scrollPaneModels.setForeground(java.awt.Color.blue);
 		scrollPaneModels.setBackground(null);
 		scrollPaneModels.setFont(XManager.FONT_TABULAR);
 		tableModels.setColumnSelectionAllowed(true);
 		tableModels.setToolTipText("Click and Shift+Click on headers to order up and down");
 		tableModels.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
 		tableModels.setCellSelectionEnabled(true);
 		tableModels.setVisible(true);
 		tableModels.setPreferredScrollableViewportSize(new java.awt.Dimension(500, 300));
 		tableModels.setGridColor(java.awt.Color.gray);
 		tableModels.setFont(XManager.FONT_TABULAR);
 		panelAIC.setVisible(true);
		panelAIC.setLayout(null);
 		scrollPaneAIC.setSize(new java.awt.Dimension(570, 320));
 		scrollPaneAIC.setLocation(new java.awt.Point(12, 14));
 		scrollPaneAIC.setVisible(true);
 		scrollPaneAIC.setAutoscrolls(true);
 		scrollPaneAIC.setForeground(java.awt.Color.blue);
 		scrollPaneAIC.setBackground(null);
 		scrollPaneAIC.setFont(XManager.FONT_TABULAR);
 		tableAIC.setColumnSelectionAllowed(true);
 		tableAIC.setToolTipText("Click and Shift+Click on headers to order up and down");
 		tableAIC.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
 		tableAIC.setCellSelectionEnabled(true);
 		tableAIC.setVisible(true);
 		tableAIC.setPreferredScrollableViewportSize(new java.awt.Dimension(575, 350));
 		tableAIC.setGridColor(java.awt.Color.gray);
 		tableAIC.setFont(XManager.FONT_TABULAR);
 		panelAICc.setVisible(true);
 		panelAICc.setLayout(null);
 		scrollPaneAICc.setSize(new java.awt.Dimension(570, 320));
 		scrollPaneAICc.setLocation(new java.awt.Point(12, 14));
 		scrollPaneAICc.setVisible(true);
 		scrollPaneAICc.setAutoscrolls(true);
 		scrollPaneAICc.setForeground(java.awt.Color.blue);
 		scrollPaneAICc.setBackground(null);
 		scrollPaneAICc.setFont(XManager.FONT_TABULAR);
 		tableAICc.setColumnSelectionAllowed(true);
 		tableAICc.setToolTipText("Click and Shift+Click on headers to order up and down");
 		tableAICc.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
 		tableAICc.setCellSelectionEnabled(true);
 		tableAICc.setVisible(true);
 		tableAICc.setPreferredScrollableViewportSize(new java.awt.Dimension(575, 350));
 		tableAICc.setGridColor(java.awt.Color.gray);
 		tableAICc.setFont(XManager.FONT_TABULAR);
 		panelBIC.setVisible(true);
 		panelBIC.setLayout(null);
 		scrollPaneBIC.setSize(new java.awt.Dimension(570, 320));
 		scrollPaneBIC.setLocation(new java.awt.Point(12, 14));
 		scrollPaneBIC.setVisible(true);
 		scrollPaneBIC.setAutoscrolls(true);
 		scrollPaneBIC.setForeground(java.awt.Color.blue);
 		scrollPaneBIC.setBackground(null);
 		scrollPaneBIC.setFont(XManager.FONT_TABULAR);
 		tableBIC.setColumnSelectionAllowed(true);
 		tableBIC.setToolTipText("Click and Shift+Click on headers to order up and down");
 		tableBIC.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
 		tableBIC.setCellSelectionEnabled(true);
 		tableBIC.setVisible(true);
 		tableBIC.setPreferredScrollableViewportSize(new java.awt.Dimension(575, 350));
 		tableBIC.setGridColor(java.awt.Color.gray);
 		tableBIC.setFont(XManager.FONT_TABULAR);
  		panelDT.setVisible(true);
 		panelDT.setLayout(null);
 		scrollPaneDT.setSize(new java.awt.Dimension(570, 320));
 		scrollPaneDT.setLocation(new java.awt.Point(12, 14));
 		scrollPaneDT.setVisible(true);
 		scrollPaneDT.setAutoscrolls(true);
 		scrollPaneDT.setForeground(java.awt.Color.blue);
 		scrollPaneDT.setBackground(null);
 		scrollPaneDT.setFont(XManager.FONT_TABULAR);
 		tableDT.setColumnSelectionAllowed(true);
 		tableDT.setToolTipText("Click and Shift+Click on headers to order up and down");
 		tableDT.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
 		tableDT.setCellSelectionEnabled(true);
 		tableDT.setVisible(true);
 		tableDT.setPreferredScrollableViewportSize(new java.awt.Dimension(575, 350));
 		tableDT.setGridColor(java.awt.Color.gray);
 		tableDT.setFont(XManager.FONT_TABULAR);
 		setLocation(new java.awt.Point(281, 80));
 		setResizable(true);
 		setFont(XManager.FONT_TABULAR);
 		setLayout(null);
 		setTitle("Results");
		setResizable(false);
		
 		panelInfo.add(labelInfo);
 		panelInfo.add(labelDate);
 		tabbedPane.add(panelModels);
 		tabbedPane.setTitleAt(tabbedPane.getTabCount() - 1, "Models");
 		tabbedPane.add(panelAIC);
		tabbedPane.setTitleAt(tabbedPane.getTabCount() - 1, "AIC");
		tabbedPane.setEnabledAt(tabbedPane.getTabCount() - 1, false);
		tabbedPane.add(panelAICc);
 		tabbedPane.setTitleAt(tabbedPane.getTabCount() - 1, "AICc");
 		tabbedPane.setEnabledAt(tabbedPane.getTabCount() - 1, false);
		tabbedPane.add(panelBIC);
 		tabbedPane.setTitleAt(tabbedPane.getTabCount() - 1, "BIC");
		tabbedPane.setEnabledAt(tabbedPane.getTabCount() - 1, false);
		tabbedPane.add(panelDT);
 		tabbedPane.setTitleAt(tabbedPane.getTabCount() - 1, "DT");
		tabbedPane.setEnabledAt(tabbedPane.getTabCount() - 1, false);

		panelModels.add(scrollPaneModels);
 		scrollPaneModels.getViewport().add(tableModels);
 		panelAIC.add(scrollPaneAIC);
 		scrollPaneAIC.getViewport().add(tableAIC);
 		panelAICc.add(scrollPaneAICc);
 		scrollPaneAICc.getViewport().add(tableAICc);
 		panelBIC.add(scrollPaneBIC);
 		scrollPaneBIC.getViewport().add(tableBIC);
 		panelDT.add(scrollPaneDT);
 		scrollPaneDT.getViewport().add(tableDT);

 		add(panelInfo);
 		add(tabbedPane);
 
 		tabbedPane.setSelectedIndex(0);
 		setSize(new java.awt.Dimension(600, 460));
 
 		// event handling
 		addWindowListener(new java.awt.event.WindowAdapter() {
 			public void windowClosing(java.awt.event.WindowEvent e) {
 				thisWindowClosing(e);
 			}
 		});
 
 		 sorterModels.addMouseListenerToHeaderInTable(tableModels);
 		 sorterAIC.addMouseListenerToHeaderInTable(tableAIC);
 		 sorterAICc.addMouseListenerToHeaderInTable(tableAICc);
 		 sorterBIC.addMouseListenerToHeaderInTable(tableBIC);			

		// set date
		Date today = new Date();
	    SimpleDateFormat formatter = new SimpleDateFormat("dd MMMMM yyyy");
	    String datenewformat = formatter.format(today);
	 	labelDate.setText(datenewformat);

	}
  
  	private boolean mShown = false;
  	
	public void addNotify() {
		super.addNotify();
		
		if (mShown)
			return;
			
		// move components to account for insets
		Insets insets = getInsets();
		Component[] components = getComponents();
		for (int i = 0; i < components.length; i++) {
			Point location = components[i].getLocation();
			location.move(location.x, location.y + insets.top);
			components[i].setLocation(location);
		}

		mShown = true;
	}

	// Close the window when the close box is clicked
	void thisWindowClosing(java.awt.event.WindowEvent e) {
		setVisible(false);
		dispose();
		//System.exit(0);
	}
	
	public void enablePane(int pane) {
		tabbedPane.setEnabledAt(pane, true);
	}
	
	public void disablePane(int pane) {
		tabbedPane.setEnabledAt(pane, false);
	}

	public void populate(int pane) {
		switch(pane) {
		case TAB_AIC:
			modelAIC.populate("AIC");
			break;
		case TAB_AICc:
			modelAICc.populate("AICc");
			break;
		case TAB_BIC:
			modelBIC.populate("BIC");
			break;
		case TAB_DT:
			modelDT.populate("DT");
			break;
		}
	}

}




