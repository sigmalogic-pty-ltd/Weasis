package org.weasis.base.explorer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;

import org.weasis.core.api.media.data.Codec;
import org.weasis.core.api.media.data.MediaElement;
import org.weasis.core.api.media.data.MediaReader;
import org.weasis.core.api.media.data.MediaSeries;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.editor.SeriesViewerFactory;
import org.weasis.core.ui.editor.ViewerPluginBuilder;
import org.weasis.core.ui.util.TitleMenuItem;

public final class JIThumbnailList extends JList implements JIObservable {

    public static final Dimension ICON_DIM = new Dimension(150, 150);
    private static final NumberFormat intGroupFormat = NumberFormat.getIntegerInstance();
    static {
        intGroupFormat.setGroupingUsed(true);
    }
    private final int editingIndex = -1;

    private final FileTreeModel model;

    private final ToggleSelectionModel selectionModel;

    private boolean changed;

    private MediaElement lastSelectedDiskObject = null;

    public JIThumbnailList(final FileTreeModel model) {
        this(model, VERTICAL_WRAP, null);
    }

    public JIThumbnailList(final FileTreeModel model, final OrderedFileList dObjList) {
        this(model, VERTICAL_WRAP, dObjList);
    }

    public JIThumbnailList(final FileTreeModel model, final int scrollMode, final OrderedFileList dObjList) {
        super();

        boolean useSelection = false;
        if (dObjList != null) {
            this.setModel(new JIListModel(this, dObjList));
            useSelection = true;
        } else {
            this.setModel(new JIListModel(this));
        }

        this.model = model;
        this.changed = false;

        this.selectionModel = new ToggleSelectionModel();
        this.setBackground(new Color(242, 242, 242));

        setSelectionModel(this.selectionModel);
        setDragEnabled(true);
        // setTransferHandler(new ListTransferHandler());
        ThumbnailRenderer panel = new ThumbnailRenderer();
        Dimension dim = panel.getPreferredSize();
        setCellRenderer(panel);
        setFixedCellHeight(dim.height);
        setFixedCellWidth(dim.width);
        setVisibleRowCount(-1);

        setLayoutOrientation(HORIZONTAL_WRAP);

        addMouseListener(new PopupTrigger());
        addKeyListener(new JIThumbnailKeyAdapter());

        if (useSelection) {
            // JIExplorer.instance().getContext();
        }

        setVerifyInputWhenFocusTarget(false);
        JIThumbnailCache.getInstance().invalidate();
    }

    public JIListModel getThumbnailListModel() {
        return (JIListModel) getModel();
    }

    public Frame getFrame() {
        return null;
    }

    public boolean isEditing() {
        if (this.editingIndex > -1) {
            return true;
        }
        return false;
    }

    // Subclass JList to workaround bug 4832765, which can cause the
    // scroll pane to not let the user easily scroll up to the beginning
    // of the list. An alternative would be to set the unitIncrement
    // of the JScrollBar to a fixed value. You wouldn't get the nice
    // aligned scrolling, but it should work.
    @Override
    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        int row;
        if ((orientation == SwingConstants.VERTICAL) && (direction < 0) && ((row = getFirstVisibleIndex()) != -1)) {
            final Rectangle r = getCellBounds(row, row);
            if ((r.y == visibleRect.y) && (row != 0)) {
                final Point loc = r.getLocation();
                loc.y--;
                final int prevIndex = locationToIndex(loc);
                final Rectangle prevR = getCellBounds(prevIndex, prevIndex);

                if ((prevR == null) || (prevR.y >= r.y)) {
                    return 0;
                }
                return prevR.height;
            }
        }
        return super.getScrollableUnitIncrement(visibleRect, orientation, direction);
    }

    @Override
    public String getToolTipText(final MouseEvent evt) {
        // if (!JIPreferences.getInstance().isThumbnailToolTips()) {
        // return null;
        // }
        // Get item index
        final int index = locationToIndex(evt.getPoint());
        if (index < 0) {
            return "";
        }

        // Get item
        final Object item = getModel().getElementAt(index);

        if (((MediaElement) item).getName() == null) {
            return null;
        }

        return "<html>" + ((MediaElement) item).getName() + "<br> Size: "
            + intGroupFormat.format(((MediaElement) item).getLength() / 1024L) + " KB<br>" + "Date: "
            + new Date(((MediaElement) item).getLastModified()).toString() + "</html>";
    }

    public void reset() {
        setFixedCellHeight(ICON_DIM.height);
        setFixedCellWidth(ICON_DIM.width);
        setLayoutOrientation(HORIZONTAL_WRAP);

        ((JIListModel) getModel()).reload();
        setVisibleRowCount(-1);
        clearSelection();
        ensureIndexIsVisible(0);
    }

    public void openSelection() {
        Object object = getSelectedValue();
        if (object instanceof MediaElement) {
            MediaElement mediaElement = (MediaElement) object;
            openSelection(new MediaElement[] { mediaElement }, true, true);
        }
    }

    public void openSelection(MediaElement[] medias, boolean compareEntryToBuildNewViewer, boolean bestDefaultLayout) {
        if (medias != null) {
            ArrayList<MediaSeries> list = new ArrayList<MediaSeries>();
            for (MediaElement mediaElement : medias) {
                String cfile = getThumbnailListModel().getFileInCache(mediaElement.getFile().getAbsolutePath());
                File file = cfile == null ? mediaElement.getFile() : new File(JIListModel.EXPLORER_CACHE_DIR, cfile);
                MediaReader reader = ViewerPluginBuilder.getMedia(file);
                if (reader != null && file != null) {
                    String sUID;
                    String gUID;
                    TagW tname;
                    String tvalue;

                    Codec codec = reader.getCodec();
                    if (codec != null && codec.isMimeTypeSupported("application/dicom")) {
                        if (reader.getMediaElement() == null) {
                            // DICOM is not readable
                            return;
                        }
                        sUID = (String) reader.getTagValue(TagW.SeriesInstanceUID);
                        gUID = (String) reader.getTagValue(TagW.PatientID);
                        tname = TagW.PatientName;
                        tvalue = (String) reader.getTagValue(TagW.PatientName);
                    } else {
                        sUID = mediaElement.getFile().getAbsolutePath();
                        gUID = sUID;
                        tname = TagW.FileName;
                        tvalue = mediaElement.getFile().getName();
                    }

                    MediaSeries s =
                        ViewerPluginBuilder.buildMediaSeriesWithDefaultModel(reader, gUID, tname, tvalue, sUID);
                    if (s != null && !list.contains(s)) {
                        list.add(s);
                    }
                }
            }
            if (list.size() > 0) {
                ViewerPluginBuilder.openSequenceInDefaultPlugin(list, ViewerPluginBuilder.DefaultDataModel,
                    compareEntryToBuildNewViewer, bestDefaultLayout);
            }
        }
    }

    public void openGroup(MediaElement[] medias, boolean compareEntryToBuildNewViewer, boolean bestDefaultLayout,
        boolean modeLayout) {
        if (medias != null) {
            String groupUID = null;

            if (modeLayout) {
                groupUID = UUID.randomUUID().toString();
            }
            Map<SeriesViewerFactory, List<MediaSeries>> plugins = new HashMap<SeriesViewerFactory, List<MediaSeries>>();
            for (MediaElement m : medias) {
                String mime = m.getMimeType();
                if (mime != null) {
                    SeriesViewerFactory plugin = UIManager.getViewerFactory(mime);
                    if (plugin != null) {
                        List<MediaSeries> list = plugins.get(plugin);
                        if (list == null) {
                            list = new ArrayList<MediaSeries>(modeLayout ? 1 : 10);
                            plugins.put(plugin, list);
                        }

                        // Get only application readers from files
                        String cfile = getThumbnailListModel().getFileInCache(m.getFile().getAbsolutePath());
                        File file = cfile == null ? m.getFile() : new File(JIListModel.EXPLORER_CACHE_DIR, cfile);
                        MediaReader mreader = ViewerPluginBuilder.getMedia(file, false);
                        if (mreader != null) {
                            if (modeLayout) {
                                MediaSeries series =
                                    ViewerPluginBuilder.buildMediaSeriesWithDefaultModel(mreader, groupUID, null, null,
                                        null);
                                if (series != null) {
                                    list.add(series);
                                }
                            } else {
                                MediaSeries series = null;
                                if (list.size() == 0) {
                                    series = ViewerPluginBuilder.buildMediaSeriesWithDefaultModel(mreader);
                                    if (series != null) {
                                        list.add(series);
                                    }
                                } else {
                                    series = list.get(0);
                                    if (series != null) {
                                        MediaElement[] ms = mreader.getMediaElement();
                                        if (ms != null) {
                                            for (MediaElement media : ms) {
                                                media.setTag(TagW.SeriesInstanceUID,
                                                    series.getTagValue(series.getTagID()));
                                                URI uri = media.getMediaURI();
                                                media.setTag(TagW.SOPInstanceUID, uri == null ? UUID.randomUUID()
                                                    .toString() : uri.toString());
                                                series.addMedia(media);
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }

            for (Iterator<Entry<SeriesViewerFactory, List<MediaSeries>>> iterator = plugins.entrySet().iterator(); iterator
                .hasNext();) {
                Entry<SeriesViewerFactory, List<MediaSeries>> item = iterator.next();
                ViewerPluginBuilder.openSequenceInPlugin(item.getKey(), item.getValue(),
                    ViewerPluginBuilder.DefaultDataModel, compareEntryToBuildNewViewer, bestDefaultLayout);
            }
        }
    }

    public void nextPage(final KeyEvent e) {
        final int lastIndex = getLastVisibleIndex();

        if (getLayoutOrientation() != JList.HORIZONTAL_WRAP) {
            e.consume();
            final int firstIndex = getFirstVisibleIndex();
            final int visibleRows = getVisibleRowCount();
            final int visibleColums = (int) (((float) (lastIndex - firstIndex) / (float) visibleRows) + .5);
            final int visibleItems = visibleRows * visibleColums;

            final int val =
                (lastIndex + visibleItems >= getModel().getSize()) ? getModel().getSize() - 1 : lastIndex
                    + visibleItems;
            // log.debug("Next index is " + val + " " + lastIndex + " " + visibleItems + " " + visibleRows);
            clearSelection();
            setSelectedIndex(val);
            fireSelectionValueChanged(val, val, false);
        } else {
            clearSelection();
            setSelectedIndex(lastIndex);
            fireSelectionValueChanged(lastIndex, lastIndex, false);
        }
    }

    public void lastPage(final KeyEvent e) {
        final int lastIndex = getLastVisibleIndex();

        if (getLayoutOrientation() != JList.HORIZONTAL_WRAP) {
            e.consume();
            final int firstIndex = getFirstVisibleIndex();
            final int visibleRows = getVisibleRowCount();
            final int visibleColums = (int) (((float) (lastIndex - firstIndex) / (float) visibleRows) + .5);
            final int visibleItems = visibleRows * visibleColums;

            final int val = ((firstIndex - 1) - visibleItems < 0) ? 0 : (firstIndex - 1) - visibleItems;
            // log.debug("Next index is " + val + " " + lastIndex + " " + visibleItems + " " + visibleRows);
            clearSelection();
            setSelectedIndex(val);
            fireSelectionValueChanged(val, val, false);
        } else {
            clearSelection();
            setSelectedIndex(lastIndex);
            fireSelectionValueChanged(lastIndex, lastIndex, false);
        }
    }

    public void jiThumbnail_keyPressed(final KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_PAGE_DOWN:
                nextPage(e);
                break;
            case KeyEvent.VK_PAGE_UP:
                lastPage(e);
                break;
            case KeyEvent.VK_ENTER:
                openSelection();
                e.consume();
                break;
        }
    }

    private Action refreshAction() {
        // TODO set this action in toolbar
        return new AbstractAction("Refresh List") {

            @Override
            public void actionPerformed(final ActionEvent e) {
                final Thread runner = new Thread() {

                    @Override
                    public void run() {
                        Runnable runnable = new Runnable() {

                            @Override
                            public void run() {
                                JIThumbnailList.this.getThumbnailListModel().reload();
                            }
                        };
                        SwingUtilities.invokeLater(runnable);
                    }
                };
                runner.start();
            }
        };
    }

    @Override
    public MediaElement[] getSelectedValues() {
        final Object[] objs = super.getSelectedValues();
        final MediaElement[] dObjs = new MediaElement[objs.length];
        int cnt = 0;
        for (final Object obj : objs) {
            dObjs[cnt++] = (MediaElement) obj;
        }
        return dObjs;
    }

    public int getLastSelectedIndex() {
        final Object[] objs = super.getSelectedValues();
        final Object obj = super.getSelectedValue();
        int cnt = 0;
        for (final Object o : objs) {
            if (o.equals(obj)) {
                return cnt;
            }
            cnt++;
        }
        return cnt - 1;
    }

    protected void listValueChanged(final ListSelectionEvent e) {
        if (this.lastSelectedDiskObject == null) {
            this.lastSelectedDiskObject = (MediaElement) getModel().getElementAt(e.getLastIndex());
        }
        DefaultExplorer.getTreeContext().setSelectedDiskObjects(this.getSelectedValues(), this.lastSelectedDiskObject);

        this.lastSelectedDiskObject = null;

        setChanged();
        notifyObservers(JIObservable.SECTION_CHANGED);
        clearChanged();
    }

    public JPopupMenu buidContexMenu(final MouseEvent e) {

        try {
            final MediaElement[] medias = getSelectedValues();

            if (medias == null || medias.length == 0) {
                return null;
            } else {
                JPopupMenu popupMenu = new JPopupMenu();
                TitleMenuItem itemTitle = new TitleMenuItem("Selection Menu", popupMenu.getInsets());
                popupMenu.add(itemTitle);
                popupMenu.addSeparator();
                JMenuItem menuItem = new JMenuItem(new AbstractAction("Open") {

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        openSelection(medias, true, true);
                    }
                });

                popupMenu.add(menuItem);

                menuItem = new JMenuItem(new AbstractAction("Open in a new viewer") {

                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        openSelection(medias, false, true);
                    }
                });

                popupMenu.add(menuItem);

                if (medias.length > 1) {
                    popupMenu.addSeparator();
                    menuItem = new JMenuItem(new AbstractAction("Open in Series") {

                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            openGroup(medias, true, true, false);
                        }

                    });
                    popupMenu.add(menuItem);

                    menuItem = new JMenuItem(new AbstractAction("Open in Layout") {

                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            openGroup(medias, true, false, true);
                        }

                    });
                    popupMenu.add(menuItem);
                }
                return popupMenu;

            }
        } catch (final Exception exp) {
        } finally {
            e.consume();
        }
        return null;

    }

    final class PopupTrigger extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                openSelection();
            }
        }

        @Override
        public void mousePressed(final MouseEvent evt) {
            showPopup(evt);
        }

        @Override
        public void mouseReleased(final MouseEvent evt) {
            showPopup(evt);
        }

        private void showPopup(final MouseEvent evt) {
            // Context menu
            if (SwingUtilities.isRightMouseButton(evt)) {
                JPopupMenu popupMenu = JIThumbnailList.this.buidContexMenu(evt);
                if (popupMenu != null) {
                    popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            }
        }
    }

    /**
     * If this object has changed, as indicated by the <code>hasChanged</code> method, then notify all of its observers
     * and then call the <code>clearChanged</code> method to indicate that this object has no longer changed.
     * <p>
     * Each observer has its <code>update</code> method called with two arguments: this observable object and
     * <code>null</code>. In other words, this method is equivalent to: <blockquote><tt>
     * notifyObservers(null)</tt> </blockquote>
     * 
     * @see java.util.Observable#clearChanged()
     * @see java.util.Observable#hasChanged()
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    public void notifyObservers() {
        notifyObservers(null);
    }

    /**
     * If this object has changed, as indicated by the <code>hasChanged</code> method, then notify all of its observers
     * and then call the <code>clearChanged</code> method to indicate that this object has no longer changed.
     * <p>
     * Each observer has its <code>update</code> method called with two arguments: this observable object and the
     * <code>arg</code> argument.
     * 
     * @param arg
     *            any object.
     * @see java.util.Observable#clearChanged()
     * @see java.util.Observable#hasChanged()
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void notifyObservers(final Object arg) {

        synchronized (this) {
            if (!this.changed) {
                return;
            }
            clearChanged();
        }

    }

    public void notifyStatusBar(final Object arg) {
        synchronized (this) {
            clearChanged();
        }

    }

    /**
     * Marks this <tt>Observable</tt> object as having been changed; the <tt>hasChanged</tt> method will now return
     * <tt>true</tt>.
     */
    public synchronized void setChanged() {
        this.changed = true;
    }

    /**
     * Indicates that this object has no longer changed, or that it has already notified all of its observers of its
     * most recent change, so that the <tt>hasChanged</tt> method will now return <tt>false</tt>. This method is called
     * automatically by the <code>notifyObservers</code> methods.
     * 
     * @see java.util.Observable#notifyObservers()
     * @see java.util.Observable#notifyObservers(java.lang.Object)
     */
    public synchronized void clearChanged() {
        this.changed = false;
    }

    /**
     * Tests if this object has changed.
     * 
     * @return <code>true</code> if and only if the <code>setChanged</code> method has been called more recently than
     *         the <code>clearChanged</code> method on this object; <code>false</code> otherwise.
     * @see java.util.Observable#clearChanged()
     * @see java.util.Observable#setChanged()
     */
    @Override
    public synchronized boolean hasChanged() {
        return this.changed;
    }

    final class JIThumbnailKeyAdapter extends java.awt.event.KeyAdapter {

        /** Creates a new instance of JIThumbnailKeyListener */
        public JIThumbnailKeyAdapter() {
        }

        /** key event handlers */
        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
                JIThumbnailList.this.selectionModel.setShiftKey(true);
            }
            if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                JIThumbnailList.this.selectionModel.setCntrlKey(true);
            }
            final Thread runner = new Thread() {

                @Override
                public void run() {
                    Runnable runnable = new Runnable() {

                        @Override
                        public void run() {
                            JIThumbnailList.this.jiThumbnail_keyPressed(e);
                        }
                    };
                    SwingUtilities.invokeLater(runnable);
                }
            };
            runner.start();
        }

        @Override
        public void keyReleased(final KeyEvent e) {
            JIThumbnailList.this.selectionModel.setShiftKey(false);
            JIThumbnailList.this.selectionModel.setCntrlKey(false);
        }
    }
}
