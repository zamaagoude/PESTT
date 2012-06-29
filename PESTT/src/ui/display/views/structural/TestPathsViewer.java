package ui.display.views.structural;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeSet;

import main.activator.Activator;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPartSite;

import ui.constants.Description;
import ui.constants.TableViewers;
import adt.graph.Graph;
import adt.graph.Path;
import domain.events.TestPathChangedEvent;

public class TestPathsViewer extends AbstractTableViewer implements Observer {

	private Composite parent;
	private TableViewer testPathhsViewer;
	private Control testPathsControl; // control of executedGraphViewer.
	private IWorkbenchPartSite site;
	private Listener listener;
	
	public TestPathsViewer(Composite parent, IWorkbenchPartSite site) {
		this.parent = parent;
		this.site = site;
		Activator.getDefault().getTestPathController().addObserverTestPath(this);
	}
	
	public TableViewer create() {
		testPathhsViewer = createViewTable(parent, site, TableViewers.TESTPATHSVIEWER);
		testPathsControl = testPathhsViewer.getControl();
		createColumnsToExecutedGraphViewer();
		setSelections(); // associate path to the ViewGraph elements.
		return testPathhsViewer;
	}

	@Override
	public void update(Observable obs, Object data) {
		if(data instanceof TestPathChangedEvent) {
			List<Object> testPaths = new ArrayList<Object>();
			Set<Path<Integer>> paths = getPathSet(((TestPathChangedEvent) data).testPathSet, ((TestPathChangedEvent) data).manuallyAdded);
			for(Path<Integer> path : paths)
				testPaths.add(path);
			if(testPaths.size() > 1)
				testPaths.add(Description.TOTAL);
			testPathhsViewer.setInput(testPaths);
			if(listener != null)
				testPathhsViewer.getTable().removeListener(SWT.MouseHover, listener);
			addTooltips();
		}
	}
	
	private Set<Path<Integer>> getPathSet(Iterable<Path<Integer>> automatic, Iterable<Path<Integer>> manually) {
		Set<Path<Integer>> set = new TreeSet<Path<Integer>>();
		for(Path<Integer> path : automatic)
			set.add(path);
		for(Path<Integer> path : manually)
			set.add(path);
		return set;
	}

	public void dispose() {
		testPathsControl.dispose();
	}
	
	private void createColumnsToExecutedGraphViewer() {
		String columnNames = TableViewers.TEST_PATTHS; // the names of column.
		int columnWidths = 500; // the width of column.
		TableViewerColumn col = createColumnsHeaders(testPathhsViewer, columnNames, columnWidths, 0);
		col.setLabelProvider(new StyledCellLabelProvider() {

			@Override
			public void update(ViewerCell cell) {
				if(cell.getElement() instanceof Graph<?>) {
					Graph<?> graph = (Graph<?>) cell.getElement();
					cell.setText(graph.toString());
				} else if(cell.getElement() instanceof Path<?>) {
					Path<?> path = (Path<?>) cell.getElement();
					cell.setText(path.toString());
				} else {
					String str = (String) cell.getElement();
					cell.setText(str);
				}
			}
		});
	}	
	
	private void setSelections() {
		testPathhsViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@SuppressWarnings("unchecked")
			public void selectionChanged(final SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection(); // get the selection.
				boolean hasTotal = false;
				Set<Path<Integer>> testPaths = new TreeSet<Path<Integer>>();
				for(Object obj : selection.toList())
					if(obj instanceof String) {
						hasTotal = true;
						break;
					}
					else
						testPaths.add((Path<Integer>) obj);
				if(hasTotal) {
					testPaths.clear();
					for(Path<Integer> path : Activator.getDefault().getTestPathController().getTestPathsManuallyAdded())
						testPaths.add(path);
					for(Path<Integer> path : Activator.getDefault().getTestPathController().getTestPaths())
						testPaths.add(path);
				}	
				Activator.getDefault().getTestPathController().selectTestPath(testPaths);
		    }
		});
	}
	
	private void addTooltips() {
		listener =  new Listener() {
			
			public void handleEvent(Event event) {
				Point coords = new Point(event.x, event.y);
				TableItem item = testPathhsViewer.getTable().getItem(coords);
				Set<Path<Integer>> set = getPathSet(Activator.getDefault().getTestPathController().getTestPaths(), Activator.getDefault().getTestPathController().getTestPathsManuallyAdded());
				Iterator<Path<Integer>> iterator = set.iterator();
				Path<Integer> path = null;
				for(TableItem i : testPathhsViewer.getTable().getItems()) {  
					path = null;
					if(iterator.hasNext()) {
						path = iterator.next();
						if(i == item)
							break;
					}
				}
				if(path != null)
					testPathhsViewer.getTable().setToolTipText(Activator.getDefault().getTestPathController().getTooltip(path));
				else
					testPathhsViewer.getTable().setToolTipText("");	
			}
		};
		testPathhsViewer.getTable().addListener(SWT.MouseHover, listener);
	}
}