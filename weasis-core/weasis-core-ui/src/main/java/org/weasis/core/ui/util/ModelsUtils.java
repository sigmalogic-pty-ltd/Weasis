package org.weasis.core.ui.util;

import org.weasis.core.api.image.GridBagLayoutModel;
import org.weasis.core.ui.editor.image.ImageViewerPlugin;
import org.weasis.core.ui.editor.image.SynchView;

import java.util.*;

public final class ModelsUtils {
    private ModelsUtils() {

    }

    public static Map<String, GridBagLayoutModel> createDefaultLayoutModels() {

        Map<String, GridBagLayoutModel> layoutModels = Collections.synchronizedMap(new LinkedHashMap<>());

        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.VIEWS_1x1);
        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.VIEWS_1x2);
        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.VIEWS_2x1);
        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.VIEWS_2x2);
        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.VIEWS_2x1_r1xc2_dump);
        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.VIEWS_2x2_f2);
        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.VIEWS_2_f1x2);
        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.buildGridBagLayoutModel(1, 3, ImageViewerPlugin.view2dClass.getName()));
        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.buildGridBagLayoutModel(1, 4, ImageViewerPlugin.view2dClass.getName()));
        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.buildGridBagLayoutModel(2, 4, ImageViewerPlugin.view2dClass.getName()));
        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.buildGridBagLayoutModel(2, 6, ImageViewerPlugin.view2dClass.getName()));
        layoutModels = putModelIntoTable(layoutModels, ImageViewerPlugin.buildGridBagLayoutModel(2, 8, ImageViewerPlugin.view2dClass.getName()));

        return layoutModels;
    }

    public static Map<String, SynchView> createDefaultSynchViews() {
        Map<String, SynchView> synchViews = Collections.synchronizedMap(new LinkedHashMap<>());

        synchViews = putModelIntoTable(synchViews, SynchView.NONE);
        synchViews = putModelIntoTable(synchViews, SynchView.DEFAULT_STACK);
        synchViews = putModelIntoTable(synchViews, SynchView.DEFAULT_TILE);
        synchViews = putModelIntoTable(synchViews, SynchView.DEFAULT_TILE_MULTIPLE);
        synchViews = putModelIntoTable(synchViews, SynchView.DEFAULT_SERIES_TILE);

        return synchViews;
    }

    private static Map<String, GridBagLayoutModel> putModelIntoTable(Map<String, GridBagLayoutModel> table, GridBagLayoutModel model) {
        table.put(model.getUIName(), model);
        return table;
    }

    private static Map<String, SynchView> putModelIntoTable(Map<String, SynchView> table, SynchView model) {
        table.put(model.getName(), model);
        return table;
    }
}
