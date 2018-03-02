/*******************************************************************************
 * Copyright (c) 2009-2018 Weasis Team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     Nicolas Roduit - initial API and implementation
 *******************************************************************************/
package org.weasis.dicom.explorer;

import java.util.ArrayList;
import java.util.List;

<<<<<<< Upstream, based on origin/master
import javax.swing.SwingWorker;

import org.weasis.core.api.gui.task.CircularProgressBar;
import org.weasis.core.api.gui.util.GuiExecutor;
=======
>>>>>>> 9d81b79 Adapt the project for FX transition
import org.weasis.dicom.param.CancelListener;

import javafx.concurrent.Task;

public abstract class ExplorerTask<T> extends Task<T> {
    private final boolean globalLoadingManager;
    private final boolean subTask;
    private final List<CancelListener> cancelListeners;

    public ExplorerTask(String message, boolean interruptible) {
        this(message, interruptible, false);
    }

    public ExplorerTask(String message, boolean globalLoadingManager, boolean subTask) {
        this.updateMessage(message);
        this.globalLoadingManager = globalLoadingManager;
        this.subTask = subTask;
        this.cancelListeners = new ArrayList<>();

        this.setOnCancelled(event -> {
            if (isCancelled()) {
                fireProgress();
            }
        });
    }

    public boolean isGlobalLoadingManager() {
        return globalLoadingManager;
    }

    public boolean isSubTask() {
        return subTask;
    }

    public void addCancelListener(CancelListener listener) {
        if (listener != null && !cancelListeners.contains(listener)) {
            cancelListeners.add(listener);
        }
    }

    public void removeCancelListener(CancelListener listener) {
        if (listener != null) {
            cancelListeners.remove(listener);
        }
    }

    public void removeAllCancelListeners() {
        cancelListeners.clear();
    }

    private void fireProgress() {
        for (int i = 0; i < cancelListeners.size(); i++) {
            cancelListeners.get(i).cancel();
        }
    }
}
