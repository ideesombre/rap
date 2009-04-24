/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.internal.cheatsheets.composite.explorer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;

public class SkipAction extends Action {

	private static final String SKIP_CCS_TASK_GIF = "skip_ccs_task.gif"; //$NON-NLS-1$
	private AbstractTask task;

	public SkipAction(ICompositeCheatSheetTask task) {
		this.task = (AbstractTask) task;
		this.setText(Messages.get().COMPOSITE_MENU_SKIP);
		IPath ePath = CheatSheetPlugin.ICONS_PATH.append(CheatSheetPlugin.T_ELCL).append(SKIP_CCS_TASK_GIF);
		ImageDescriptor skipImage = CheatSheetPlugin.createImageDescriptor(CheatSheetPlugin.getPlugin().getBundle(), ePath);	
		this.setImageDescriptor(skipImage);
	}
	
	public void run() {
	    task.setState(ICompositeCheatSheetTask.SKIPPED);
	}

}
